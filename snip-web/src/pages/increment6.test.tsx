import { render, screen, waitFor, within } from '@testing-library/react'
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
  cellThreeFixture,
  cellTwoFixture,
  contextThreeFixture,
  contextTwoFixture,
  gnbFixture,
  gnbTwoFixture,
  investigationContextFixture,
  resolvedCaseFixture,
  siteFixture,
  unlocatedSiteFixture,
  warningCaseFixture,
} from '../test/fixtures'
import type { CellDto } from '../types/network'

vi.mock('../features/map/SiteMap', () => ({
  SiteMap: ({ sites }: { sites: Array<{ siteId: string; name: string }> }) => (
    <div data-testid="site-map">
      {sites.map((site) => (
        <div key={site.siteId}>{site.name}</div>
      ))}
    </div>
  ),
}))

const farCell: CellDto = {
  ...cellFixture,
  cellId: 'CELL-099',
  name: 'unrelated far cell',
  gnbId: 'GNB-099',
  siteId: 'SITE-099',
}

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

function apiUrls(fetchMock: ReturnType<typeof vi.fn>): string[] {
  return fetchMock.mock.calls.map((call) => String(call[0])).filter((url) => url.startsWith('/api/v1/'))
}

describe('Increment 6 multi-cell and site investigation workspace', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  function mockInvestigationApis(overrides?: {
    cellTwoContext?: Response
    includeFar?: boolean
  }) {
    const cells = overrides?.includeFar
      ? [cellFixture, cellTwoFixture, cellThreeFixture, farCell]
      : [cellFixture, cellTwoFixture, cellThreeFixture]
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture, unlocatedSiteFixture])
      if (url === '/api/v1/sites/SITE-001') return mockJson(siteFixture)
      if (url === '/api/v1/sites/SITE-002') return mockJson(unlocatedSiteFixture)
      if (url === '/api/v1/cells') return mockJson(cells)
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture, gnbTwoFixture])
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(investigationContextFixture)
      if (url === '/api/v1/cells/CELL-002/context') {
        return overrides?.cellTwoContext ?? mockJson(contextTwoFixture)
      }
      if (url === '/api/v1/cells/CELL-003/context') return mockJson(contextThreeFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([caseFixture, resolvedCaseFixture])
      if (url === '/api/v1/assurance/cases') {
        return mockJson([caseFixture, acknowledgedCaseFixture, warningCaseFixture, resolvedCaseFixture])
      }
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      if (url === '/api/v1/integration/sync/sources') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
  }

  it('renders related radio context with truthful relationships and comparison values', async () => {
    mockInvestigationApis({ includeFar: true })
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByRole('heading', { name: 'Related radio context' })).toBeInTheDocument()
    const related = screen.getByRole('heading', { name: 'Related radio context' }).closest('section') as HTMLElement
    expect(await within(related).findByText('n78-2 neighbour demo')).toBeInTheDocument()
    expect(await within(related).findByText('n78-3 cross-site neighbour demo')).toBeInTheDocument()
    expect(within(related).queryByText('CELL-099')).not.toBeInTheDocument()
    expect(within(related).queryByText('CELL-UNKNOWN')).not.toBeInTheDocument()

    const selectedRow = within(related).getByText('CELL-001').closest('tr') as HTMLElement
    expect(selectedRow).toHaveAttribute('aria-current', 'true')
    expect(within(selectedRow).getByText('Selected cell')).toBeInTheDocument()
    expect(within(selectedRow).getByText('46 dBm')).toBeInTheDocument()
    expect(within(selectedRow).getByText('0.12 ratio')).toBeInTheDocument()
    expect(within(selectedRow).getByText('Unavailable')).toBeInTheDocument()
    expect(within(selectedRow).getByText('Critical Assurance finding')).toBeInTheDocument()
    expect(within(selectedRow).getByText('Synthetic demo observation — not live network data.')).toBeInTheDocument()

    const sameSiteRow = within(related).getByText('CELL-002').closest('tr') as HTMLElement
    expect(within(sameSiteRow).getByText('Same gNB')).toBeInTheDocument()
    expect(within(sameSiteRow).getByText('Same site')).toBeInTheDocument()
    expect(within(sameSiteRow).getByText('Configured neighbour')).toBeInTheDocument()
    expect(within(sameSiteRow).queryByText('Cross-site neighbour')).not.toBeInTheDocument()
    expect(within(sameSiteRow).getByText('43 dBm')).toBeInTheDocument()
    expect(within(sameSiteRow).getByText('0.55 ratio')).toBeInTheDocument()
    expect(within(sameSiteRow).getByText('Major Assurance finding')).toBeInTheDocument()
    expect(within(sameSiteRow).queryByText('0')).not.toBeInTheDocument()

    const crossRow = within(related).getByText('CELL-003').closest('tr') as HTMLElement
    expect(within(crossRow).getByText('Configured neighbour')).toBeInTheDocument()
    expect(within(crossRow).getByText('Cross-site neighbour')).toBeInTheDocument()
    expect(within(crossRow).queryByText('Same site')).not.toBeInTheDocument()
    expect(within(crossRow).getByText('40 dBm')).toBeInTheDocument()
    expect(within(crossRow).getByText('No active Assurance finding')).toBeInTheDocument()
    expect(within(crossRow).queryByText('Synthetic demo observation — not live network data.')).not.toBeInTheDocument()

    expect(related.textContent).toMatch(/not coordinated multi-cell optimization/i)
    expect(related.textContent).not.toMatch(/interference|coverage overlap|handover quality/i)
    expect(screen.queryByRole('heading', { name: /site optimization|multi-cell optimization|site-aware ranking/i })).not.toBeInTheDocument()
    expect(screen.queryByText(/coverage polygon|sector wedge|azimuth|heatmap|RF propagation/i)).not.toBeInTheDocument()
  })

  it('navigates inventory-known neighbours and does not fabricate unknown targets', async () => {
    mockInvestigationApis()
    const user = userEvent.setup()
    signedIn('/network/cells/CELL-001')
    const neighbours = (await screen.findByRole('heading', { name: 'Neighbours' })).closest('section') as HTMLElement
    expect(await within(neighbours).findByText('INTRA_FREQ')).toBeInTheDocument()
    expect(within(neighbours).getAllByText('INTER_FREQUENCY').length).toBe(2)
    const unknownRow = within(neighbours).getByText('CELL-UNKNOWN').closest('tr') as HTMLElement
    expect(within(unknownRow).getByText('Not in inventory')).toBeInTheDocument()
    expect(within(unknownRow).queryByRole('link', { name: 'Open cell' })).not.toBeInTheDocument()
    const known = within(neighbours).getByText('CELL-002').closest('tr') as HTMLElement
    await user.click(within(known).getByRole('link', { name: 'Open cell' }))
    expect(await screen.findByRole('heading', { name: 'n78-2 neighbour demo' })).toBeInTheDocument()
  })

  it('hands off one cell to the existing PI3 Optimize route without generating a proposal', async () => {
    mockInvestigationApis()
    signedIn('/network/cells/CELL-001')
    const related = (await screen.findByRole('heading', { name: 'Related radio context' })).closest(
      'section',
    ) as HTMLElement
    const relatedOptimize = await within(related).findByRole('link', {
      name: 'Propose txPower optimization (CELL-002)',
    })
    expect(relatedOptimize).toHaveAttribute('href', '/network/cells/CELL-002/optimize')
    const headerOptimize = screen.getByRole('link', { name: /Propose txPower optimization \(46/i })
    expect(headerOptimize).toHaveAttribute('href', '/network/cells/CELL-001/optimize')
    await waitFor(() => {
      const urls = apiUrls(fetchMock)
      expect(urls.some((url) => url.includes('/change-intelligence/proposals'))).toBe(false)
      expect(urls.some((url) => url.includes('/sandbox'))).toBe(false)
      expect(urls.some((url) => url.includes('/agent-runs'))).toBe(false)
      expect(urls.some((url) => url.includes('/mcp'))).toBe(false)
      expect(urls.some((url) => url.includes('production-change'))).toBe(false)
      expect(urls.some((url) => url.includes('production-campaign'))).toBe(false)
    })
  })

  it('keeps related context bounded and does not crawl unknown or far cells', async () => {
    mockInvestigationApis({ includeFar: true })
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByText('n78-2 neighbour demo')).toBeInTheDocument()
    await waitFor(() => {
      const urls = apiUrls(fetchMock)
      expect(urls.filter((url) => url.endsWith('/context')).sort()).toEqual(
        [
          '/api/v1/cells/CELL-001/context',
          '/api/v1/cells/CELL-002/context',
          '/api/v1/cells/CELL-003/context',
        ].sort(),
      )
      expect(urls.some((url) => url.includes('CELL-UNKNOWN'))).toBe(false)
      expect(urls.some((url) => url.includes('CELL-099'))).toBe(false)
      expect(urls.filter((url) => url.includes('/assessment'))).toHaveLength(0)
      expect(urls.filter((url) => url.includes('/assurance') && url.includes('/cells/CELL-002'))).toHaveLength(0)
      expect(urls.some((url) => url.includes('/twins'))).toBe(false)
      expect(urls.some((url) => url.includes('synchronize'))).toBe(false)
    })
  })

  it('keeps selected-cell comparison values when a related context fails', async () => {
    mockInvestigationApis({
      cellTwoContext: mockJson({ error: 'CELL-002 context failed' }, 500),
    })
    signedIn('/network/cells/CELL-001')
    const related = (await screen.findByRole('heading', { name: 'Related radio context' })).closest(
      'section',
    ) as HTMLElement
    expect(await within(related).findByText('n78-3 cross-site neighbour demo')).toBeInTheDocument()
    const selectedRow = within(related).getByText('CELL-001').closest('tr') as HTMLElement
    expect(within(selectedRow).getByText('46 dBm')).toBeInTheDocument()
    expect(within(selectedRow).getByText('0.12 ratio')).toBeInTheDocument()
    const failedRow = within(related).getByText('CELL-002').closest('tr') as HTMLElement
    expect(within(failedRow).getAllByText('Unavailable').length).toBeGreaterThanOrEqual(3)
    expect(within(failedRow).queryByText('0')).not.toBeInTheDocument()
    expect(within(failedRow).queryByText('43 dBm')).not.toBeInTheDocument()
  })

  it('renders site cell comparison without claiming site optimization or inventing a selected cell', async () => {
    mockInvestigationApis()
    signedIn('/network/sites/SITE-001')
    expect(await screen.findByRole('heading', { name: 'Midband Demo Site' })).toBeInTheDocument()
    expect(screen.getByText('SITE-001')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Site cell comparison' })).toBeInTheDocument()
    const comparison = screen.getByRole('heading', { name: 'Site cell comparison' }).closest('section') as HTMLElement
    expect(await within(comparison).findByText('46 dBm')).toBeInTheDocument()
    expect(within(comparison).getByText('43 dBm')).toBeInTheDocument()
    expect(within(comparison).queryByText('Selected cell')).not.toBeInTheDocument()
    expect(within(comparison).queryByText('Same gNB')).not.toBeInTheDocument()
    expect(comparison.textContent).toMatch(/not site optimization/i)
    expect(screen.queryByRole('heading', { name: /site optimization/i })).not.toBeInTheDocument()
    expect(screen.getByText('Cells needing attention')).toBeInTheDocument()
    const cellTwoActions = within(comparison).getByText('CELL-002').closest('tr') as HTMLElement
    expect(within(cellTwoActions).getByRole('link', { name: 'Open cell' })).toHaveAttribute(
      'href',
      '/network/cells/CELL-002',
    )
    expect(
      within(cellTwoActions).getByRole('link', { name: 'Propose txPower optimization (CELL-002)' }),
    ).toHaveAttribute('href', '/network/cells/CELL-002/optimize')
  })

  it('preserves the PI5 Open cell path into related-cell context', async () => {
    mockInvestigationApis()
    const user = userEvent.setup()
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByRole('heading', { name: 'Degrading radio quality' })).toBeInTheDocument()
    await user.click(screen.getAllByRole('link', { name: 'Open cell' })[0])
    expect(await screen.findByRole('heading', { name: 'Related radio context' })).toBeInTheDocument()
    expect(screen.getAllByText('Selected cell').length).toBeGreaterThan(0)
  })

  it('does not call getCellContext from Network Operations', async () => {
    mockInvestigationApis({ includeFar: true })
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Network operations' })).toBeInTheDocument()
    const urls = apiUrls(fetchMock)
    expect(urls.some((url) => url.includes('/context'))).toBe(false)
    expect(urls.filter((url) => url.startsWith('/api/v1/')).sort()).toEqual(
      ['/api/v1/assurance/cases', '/api/v1/cells', '/api/v1/gnbs', '/api/v1/sites'].sort(),
    )
  })
})
