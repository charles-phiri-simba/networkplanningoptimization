import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { containsForbiddenCapabilityClaim, PLANNING_TRUTH } from '../features/planning/planningCopy'
import { AppRoutes } from '../routes/AppRoutes'
import { cellFixture, cellTwoFixture, contextFixture, gnbFixture, siteFixture } from '../test/fixtures'
import type { PlanningEvaluationDto, PlanningScenarioDetailDto, PlanningScenarioSummaryDto } from '../types/planning'

const truth = {
  independentCellLocal: true,
  jointSiteSimulation: false,
  crossCellEffectsModelled: false,
  synthetic: true,
  modelId: 'snip.synthetic.cell-parameter.v1',
  confidence: 'LOW',
}

const scenario: PlanningScenarioDetailDto = {
  id: '11111111-1111-4111-8111-111111111111',
  name: 'SITE-001 what-if',
  description: '',
  createdBy: 'demo-network-engineer',
  createdAt: '2026-10-07T00:00:00Z',
  updatedAt: '2026-10-07T00:00:00Z',
  rowVersion: 1,
  cells: [
    { id: 'c1', cellId: 'CELL-001', ordinal: 1 },
    { id: 'c2', cellId: 'CELL-002', ordinal: 2 },
  ],
  alternatives: [
    {
      id: 'a1',
      name: 'Alternative A',
      ordinal: 1,
      intents: [
        { id: 'i1', cellId: 'CELL-001', parameterId: 'txPower', intendedValue: 44 },
        { id: 'i2', cellId: 'CELL-002', parameterId: 'txPower', intendedValue: 45 },
      ],
    },
  ],
  evaluationView: 'NOT_EVALUATED',
  currentEvaluationId: null,
  truth,
}

const evaluated: PlanningScenarioDetailDto = {
  ...scenario,
  evaluationView: 'EVALUATED',
  currentEvaluationId: '22222222-2222-4222-8222-222222222222',
}

const evaluation: PlanningEvaluationDto = {
  id: '22222222-2222-4222-8222-222222222222',
  scenarioId: scenario.id,
  status: 'SUCCEEDED',
  intentFingerprint: 'a'.repeat(64),
  admissionFingerprint: 'b'.repeat(64),
  createdBy: 'demo-network-engineer',
  createdAt: '2026-10-07T00:00:00Z',
  completedAt: '2026-10-07T00:00:01Z',
  itemCount: 2,
  succeededCount: 2,
  failedCount: 0,
  historical: false,
  items: [
    {
      id: 'e1',
      evaluationId: '22222222-2222-4222-8222-222222222222',
      alternativeId: 'a1',
      alternativeName: 'Alternative A',
      alternativeOrdinal: 1,
      cellId: 'CELL-001',
      parameterId: 'txPower',
      intendedValue: 44,
      outcome: 'SUCCEEDED',
      failureCode: null,
      failureMessage: null,
      twinId: 't1',
      twinVersion: 1,
      pinnedBaselineTxPower: 46,
      configurationFingerprint: 'fp',
      simulationScenarioId: 's1',
      simulationRunId: 'r1',
      modelId: 'snip.synthetic.cell-parameter.v1',
      modelVersion: '1.0',
      synthetic: true,
      confidence: 'LOW',
      reusedExistingRun: false,
    },
    {
      id: 'e2',
      evaluationId: '22222222-2222-4222-8222-222222222222',
      alternativeId: 'a1',
      alternativeName: 'Alternative A',
      alternativeOrdinal: 1,
      cellId: 'CELL-002',
      parameterId: 'txPower',
      intendedValue: 45,
      outcome: 'SUCCEEDED',
      failureCode: null,
      failureMessage: null,
      twinId: 't2',
      twinVersion: 1,
      pinnedBaselineTxPower: 43,
      configurationFingerprint: 'fp',
      simulationScenarioId: 's2',
      simulationRunId: 'r2',
      modelId: 'snip.synthetic.cell-parameter.v1',
      modelVersion: '1.0',
      synthetic: true,
      confidence: 'LOW',
      reusedExistingRun: false,
    },
  ],
  truth,
}

