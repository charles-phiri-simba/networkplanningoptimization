import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { PLANNING_TRUTH } from '../features/planning/planningCopy'
import { AppRoutes } from '../routes/AppRoutes'
import {
  assessmentFixture,
  caseFixture,
  cellFixture,
  cellTwoFixture,
  gnbFixture,
  readyPlanFixture,
  siteFixture,
} from '../test/fixtures'
import { planId } from '../test/fixtures'
import type { PlanningEvaluationDto, PlanningScenarioDetailDto } from '../types/planning'

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
      intents: [{ id: 'i1', cellId: 'CELL-001', parameterId: 'txPower', intendedValue: 44 }],
    },
  ],
  evaluationView: 'EVALUATED',
  currentEvaluationId: '22222222-2222-4222-8222-222222222222',
  truth,
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
  itemCount: 1,
  succeededCount: 1,
  failedCount: 0,
  historical: false,
  items: [],
  truth,
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

function forbidden(text: string) {
  expect(text).not.toMatch(/Increment 1A/)
  expect(text).not.toMatch(/PI3/)
  expect(text).not.toMatch(/Phase 1[3-8]/)
  expect(text).not.toMatch(/Later increment/)
}

describe('SNIP 1.0 customer terminology', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('login has demo identity copy without actorId or increment jargon', async () => {
    sessionStorage.clear()
    render(
      <AuthProvider>
        <MemoryRouter initialEntries={['/login']}>
          <AppRoutes />
        </MemoryRouter>
      </AuthProvider>,
    )
    expect(await screen.findByRole('heading', { name: 'Select a demo persona' })).toBeInTheDocument()
    expect(screen.getByText(/frontend-only demo identity/i)).toBeInTheDocument()
    expect(screen.queryByText(/demo-rf-optimisation/)).not.toBeInTheDocument()
    forbidden(document.body.textContent ?? '')
  })

  it('hides Changes and Campaigns and keeps Digital Twin and sandbox truth', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/sites') return mockJson([siteFixture])
      if (url === '/api/v1/cells') return mockJson([cellFixture, cellTwoFixture])
      if (url === '/api/v1/gnbs') return mockJson([gnbFixture])
      if (url === '/api/v1/assurance/cases') return mockJson([caseFixture])
      return mockJson({ error: url }, 404)
    })
    signedIn('/network')
    expect(await screen.findByRole('heading', { name: 'Network operations' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Primary' })).not.toHaveTextContent('Changes')
    expect(screen.getByRole('navigation', { name: 'Primary' })).not.toHaveTextContent('Campaigns')
    expect(screen.getByText(/Synthetic demonstration locations/)).toBeInTheDocument()
    forbidden(document.body.textContent ?? '')
  })

  it('planning scenario uses Optimize handoff and shows comparison failures', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url.endsWith(`/planning/scenarios/${scenario.id}`) && !url.includes('prerequisites')) {
        return mockJson(scenario)
      }
      if (url.endsWith('/prerequisites')) {
        return mockJson({
          scenarioId: scenario.id,
          cells: [
            {
              cellId: 'CELL-001',
              twinPresent: true,
              twinId: 't1',
              latestVersion: 1,
              freshness: 'CURRENT',
              observedTxPower: 46,
              canEvaluate: true,
            },
          ],
        })
      }
      if (url.includes('/evaluations/')) return mockJson(evaluation)
      if (url.endsWith('/comparison')) return mockJson({ error: 'compare failed' }, 500)
      return mockJson({ error: url }, 404)
    })
    signedIn(`/planning/scenarios/${scenario.id}`)
    expect(await screen.findByRole('heading', { name: 'Optimize handoff' })).toBeInTheDocument()
    expect(await screen.findByRole('alert')).toHaveTextContent(/comparison could not be loaded/i)
    expect(screen.getAllByText(/CURRENT cell Digital Twin/i).length).toBeGreaterThan(0)
    expect(PLANNING_TRUTH).toMatch(/synthetic/i)
    forbidden(document.body.textContent ?? '')
  })

  it('Ask SNIP has no raw API path copy', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      if (String(input) === '/api/v1/cells') return mockJson([cellFixture])
      return mockJson({ error: String(input) }, 404)
    })
    signedIn('/ai')
    expect(await screen.findByRole('heading', { name: 'AI explanation' })).toBeInTheDocument()
    expect(document.body.textContent).not.toMatch(/\/api\/v1\/recommendations/)
    expect(screen.getAllByText(/decision support/i).length).toBeGreaterThan(0)
    forbidden(document.body.textContent ?? '')
  })

  it('assurance and not-found pages drop increment jargon', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}`) return mockJson(caseFixture)
      if (url === `/api/v1/assurance/cases/${caseFixture.id}/assessment`) return mockJson(assessmentFixture)
      return mockJson({ error: url }, 404)
    })
    signedIn(`/assurance/${caseFixture.id}`)
    expect(await screen.findByText(/existing Optimize workflow/)).toBeInTheDocument()
    forbidden(document.body.textContent ?? '')

    signedIn('/missing-route')
    expect(await screen.findByText('That page is not part of SNIP.')).toBeInTheDocument()
    forbidden(document.body.textContent ?? '')
  })

  it('maps READY_FOR_EXECUTION to sandbox admission without implying a real write', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      if (String(input).includes(`/change-planning/plans/${planId}`)) return mockJson(readyPlanFixture)
      return mockJson({ error: String(input) }, 404)
    })
    signedIn(`/change-plans/${planId}`)
    expect(await screen.findAllByText(/READY FOR SANDBOX ADMISSION/)).not.toHaveLength(0)
    await waitFor(() => {
      expect(document.body.textContent).not.toMatch(/Backend status remains READY_FOR_EXECUTION/)
    })
    expect(screen.getByText(/NO REAL NETWORK CHANGE/)).toBeInTheDocument()
  })
})
