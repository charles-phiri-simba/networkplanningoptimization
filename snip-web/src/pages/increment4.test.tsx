import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { AppRoutes } from '../routes/AppRoutes'
import {
  acknowledgedCaseFixture,
  assessmentFixture,
  caseFixture,
  cellFixture,
  cellTwoFixture,
  contextFixture,
  gnbFixture,
  resolvedCaseFixture,
  siteFixture,
  unlocatedSiteFixture,
  warningCaseFixture,
} from '../test/fixtures'

vi.mock('../features/map/SiteMap', () => ({
  SiteMap: ({ sites }: { sites: Array<{ siteId: string; name: string; latitude: number | null }> }) => (
    <div data-testid="site-map">
      {sites.map((site) => (
        <div key={site.siteId}>
          {site.name}
          {site.latitude === null ? ' (no coordinates)' : ''}
        </div>
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

function summaryValue(label: string) {
  const term = screen.getByText(label)
  return term.parentElement?.querySelector('dd')?.textContent ?? ''
}

describe('Increment 4 network operations workspace', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('renders the workspace when a site has no coordinates', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture, unlocatedSiteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByRole('link', { name: /Unlocated Demo Site/i })).toBeInTheDocument()
    expect(screen.getByTestId('site-map')).toHaveTextContent('Unlocated Demo Site (no coordinates)')
  })

  it('navigates from an issue to the Assurance case', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network')
    await user.click(await screen.findByRole('link', { name: 'Open case' }))
    expect(await screen.findByRole('heading', { name: 'DEGRADING_RADIO_QUALITY' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Open cell' })).toHaveAttribute('href', '/network/cells/CELL-001')
  })

  it('keeps case to cell navigation valid', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([caseFixture])
      if (url === '/api/v1/integration/sync/sources') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/assurance/${caseFixture.id}`)
    await user.click(await screen.findByRole('link', { name: 'Open cell' }))
    expect(await screen.findByRole('heading', { name: 'n78-1 high-BLER demo' })).toBeInTheDocument()
  })

  it('keeps cell to Optimize handoff valid', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([caseFixture])
      if (url === '/api/v1/integration/sync/sources') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network/cells/CELL-001')
    const link = await screen.findByRole('link', { name: /Propose txPower optimization/i })
    expect(link).toHaveAttribute('href', '/network/cells/CELL-001/optimize')
  })

  it('does not present a generic network-health score or state', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText(/Critical Assurance finding/)).toBeInTheDocument()
    expect(screen.queryByText(/network health/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/\bHealthy\b/)).not.toBeInTheDocument()
    expect(screen.queryByText(/\bDegraded\b/)).not.toBeInTheDocument()
    expect(screen.queryByText(/\bUnhealthy\b/)).not.toBeInTheDocument()
    expect(screen.queryByText(/health score/i)).not.toBeInTheDocument()
  })

  it('does not use sourceHealth or Twin freshness as site or cell health', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText('Degrading radio quality')).toBeInTheDocument()
    expect(screen.queryByText(/sourceHealth/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/twin freshness/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/digital twin/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/network knowledge/i)).not.toBeInTheDocument()
  })

  it('does not perform per-cell KPI, context, twin, or knowledge N+1 calls on overview', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture, acknowledgedCaseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect((await screen.findAllByText('Degrading radio quality')).length).toBeGreaterThan(0)
    const urls = fetchMock.mock.calls.map((call) => String(call[0]))
    const apiUrls = urls.filter((url) => url.startsWith('/api/v1/'))
    expect(apiUrls.sort()).toEqual(
      ['/api/v1/assurance/cases', '/api/v1/cells', '/api/v1/gnbs', '/api/v1/sites'].sort(),
    )
    expect(apiUrls.some((url) => url.includes('/context'))).toBe(false)
    expect(apiUrls.some((url) => url.includes('/kpis'))).toBe(false)
    expect(apiUrls.some((url) => url.includes('/telemetry'))).toBe(false)
    expect(apiUrls.some((url) => url.includes('/twins'))).toBe(false)
    expect(apiUrls.some((url) => url.includes('/integration/sync'))).toBe(false)
    expect(apiUrls.some((url) => /\/cells\/[^/]+\//.test(url))).toBe(false)
  })

  it('shows operator labels for degrading radio quality', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText('Degrading radio quality')).toBeInTheDocument()
    expect(summaryValue('Active Assurance cases')).toBe('1')
    expect(summaryValue('Critical active cases')).toBe('1')
    expect(summaryValue('Cells needing attention')).toBe('1')
  })

  it('shows Assurance failure as unavailable, not zero', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson({ error: 'assurance down' }, 500)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText('Request failed')).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: /Midband Demo Site/i })).toBeInTheDocument()
    expect(summaryValue('Sites')).toBe('1')
    expect(summaryValue('Active Assurance cases')).toBe('Unavailable')
    expect(summaryValue('Critical active cases')).toBe('Unavailable')
    expect(summaryValue('Cells needing attention')).toBe('Unavailable')
    expect(screen.queryByText('0 active')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Active Assurance issues' })).toBeInTheDocument()
    expect(screen.getByText('Request failed')).toBeInTheDocument()
  })

  it('shows inventory failure as unavailable, not zero', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson({ error: 'sites down' }, 500)
      if (url === '/api/v1/cells') return mockJson({ error: 'cells down' }, 500)
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText('Degrading radio quality')).toBeInTheDocument()
    expect(summaryValue('Sites')).toBe('Unavailable')
    expect(summaryValue('Cells')).toBe('Unavailable')
    expect(summaryValue('Active Assurance cases')).toBe('1')
    expect(screen.getAllByText('Unavailable').length).toBeGreaterThan(0)
  })

  it('shows an empty active queue when Assurance succeeds with no active cases', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([resolvedCaseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText('No active Assurance cases')).toBeInTheDocument()
    expect(summaryValue('Active Assurance cases')).toBe('0')
    expect(summaryValue('Critical active cases')).toBe('0')
    expect(summaryValue('Cells needing attention')).toBe('0')
    expect(screen.getByText('No active Assurance findings')).toBeInTheDocument()
  })

  it('filters the queue by status and severity together', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') {
        return mockJson([caseFixture, acknowledgedCaseFixture, warningCaseFixture, resolvedCaseFixture])
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Active Assurance issues' })).toBeInTheDocument()
    const queue = screen.getByRole('heading', { name: 'Active Assurance issues' }).closest('section')
    expect(queue).toBeTruthy()
    expect(within(queue as HTMLElement).getAllByRole('link', { name: 'Open case' })).toHaveLength(3)
    await user.selectOptions(screen.getByLabelText('Issue status'), 'Open')
    await user.selectOptions(screen.getByLabelText('Issue severity'), 'Critical')
    expect(within(queue as HTMLElement).getAllByRole('link', { name: 'Open case' })).toHaveLength(1)
    expect(within(queue as HTMLElement).getByText('CELL-001')).toBeInTheDocument()
    expect(within(queue as HTMLElement).queryByText('CELL-002')).not.toBeInTheDocument()
  })

  it('shows compact evidence preview on the issue queue', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByText(/Downlink BLER/)).toBeInTheDocument()
    expect(screen.getByText(/0\.12 ratio · DEGRADING/)).toBeInTheDocument()
  })
})
