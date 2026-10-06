import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { selectedCandidateRank } from '../features/optimization/proposalGuards'
import { AppRoutes } from '../routes/AppRoutes'
import {
  contextFixture,
  enabledSourceFixture,
  currentTwinFixture,
  evaluatedProposalFixture,
  executionEvidenceFixture,
  invalidTwinProposalFixture,
  executionId,
  knowledgeAbsentFixture,
  knowledgeCurrentFixture,
  knowledgeRecoveryFixture,
  planId,
  proposalId,
  readyForReviewPlanFixture,
  readyPlanFixture,
  recommendedProposalFixture,
  simulationFixture,
  simulationId,
  verifiedExecutionFixture,
} from '../test/fixtures'
import {
  EXECUTION_PERMISSION_HEADER,
  ExecutionPermission,
  SIMULATOR_EXECUTION_TARGET_ID,
} from '../types/execution'
import { VENDOR_IMPORT_PERMISSION_HEADER, VendorImportPermission } from '../types/sync'

function signedIn(path: string) {
  storeDemoIdentity(DEMO_PERSONAS[1])
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
      </MemoryRouter>
    </AuthProvider>,
  )
}

function mockJson(data: unknown, status = 200, failureCode?: string) {
  return new Response(JSON.stringify(failureCode ? { error: data, failureCode } : data), {
    status,
    headers: { 'Content-Type': 'application/json', 'X-Correlation-Id': 'test-corr' },
  })
}

function knowledgeUrls(state: typeof knowledgeCurrentFixture) {
  return async (input: RequestInfo) => {
    const url = String(input)
    if (url === '/api/v1/integration/sync/sources') return mockJson([enabledSourceFixture])
    if (url === '/api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT') {
      return mockJson(state)
    }
    return null
  }
}

