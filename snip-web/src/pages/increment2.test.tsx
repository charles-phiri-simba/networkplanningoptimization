import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider } from '../features/auth/AuthContext'
import { DEMO_PERSONAS, storeDemoIdentity } from '../features/auth/demoIdentity'
import { AppRoutes } from '../routes/AppRoutes'
import {
  approvedProposalFixture,
  authorizedExecutionFixture,
  authorizedPlanFixture,
  contextFixture,
  executionId,
  planId,
  proposalId,
  readyForReviewPlanFixture,
  readyPlanFixture,
  recommendedProposalFixture,
  reviewableExecutionFixture,
  reviewedPlanFixture,
  simulationFixture,
  simulationId,
  verifiedExecutionFixture,
} from '../test/fixtures'
import {
  EXECUTION_PERMISSION_HEADER,
  ExecutionPermission,
  SIMULATOR_EXECUTION_TARGET_ID,
} from '../types/execution'
import { PLAN_PERMISSION_HEADER, PlanPermission } from '../types/plan'
import { PROPOSAL_PERMISSION_HEADER, ProposalPermission } from '../types/proposal'

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

describe('Increment 2 workflow', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    sessionStorage.clear()
  })

  it('offers cell optimization entry when txPower is present', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/cells/CELL-001/assurance') return mockJson([])
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn('/network/cells/CELL-001')
    const link = await screen.findByRole('link', { name: /Propose txPower optimization/i })
    expect(link).toHaveAttribute('href', '/network/cells/CELL-001/optimize')
    expect(link).toHaveTextContent('46')
  })

  it('generates a proposal with the exact body and permission, and blocks double submit', async () => {
    let resolveGenerate: ((value: Response) => void) | undefined
    const generateGate = new Promise<Response>((resolve) => {
      resolveGenerate = resolve
    })
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === '/api/v1/cells/CELL-001/context') return mockJson(contextFixture)
      if (url === '/api/v1/change-intelligence/proposals') return generateGate
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(recommendedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn('/network/cells/CELL-001/optimize')
    expect(await screen.findByRole('heading', { name: 'Propose txPower optimization' })).toBeInTheDocument()
    const button = screen.getByRole('button', { name: 'Generate txPower proposal' })
    await user.click(button)
    expect(screen.getByRole('button', { name: 'Generating proposal…' })).toBeDisabled()
    await user.click(screen.getByRole('button', { name: 'Generating proposal…' }))
    const generateCalls = fetchMock.mock.calls.filter(([url]) => String(url) === '/api/v1/change-intelligence/proposals')
    expect(generateCalls).toHaveLength(1)
    const init = generateCalls[0][1] as RequestInit
    const headers = new Headers(init.headers)
    expect(headers.get(PROPOSAL_PERMISSION_HEADER)).toBe(ProposalPermission.GENERATE)
    expect(JSON.parse(String(init.body))).toEqual({
      targetEntityType: 'CELL',
      targetEntityId: 'CELL-001',
      parameterName: 'txPower',
      generationInitiator: 'MANUAL',
      requestedBy: DEMO_PERSONAS[0].actorId,
    })
    resolveGenerate?.(mockJson(recommendedProposalFixture))
    expect(await screen.findByRole('heading', { name: 'Optimization proposal' })).toBeInTheDocument()
  })

  it('renders current/proposed, simulation evidence, and RECOMMENDED actions', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(recommendedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByRole('heading', { name: 'Optimization proposal' })).toBeInTheDocument()
    expect(screen.getByText('txPower = 46 dBm')).toBeInTheDocument()
    expect(screen.getByText('txPower = 42 dBm')).toBeInTheDocument()
    expect(screen.getByText(/No real network change has occurred/i)).toBeInTheDocument()
    expect(await screen.findByText('SYNTHETIC SIMULATION')).toBeInTheDocument()
    expect(screen.getAllByText(/NOT VENDOR-CALIBRATED RF/i).length).toBeGreaterThan(0)
    expect(screen.getByText('BLER_DL')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Approve proposal' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reject proposal' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Create change plan' })).not.toBeInTheDocument()
  })

  it('hides approve/reject when the proposal is not RECOMMENDED and allows plan creation when APPROVED', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(approvedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    signedIn(`/optimization/proposals/${proposalId}`)
    expect(await screen.findByText(/Approve and reject are available only when status is RECOMMENDED/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Approve proposal' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create change plan' })).toBeInTheDocument()
  })

  it('creates a plan from an approved proposal', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-intelligence/proposals/${proposalId}`) {
        return mockJson(approvedProposalFixture)
      }
      if (url === `/api/v1/simulations/${simulationId}`) return mockJson(simulationFixture)
      if (url === '/api/v1/change-planning/plans') return mockJson(readyForReviewPlanFixture)
      if (url === `/api/v1/change-planning/plans/${planId}`) return mockJson(readyForReviewPlanFixture)
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/optimization/proposals/${proposalId}`)
    await user.click(await screen.findByRole('button', { name: 'Create change plan' }))
    const createCall = fetchMock.mock.calls.find(([url]) => String(url) === '/api/v1/change-planning/plans')
    expect(createCall).toBeTruthy()
    const init = createCall?.[1] as RequestInit
    expect(new Headers(init.headers).get(PLAN_PERMISSION_HEADER)).toBe(PlanPermission.CREATE)
    expect(JSON.parse(String(init.body))).toEqual({ proposalId })
    expect(await screen.findByRole('heading', { name: 'Change plan' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Engineering review' })).toBeInTheDocument()
  })

  it('maps engineering review, authorize, readiness, and sandbox-disabled handling', async () => {
    let plan: typeof readyForReviewPlanFixture = readyForReviewPlanFixture
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-planning/plans/${planId}`) return mockJson(plan)
      if (url === `/api/v1/change-planning/plans/${planId}/review`) {
        plan = reviewedPlanFixture
        return mockJson(reviewedPlanFixture)
      }
      if (url === `/api/v1/change-planning/plans/${planId}/authorize`) {
        plan = authorizedPlanFixture
        return mockJson(authorizedPlanFixture)
      }
      if (url === `/api/v1/change-planning/plans/${planId}/readiness`) {
        plan = readyPlanFixture
        return mockJson(readyPlanFixture)
      }
      if (url === '/api/v1/change-execution/executions') {
        return mockJson({ error: 'execution disabled', failureCode: 'CHANGE_EXECUTION_DISABLED' }, 403)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/change-plans/${planId}`)
    expect(await screen.findByRole('heading', { name: 'Change plan' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Authorize plan' })).not.toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Engineering review' }))
    expect(await screen.findByRole('button', { name: 'Authorize plan' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Authorize plan' }))
    const planDialog = await screen.findByRole('dialog')
    await user.click(within(planDialog).getByRole('button', { name: 'Authorize plan' }))
    expect(await screen.findByRole('button', { name: 'Assess readiness' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Assess readiness' }))
    expect(await screen.findByText(/READY FOR SANDBOX ADMISSION/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Request sandbox execution' }))
    expect(await screen.findByText(/Sandbox execution is disabled in this runtime/)).toBeInTheDocument()
    expect(screen.getByText('failureCode CHANGE_EXECUTION_DISABLED')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Change plan' })).toBeInTheDocument()
  })

  it('fixes snip-simulator, requires execute confirmation, and shows simulator readback wording', async () => {
    let execution = reviewableExecutionFixture
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      const url = String(input)
      if (url === `/api/v1/change-execution/executions/${executionId}`) return mockJson(execution)
      if (url === `/api/v1/change-execution/executions/${executionId}/review`) {
        execution = { ...reviewableExecutionFixture, status: 'READY_FOR_EXECUTION_AUTHORIZATION' }
        return mockJson(execution)
      }
      if (url === `/api/v1/change-execution/executions/${executionId}/authorize`) {
        execution = authorizedExecutionFixture
        return mockJson(execution)
      }
      if (url === `/api/v1/change-execution/executions/${executionId}/execute`) {
        execution = verifiedExecutionFixture
        return mockJson(execution)
      }
      return mockJson({ error: 'not mocked ' + url }, 404)
    })
    const user = userEvent.setup()
    signedIn(`/sandbox/executions/${executionId}`)
    expect(await screen.findByText(/SANDBOX ONLY/)).toBeInTheDocument()
    expect(screen.getByText(/NO REAL NETWORK CHANGE/)).toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()
    expect(screen.getByText(SIMULATOR_EXECUTION_TARGET_ID)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Review sandbox execution' }))
    await user.click(await screen.findByRole('button', { name: 'Authorize sandbox execution' }))
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Authorize sandbox execution' }))
    expect(await screen.findByText(/AUTHORIZED FOR SANDBOX/)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Execute in Sandbox' }))
    const executeDialog = await screen.findByRole('dialog')
    expect(executeDialog).toHaveTextContent(SIMULATOR_EXECUTION_TARGET_ID)
    expect(executeDialog).toHaveTextContent('NO REAL NETWORK CHANGE')
    await user.click(within(executeDialog).getByRole('button', { name: 'Execute in Sandbox' }))
    expect(await screen.findByText(/Simulator readback verified/)).toBeInTheDocument()
    expect(screen.queryByText(/Production verified/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Network verified/i)).not.toBeInTheDocument()
    const executeCall = fetchMock.mock.calls.find(
      ([url]) => String(url) === `/api/v1/change-execution/executions/${executionId}/execute`,
    )
    expect(new Headers((executeCall?.[1] as RequestInit).headers).get(EXECUTION_PERMISSION_HEADER)).toBe(
      ExecutionPermission.AUTHORIZE,
    )
  })

  it('does not show execute on a reviewable execution', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      if (String(input) === `/api/v1/change-execution/executions/${executionId}`) {
        return mockJson(reviewableExecutionFixture)
      }
      return mockJson({ error: 'not mocked' }, 404)
    })
    signedIn(`/sandbox/executions/${executionId}`)
    expect(await screen.findByRole('button', { name: 'Review sandbox execution' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Execute in Sandbox' })).not.toBeInTheDocument()
  })

  it('lists proposals from the Optimization navigation page', async () => {
    fetchMock.mockImplementation(async (input: RequestInfo) => {
      if (String(input) === '/api/v1/change-intelligence/proposals') {
        return mockJson([recommendedProposalFixture.proposal])
      }
      return mockJson({ error: 'not mocked' }, 404)
    })
    signedIn('/optimization')
    expect(await screen.findByRole('heading', { name: 'Optimization' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'CELL-001' })).toHaveAttribute(
      'href',
      `/optimization/proposals/${proposalId}`,
    )
  })
})
