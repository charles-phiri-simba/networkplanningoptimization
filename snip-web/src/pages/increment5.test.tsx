import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { AppRoutes } from '../routes/AppRoutes'
import {
  assessmentFixture,
  caseFixture,
  cellFixture,
  contextFixture,
  gnbFixture,
  siteFixture,
} from '../test/fixtures'

vi.mock('../features/map/SiteMap', () => ({
  SiteMap: ({ sites }: { sites: Array<{ siteId: string; name: string }> }) => (
    <div data-testid="site-map">
      {sites.map((site) => (
        <div key={site.siteId}>{site.name}</div>
      ))}
    </div>
  ),
}))

function signedIn(path: string) {
  storeDemoIdentity(DEMO_PERSONAS[0])
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

function mockJson(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json', 'X-Correlation-Id': 'test-corr' },
  })
}

const emptyGuidance = {
  ...assessmentFixture,
  likelyContributors: [],
  recommendedChecks: [],
  missingEvidence: [],
  citations: [],
}

const emptyRetrieval = {
  ...assessmentFixture,
  retrievalEmpty: true,
  citations: [],
  summary: 'No grounded engineering note was retrieved for this assessment.',
}

describe('Increment 5 investigation workspace', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('renders finding, observed evidence, and analysis in evidence-first order', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByRole('heading', { name: 'Degrading radio quality' })).toBeInTheDocument()
    expect(screen.getByText('Finding')).toBeInTheDocument()
    expect(await screen.findByText(/Downlink BLER is above the critical threshold/)).toBeInTheDocument()
    const evidence = screen.getByRole('heading', { name: 'Observed evidence' })
    const analysis = screen.getByRole('heading', { name: 'SNIP analysis' })
    expect(evidence.compareDocumentPosition(analysis) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByRole('heading', { name: 'Likely contributors to investigate' })).toBeInTheDocument()
    expect(screen.getByText('High txPower')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Recommended engineering checks' })).toBeInTheDocument()
    expect(screen.getByText('Review neighbours')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Missing evidence' })).toBeInTheDocument()
    expect(screen.getByText('PRB utilisation time series is incomplete')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Knowledge sources' })).toBeInTheDocument()
    expect(screen.getByText('doc-1')).toBeInTheDocument()
    expect(screen.getByText('BLER guidance')).toBeInTheDocument()
    expect(screen.getByText('Human engineering review required.')).toBeInTheDocument()
    expect(screen.getByText('Retrieved knowledge and investigation guidance')).toBeInTheDocument()
    expect(screen.getByText(/investigation hypotheses for human engineering review/i)).toBeInTheDocument()
    expect(screen.getByText(/Rule-based investigation guidance/)).toBeInTheDocument()
    expect(screen.getAllByText('Synthetic demo observation — not live network data.').length).toBeGreaterThan(0)
    expect(screen.queryByText(/LLM-generated/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/AI-generated/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/confirmed root cause/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: /root cause/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /apply|execute|fix|remediate/i })).not.toBeInTheDocument()
    expect(screen.queryByText(/network health/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/sourceHealth/i)).not.toBeInTheDocument()
    const urls = [...new Set(fetchMock.mock.calls.map((call) => String(call[0])))]
    expect(urls.some((url) => url.includes('/agent-runs'))).toBe(false)
    expect(urls.some((url) => url.includes('/mcp'))).toBe(false)
    expect(urls.some((url) => url.includes('production-change'))).toBe(false)
    expect(urls.some((url) => url.includes('/campaign'))).toBe(false)
    expect(urls.some((url) => url.includes('/sandbox/executions'))).toBe(false)
    expect(urls.filter((url) => url.startsWith('/api/v1/')).sort()).toEqual(
      [
        `/api/v1/assurance/cases/${caseFixture.id}`,
        `/api/v1/assurance/cases/${caseFixture.id}/assessment`,
      ].sort(),
    )
  })

  it('keeps observed evidence visible while analysis is loading', async () => {
    let resolveAssessment: ((value: Response) => void) | undefined
    const assessmentGate = new Promise<Response>((resolve) => {
      resolveAssessment = resolve
    })
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return assessmentGate
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByRole('heading', { name: 'Observed evidence' })).toBeInTheDocument()
    expect(screen.getByText('High BLER')).toBeInTheDocument()
    expect(screen.getByText('Loading SNIP analysis…')).toBeInTheDocument()
    resolveAssessment?.(
      new Response(JSON.stringify(assessmentFixture), {
        status: 200,
        headers: { 'Content-Type': 'application/json', 'X-Correlation-Id': 'test-corr' },
      }),
    )
    expect(await screen.findByText(/Downlink BLER is above the critical threshold/)).toBeInTheDocument()
  })

  it('keeps case, evidence, and Open cell when assessment fails', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) {
        return mockJson({ error: 'assessment down' }, 500)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByRole('heading', { name: 'Degrading radio quality' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Observed evidence' })).toBeInTheDocument()
    expect(screen.getByText('High BLER')).toBeInTheDocument()
    expect(screen.getByText('Analysis unavailable')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Open cell' })[0]).toHaveAttribute(
      'href',
      '/network/cells/CELL-001',
    )
  })

  it('shows a bounded error when the case request fails', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson({ error: 'missing case' }, 404)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByText('Not found')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Observed evidence' })).not.toBeInTheDocument()
    expect(screen.queryByText(/Downlink BLER is above the critical threshold/)).not.toBeInTheDocument()
  })

  it('shows retrieval-empty and empty-source states without fabricating citations', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(emptyRetrieval)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(
      await screen.findByText(/No supporting knowledge source was retrieved for this assessment/),
    ).toBeInTheDocument()
    expect(screen.getByText('No retrieved sources.')).toBeInTheDocument()
    expect(screen.queryByText('doc-1')).not.toBeInTheDocument()
    expect(screen.getByText('High txPower')).toBeInTheDocument()
    expect(screen.getByText('Review neighbours')).toBeInTheDocument()
  })

  it('shows bounded empty states for empty guidance lists', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(emptyGuidance)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByText('No likely contributors were identified.')).toBeInTheDocument()
    expect(screen.getByText('No recommended engineering checks were identified.')).toBeInTheDocument()
    expect(screen.getByText('No additional missing evidence was identified.')).toBeInTheDocument()
    expect(screen.getByText('No retrieved sources.')).toBeInTheDocument()
  })

  it('preserves PI4 Open case and PI3 Optimize handoff without queue assessment calls', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([caseFixture])
      if (url === '/api/v1/integration/sync/sources') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Network operations' })).toBeInTheDocument()
    const overviewUrls = fetchMock.mock.calls.map((call) => String(call[0]))
    expect(overviewUrls.some((url) => url.includes('/assessment'))).toBe(false)
    await user.click(await screen.findByRole('link', { name: 'Open case' }))
    expect(await screen.findByRole('heading', { name: 'Degrading radio quality' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: 'Open cell' })[0])
    const optimize = await screen.findByRole('link', { name: /Propose txPower optimization/i })
    expect(optimize).toHaveAttribute('href', '/network/cells/CELL-001/optimize')
  })
})