describe('Increment 3 operator workspace', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('shows knowledge absence without inventing confidence', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeAbsentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByText('Network knowledge has not been established')).toBeInTheDocument()
    expect(screen.queryByText('HIGH')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Recover network knowledge' })).not.toBeInTheDocument()
    expect(screen.getByText(/not a per-cell measurement/i)).toBeInTheDocument()
  })

  it('shows current knowledge and does not offer recovery', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByText('Network knowledge current')).toBeInTheDocument()
    expect(screen.getByText('TRUSTED_FRESH_COMPLETE')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Recover network knowledge' })).not.toBeInTheDocument()
  })

  it('requires confirmation before recovery and refreshes after success', async () => {
    let state = knowledgeRecoveryFixture
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url === '/api/v1/integration/sync/sources') return mockJson([enabledSourceFixture])
      if (url === '/api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT') {
        return mockJson(state)
      }
      if (url.endsWith('/recovery') && init?.method === 'POST') {
        state = knowledgeCurrentFixture
        return mockJson({ status: 'COMPLETED' })
      }
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network/cells/CELL-001')
    expect(await screen.findByText('Network knowledge needs recovery')).toBeInTheDocument()
    expect(screen.queryByText('TRIGGER_RECOVERY_SYNCHRONIZATION')).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Recover network knowledge' }))
    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('does not modify the network')
    expect(dialog).toHaveTextContent('does not authorize a network change')
    await user.click(within(dialog).getByRole('button', { name: 'Recover network knowledge' }))
    expect(await screen.findByText('Network knowledge current')).toBeInTheDocument()
    const recoveryCall = fetchMock.mock.calls.find(([url]) => String(url).endsWith('/recovery'))
    expect(recoveryCall).toBeTruthy()
    expect(new Headers((recoveryCall?.[1] as RequestInit).headers).get(VENDOR_IMPORT_PERMISSION_HEADER)).toBe(
      VendorImportPermission.RECOVERY,
    )
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/recovery'))).toHaveLength(1)
  })

  it('shows recovery failure without generating a proposal', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const url = String(input)
      if (url === '/api/v1/integration/sync/sources') return mockJson([enabledSourceFixture])
      if (url === '/api/v1/integration/sync/sources/ERICSSON_ENM_SIMULATOR/DEFAULT') {
        return mockJson(knowledgeRecoveryFixture)
      }
      if (url.endsWith('/recovery') && init?.method === 'POST') {
        return mockJson('recovery failed', 409, 'RECOVERY_REQUIRED')
      }
      if (url === '/api/v1/cells/CELL-001/optimize') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network/cells/CELL-001')
    await user.click(await screen.findByRole('button', { name: 'Recover network knowledge' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Recover network knowledge' }))
    expect((await screen.findAllByText('Network knowledge needs recovery')).length).toBeGreaterThan(0)
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(false)
  })

  it('explains TWIN_STATE_UNAVAILABLE without converting INVALID or auto-synchronizing', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(invalidTwinProposalFixture)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByRole('heading', { name: 'Digital Twin required' })).toBeInTheDocument()
    expect(screen.getByText(/CURRENT Digital Twin is not available/)).toBeInTheDocument()
    expect(screen.getByText(/does not modify the real network/)).toBeInTheDocument()
    expect(screen.getByText(/does not authorize a network change/)).toBeInTheDocument()
    expect(screen.getByText('INVALID', { selector: '.badge *' })).toBeInTheDocument()
    expect(screen.getAllByText(/TWIN_STATE_UNAVAILABLE/).length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Synchronize Digital Twin' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve proposal' })).not.toBeInTheDocument()
    expect(screen.queryByText(/Increment 2/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Increment 3/)).not.toBeInTheDocument()
    expect(screen.getByText(/SNIP Demo Environment/)).toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).includes('/twins/cells/') && String(url).endsWith('/synchronize')),
    ).toBe(false)
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(
      false,
    )
    expect(
      fetchMock.mock.calls.some(([url]) =>
        /\/api\/v1\/(production-changes|production-campaigns)|\/mcp$/.test(String(url)),
      ),
    ).toBe(false)
  })

  it('synchronizes the Digital Twin only after an explicit action and does not regenerate', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(invalidTwinProposalFixture)
      }
      if (url === '/api/v1/twins/cells/CELL-001/synchronize' && init?.method === 'POST') {
        return mockJson(currentTwinFixture)
      }
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByRole('heading', { name: 'Digital Twin required' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/synchronize'))).toHaveLength(0)
    await user.click(screen.getByRole('button', { name: 'Synchronize Digital Twin' }))
    const dialog = await screen.findByRole('dialog')
    expect(dialog).toHaveTextContent('does not modify the real network')
    expect(dialog).toHaveTextContent('does not authorize a network change')
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/synchronize'))).toHaveLength(0)
    await user.click(within(dialog).getByRole('button', { name: 'Synchronize Digital Twin' }))
    expect(await screen.findByText(/Digital Twin is CURRENT/)).toBeInTheDocument()
    expect(screen.getByText(/This INVALID proposal remains INVALID/)).toBeInTheDocument()
    expect(screen.getByText('INVALID', { selector: '.badge *' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Synchronize Digital Twin' })).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/synchronize'))).toHaveLength(1)
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(
      false,
    )
    expect(
      fetchMock.mock.calls.some(([url]) =>
        /\/api\/v1\/(production-changes|production-campaigns)|\/mcp$/.test(String(url)),
      ),
    ).toBe(false)
    await user.click(screen.getByRole('link', { name: 'Generate a new optimization proposal' }))
    expect(await screen.findByRole('heading', { name: 'Propose txPower optimization' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Generate txPower proposal' })).toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(
      false,
    )
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/synchronize'))).toHaveLength(1)
  })

  it('shows the real backend result when Digital Twin synchronization fails', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(invalidTwinProposalFixture)
      }
      if (url === '/api/v1/twins/cells/CELL-001/synchronize' && init?.method === 'POST') {
        return mockJson({ error: 'cell not found' }, 404)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/optimization/proposals/${proposalId}`)
    await user.click(await screen.findByRole('button', { name: 'Synchronize Digital Twin' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Synchronize Digital Twin' }))
    expect(await screen.findByText('cell not found')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Digital Twin required' })).toBeInTheDocument()
    expect(screen.queryByText(/Digital Twin is CURRENT/)).not.toBeInTheDocument()
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(
      false,
    )
  })

  it('does not synchronize when opening Optimize', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network/cells/CELL-001/optimize')
    expect(await screen.findByRole('button', { name: 'Generate txPower proposal' })).toBeInTheDocument()
    expect(screen.getByText(/SNIP Demo Environment/)).toBeInTheDocument()
    expect(screen.queryByText(/Increment 2/)).not.toBeInTheDocument()
    expect(
      fetchMock.mock.calls.some(([url]) => String(url).includes('/twins/cells/') && String(url).endsWith('/synchronize')),
    ).toBe(false)
    expect(fetchMock.mock.calls.some(([url]) => String(url) === '/api/v1/change-intelligence/proposals')).toBe(
      false,
    )
  })

  it('explains EVALUATED eligibility and keeps it historical', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeRecoveryFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(evaluatedProposalFixture)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByText('Recommendation withheld')).toBeInTheDocument()
    expect(screen.getAllByText(/NETWORK_KNOWLEDGE_LOW/).length).toBeGreaterThan(0)
    expect(screen.getByText(/Do not reuse this EVALUATED proposal/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve proposal' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Optimization proposal' })).toBeInTheDocument()
    expect(screen.getByText('EVALUATED', { selector: '.badge *' })).toBeInTheDocument()
  })

  it('marks rank 1 as backend recommendation and does not rerank', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(recommendedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByText('Backend recommendation')).toBeInTheDocument()
    expect(selectedCandidateRank(1)).toBe(true)
    expect(selectedCandidateRank(2)).toBe(false)
    expect(screen.queryByRole('button', { name: /select candidate/i })).not.toBeInTheDocument()
  })

  it('keeps synthetic simulation wording and does not leak REST paths', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(recommendedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByText('SYNTHETIC SIMULATION')).toBeInTheDocument()
    expect(screen.getByText(/NOT VENDOR-CALIBRATED RF/)).toBeInTheDocument()
    expect(screen.getByText(/NO REAL NETWORK CHANGE/)).toBeInTheDocument()
    expect(await screen.findByText('Downlink BLER')).toBeInTheDocument()
    expect(screen.getByText('BLER_DL')).toBeInTheDocument()
    expect(screen.queryByText(/GET \/api\/v1\/simulations/)).not.toBeInTheDocument()
    expect(screen.queryByText(/Phase 13/)).not.toBeInTheDocument()
  })

  it('navigates to an existing plan on ACTIVE_PLAN_EXISTS', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo, init?: RequestInit) => {
      const knowledge = await knowledgeUrls(knowledgeCurrentFixture)(input)
      if (knowledge) return knowledge
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson({ ...recommendedProposalFixture, proposal: { ...recommendedProposalFixture.proposal, status: 'APPROVED' } })
      }
      if (url === '/api/v1/change-planning/plans' && init?.method === 'POST') {
        return mockJson(planId, 409, 'ACTIVE_PLAN_EXISTS')
      }
      if (url === `/api/v1/change-planning/plans/${planId}`) return mockJson(readyForReviewPlanFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/optimization/proposals/${proposalId}`)
    await user.click(await screen.findByRole('button', { name: 'Create change plan' }))
    expect(await screen.findByText('An active change plan already exists for this proposal.')).toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: 'Open existing plan' }))
    expect(await screen.findByRole('heading', { name: 'Change plan' })).toBeInTheDocument()
  })

  it('translates readiness checks and sandbox context', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-planning/plans/${planId}`) return mockJson(readyPlanFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/change-plans/${planId}`)
    expect(await screen.findByText('Current parameter matches expected value')).toBeInTheDocument()
    expect(screen.getByText('Network knowledge confidence')).toBeInTheDocument()
    expect(screen.getByText('Authorization is current')).toBeInTheDocument()
    expect(screen.queryByText(/Phase 14/)).not.toBeInTheDocument()
    expect(screen.queryByText(/POST \/plans/)).not.toBeInTheDocument()
  })

  it('shows verification evidence, simulator-only warning, and four-way comparison', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-execution/executions/${executionId}`) {
        return mockJson(verifiedExecutionFixture)
      }
      if (url === `/api/v1/change-execution/executions/${executionId}/evidence`) {
        return mockJson(executionEvidenceFixture)
      }
      if (url === `/api/v1/change-planning/plans/${planId}`) return mockJson(readyPlanFixture)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(recommendedProposalFixture)
      }
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/sandbox/executions/${executionId}`)
    expect(await screen.findByRole('heading', { name: 'Sandbox context' })).toBeInTheDocument()
    expect(screen.getAllByText(SIMULATOR_EXECUTION_TARGET_ID).length).toBeGreaterThan(0)
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(await screen.findByText('Expected simulator value')).toBeInTheDocument()
    expect(screen.getByText('Observed simulator value')).toBeInTheDocument()
    expect(screen.getAllByText(/42/).length).toBeGreaterThan(0)
    expect(screen.getByText(/This verifies simulator readback only/)).toBeInTheDocument()
    expect(screen.queryByText(/Production verified/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Network verified/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Phase 15/)).not.toBeInTheDocument()
    expect(await screen.findByText('Recommended, sandbox, canonical, and real network')).toBeInTheDocument()
    expect(screen.getByText(/No real-network execution was performed/)).toBeInTheDocument()
    expect(screen.getByText('46 dBm')).toBeInTheDocument()
    const evidenceCall = fetchMock.mock.calls.find(([url]) =>
      String(url).endsWith(`/executions/${executionId}/evidence`),
    )
    expect(new Headers((evidenceCall?.[1] as RequestInit).headers).get(EXECUTION_PERMISSION_HEADER)).toBe(
      ExecutionPermission.VIEW,
    )
  })

  it('translates operator errors without hiding failure codes', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-planning/plans/${planId}`) {
        return mockJson('execution disabled', 403, 'CHANGE_EXECUTION_DISABLED')
      }
      return mockJson({ error: 'not mocked' }, 404)
    })
    signedIn(`/change-plans/${planId}`)
    expect(await screen.findByText('Sandbox execution is disabled')).toBeInTheDocument()
    expect(screen.getByText('failureCode CHANGE_EXECUTION_DISABLED')).toBeInTheDocument()
  })
})
