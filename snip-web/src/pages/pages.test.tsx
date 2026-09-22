import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { storeDemoIdentity, DEMO_PERSONAS } from '../features/auth/demoIdentity'
import { AppRoutes } from '../routes/AppRoutes'
import {
  assessmentFixture,
  caseFixture,
  cellFixture,
  contextFixture,
  gnbFixture,
  recommendationFixture,
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

describe('Increment 1A pages', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo) => {
        const url = String(input)
        if (url === '/api/v1/sites') return mockJson([siteFixture])
        if (url === '/api/v1/sites/SITE-001') return mockJson(siteFixture)
        if (url === '/api/v1/cells') return mockJson([cellFixture])
        if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
        if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
        if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([caseFixture])
        if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
        if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
        if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
        if (url === '/api/v1/recommendations') return mockJson(recommendationFixture)
        return mockJson({ error: 'not mocked ' + url }, 404)
      }),
    )
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('renders the application shell navigation', async () => {
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Network' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Primary' })).toHaveTextContent('Network')
    expect(screen.getByRole('navigation', { name: 'Primary' })).toHaveTextContent('Assurance')
    expect(screen.getByRole('navigation', { name: 'Primary' })).toHaveTextContent('AI')
    expect(screen.getByText('Optimization')).toBeInTheDocument()
  })

  it('renders network sites from the API', async () => {
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Network' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Midband Demo Site/i })).toBeInTheDocument()
    expect(screen.getByTestId('site-map')).toHaveTextContent('Midband Demo Site')
  })

  it('renders site associated cells', async () => {
    signedIn('/network/sites/SITE-001')
    expect(await screen.findByRole('heading', { name: 'Midband Demo Site' })).toBeInTheDocument()
    expect(screen.getByText('CELL-001', { exact: false })).toBeInTheDocument()
    expect(screen.getByText('DemoVendor')).toBeInTheDocument()
  })

  it('renders cell configuration, KPIs, and assurance', async () => {
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByRole('heading', { name: 'n78-1 high-BLER demo' })).toBeInTheDocument()
    expect(screen.getByText('txPower')).toBeInTheDocument()
    expect(screen.getByText('46')).toBeInTheDocument()
    expect(screen.getAllByText('BLER_DL').length).toBeGreaterThan(0)
    expect(screen.getByText('DEGRADING_RADIO_QUALITY')).toBeInTheDocument()
    expect(screen.getByText(/synthetic \/ demo data/i)).toBeInTheDocument()
  })

  it('renders an assurance case and assessment', async () => {
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByRole('heading', { name: 'DEGRADING_RADIO_QUALITY' })).toBeInTheDocument()
    expect(screen.getByText('RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1')).toBeInTheDocument()
    expect(screen.getByText(/Downlink BLER is above the critical threshold/)).toBeInTheDocument()
    expect(screen.getByText('Review neighbours')).toBeInTheDocument()
  })

  it('requests an AI explanation and shows the response', async () => {
    const user = userEvent.setup()
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByRole('heading', { name: 'Ask SNIP' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Explain this cell' }))
    expect(
      await screen.findByText(/Investigate downlink BLER on CELL-001/),
    ).toBeInTheDocument()
    expect(screen.getByText(/does not execute network changes/i)).toBeInTheDocument()
  })
})