const partialEvaluation: PlanningEvaluationDto = {
  ...evaluation,
  status: 'PARTIAL',
  failedCount: 1,
  succeededCount: 1,
  historical: true,
  items: [
    evaluation.items[0],
    { ...evaluation.items[1], outcome: 'FAILED', failureCode: 'ADMISSION_TWIN_NOT_CURRENT', failureMessage: 'STALE', simulationRunId: null },
  ],
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

describe('Increment 7 what-if configuration scenario workspace', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  function apiUrls(): string[] {
    return fetchMock.mock.calls.map((call) => String(call[0])).filter((url) => url.startsWith('/api/v1/'))
  }

  it('shows Planning nav and empty list', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/planning/scenarios') return mockJson([])
      return mockJson({ error: url }, 404)
    })
    signedIn('/planning')
    expect(await screen.findByRole('heading', { name: 'What-if configuration scenarios' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Primary' })).toHaveTextContent('Planning')
    expect(screen.getByText(/Independent cell-local synthetic evaluation/)).toBeInTheDocument()
    expect(screen.getByText('No what-if scenarios yet')).toBeInTheDocument()
    expect(containsForbiddenCapabilityClaim(document.body.textContent ?? '')).toBe(false)
    expect(PLANNING_TRUTH).toContain('LOW')
  })

  it('creates a scenario from selected cells and does not auto-evaluate', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/planning/scenarios' && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body.cells).toEqual([{ cellId: 'CELL-001' }, { cellId: 'CELL-002' }])
        expect(body.alternatives[0].intents).toEqual([
          { cellId: 'CELL-001', parameterId: 'txPower', intendedValue: 44 },
          { cellId: 'CELL-002', parameterId: 'txPower', intendedValue: 44 },
        ])
        return mockJson(scenario, 201)
      }
      if (url.endsWith(`/planning/scenarios/${scenario.id}`)) return mockJson(scenario)
      if (url.endsWith('/prerequisites')) {
        return mockJson({
          scenarioId: scenario.id,
          cells: [
            { cellId: 'CELL-001', twinPresent: false, twinId: null, latestVersion: null, freshness: 'MISSING', observedTxPower: null, canEvaluate: false },
            { cellId: 'CELL-002', twinPresent: false, twinId: null, latestVersion: null, freshness: 'MISSING', observedTxPower: null, canEvaluate: false },
          ],
        })
      }
      return mockJson({ error: url }, 404)
    })
    signedIn('/planning/new?cells=CELL-001,CELL-002')
    expect(await screen.findByRole('heading', { name: 'Create what-if scenario' })).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: /CELL-001/ })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: /CELL-002/ })).toBeChecked()
    await user.click(screen.getByRole('button', { name: 'Create scenario' }))
    expect(await screen.findByRole('heading', { name: 'SITE-001 what-if' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Evaluate' })).toBeInTheDocument()
    expect(apiUrls().some((url) => url.includes('/evaluations'))).toBe(false)
  })

  it('checking cells without editing still submits displayed txPower intents', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/planning/scenarios' && init?.method === 'POST') {
        const body = JSON.parse(String(init.body))
        expect(body.cells.map((cell: { cellId: string }) => cell.cellId)).toEqual(['CELL-001', 'CELL-002'])
        expect(body.alternatives[0].intents).toEqual([
          { cellId: 'CELL-001', parameterId: 'txPower', intendedValue: 44 },
          { cellId: 'CELL-002', parameterId: 'txPower', intendedValue: 44 },
        ])
        return mockJson(scenario, 201)
      }
      if (url.endsWith(`/planning/scenarios/${scenario.id}`)) return mockJson(scenario)
      if (url.endsWith('/prerequisites')) {
        return mockJson({ scenarioId: scenario.id, cells: [] })
      }
      return mockJson({ error: url }, 404)
    })
    signedIn('/planning/new')
    await user.click(await screen.findByRole('checkbox', { name: /CELL-001/ }))
    await user.click(screen.getByRole('checkbox', { name: /CELL-002/ }))
    await user.click(screen.getByRole('button', { name: 'Create scenario' }))
    expect(await screen.findByRole('heading', { name: 'SITE-001 what-if' })).toBeInTheDocument()
  })

  it('shows stale prerequisite synchronize without auto-evaluate', async () => {
    const user = userEvent.setup()
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith(`/planning/scenarios/${scenario.id}`) && !url.includes('prerequisites')) return mockJson(scenario)
      if (url.endsWith('/prerequisites')) {
        return mockJson({
          scenarioId: scenario.id,
          cells: [
            { cellId: 'CELL-001', twinPresent: true, twinId: 't1', latestVersion: 1, freshness: 'STALE', observedTxPower: 46, canEvaluate: false },
          ],
        })
      }
      if (url.includes('/twins/cells/CELL-001/synchronize') && init?.method === 'POST') {
        return mockJson({ id: 't1', latestVersion: 2, freshness: 'CURRENT' })
      }
      return mockJson({ error: url }, 404)
    })
    signedIn(`/planning/scenarios/${scenario.id}`)
    expect(await screen.findByRole('button', { name: 'Synchronize twin' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Synchronize twin' }))
    await waitFor(() => expect(apiUrls().some((url) => url.includes('/synchronize'))).toBe(true))
    expect(apiUrls().filter((url) => url.includes('/evaluations')).length).toBe(0)
  })

  it('evaluates explicitly and shows synthetic LOW comparison and one-cell optimize handoff', async () => {
    const user = userEvent.setup()
    let evaluatedOnce = false
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url.endsWith(`/planning/scenarios/${scenario.id}`) && !url.includes('prerequisites') && !url.includes('evaluations') && !url.includes('comparison')) {
        return mockJson(evaluatedOnce ? evaluated : scenario)
      }
      if (url.endsWith('/prerequisites')) {
        return mockJson({
          scenarioId: scenario.id,
          cells: [
            { cellId: 'CELL-001', twinPresent: true, twinId: 't1', latestVersion: 1, freshness: 'CURRENT', observedTxPower: 46, canEvaluate: true },
            { cellId: 'CELL-002', twinPresent: true, twinId: 't2', latestVersion: 1, freshness: 'CURRENT', observedTxPower: 43, canEvaluate: true },
          ],
        })
      }
      if (url.endsWith('/evaluations') && init?.method === 'POST') {
        evaluatedOnce = true
        return mockJson(evaluation, 201)
      }
      if (url.includes('/evaluations/')) return mockJson(evaluation)
      if (url.endsWith('/comparison')) {
        return mockJson({
          scenarioId: scenario.id,
          evaluationId: evaluation.id,
          evaluationStatus: 'SUCCEEDED',
          intentFingerprint: evaluation.intentFingerprint,
          historical: false,
          rows: evaluation.items.map((item) => ({
            alternativeId: item.alternativeId,
            alternativeName: item.alternativeName,
            alternativeOrdinal: item.alternativeOrdinal,
            cellId: item.cellId,
            parameterId: item.parameterId,
            baselineTxPower: item.pinnedBaselineTxPower,
            intendedTxPower: item.intendedValue,
            metrics: [],
            confidence: item.confidence,
            synthetic: item.synthetic,
            modelId: item.modelId,
            modelVersion: item.modelVersion,
            twinId: item.twinId,
            twinVersion: item.twinVersion,
            simulationRunId: item.simulationRunId,
            outcome: item.outcome,
          })),
          truth,
        })
      }
      if (url.includes('/simulations/')) {
        return mockJson({
          id: 'r1',
          synthetic: true,
          confidence: 'LOW',
          limitations: ['NO_RF_PROPAGATION_MODEL'],
          assumptions: ['Isolated cell; neighbour coupling is not modelled.'],
          metrics: [],
        })
      }
      return mockJson({ error: url }, 404)
    })
    signedIn(`/planning/scenarios/${scenario.id}`)
    await user.click(await screen.findByRole('button', { name: 'Evaluate' }))
    expect(await screen.findByRole('heading', { name: 'Per-cell comparison' })).toBeInTheDocument()
    expect(screen.getAllByText(/SYNTHETIC|LOW/).length).toBeGreaterThan(0)
    expect(screen.getByRole('link', { name: 'Open Optimize for this cell' })).toHaveAttribute(
      'href',
      '/network/cells/CELL-001/optimize',
    )
    expect(apiUrls().some((url) => url.includes('change-intelligence/proposals'))).toBe(false)
    expect(apiUrls().some((url) => url.includes('/agent-runs'))).toBe(false)
    expect(apiUrls().some((url) => url.includes('/mcp'))).toBe(false)
    expect(apiUrls().some((url) => url.includes('production-change'))).toBe(false)
    expect(document.body.textContent).not.toMatch(/site health score|best site configuration/i)
    expect(containsForbiddenCapabilityClaim(PLANNING_TRUTH)).toBe(false)
  })

  it('labels partial and historical evaluation', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url.endsWith(`/planning/scenarios/${scenario.id}`) && !url.includes('prerequisites')) {
        return mockJson({ ...evaluated, evaluationView: 'PARTIAL' })
      }
      if (url.endsWith('/prerequisites')) {
        return mockJson({ scenarioId: scenario.id, cells: [] })
      }
      if (url.includes('/evaluations/')) return mockJson(partialEvaluation)
      if (url.endsWith('/comparison')) {
        return mockJson({
          scenarioId: scenario.id,
          evaluationId: partialEvaluation.id,
          evaluationStatus: 'PARTIAL',
          intentFingerprint: partialEvaluation.intentFingerprint,
          historical: true,
          rows: [],
          truth,
        })
      }
      return mockJson({ error: url }, 404)
    })
    signedIn(`/planning/scenarios/${scenario.id}`)
    expect((await screen.findAllByText(/Partially evaluated/)).length).toBeGreaterThan(0)
    expect(screen.getByText(/Historical evaluation against pinned Digital Twin versions/)).toBeInTheDocument()
  })

  it('offers Site and Cell what-if entry without dumping related cells', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites/SITE-001') return mockJson(siteFixture)
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([])
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      if (url === '/api/v1/integration/sync/sources') return mockJson([])
      return mockJson({ error: url }, 404)
    })
    signedIn('/network/sites/SITE-001')
    expect(await screen.findByRole('link', { name: 'Create what-if scenario' })).toHaveAttribute(
      'href',
      '/planning/new?cells=CELL-001,CELL-002',
    )
  })

  it('lists an existing scenario', async () => {
    const summary: PlanningScenarioSummaryDto = {
      id: scenario.id,
      name: scenario.name,
      createdBy: scenario.createdBy,
      createdAt: scenario.createdAt,
      updatedAt: scenario.updatedAt,
      rowVersion: 1,
      cellCount: 2,
      alternativeCount: 1,
      evaluationView: 'NOT_EVALUATED',
      truth,
    }
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      if (String(input) === '/api/v1/planning/scenarios') return mockJson([summary])
      return mockJson({ error: String(input) }, 404)
    })
    signedIn('/planning')
    expect(await screen.findByRole('link', { name: 'SITE-001 what-if' })).toBeInTheDocument()
  })
})
