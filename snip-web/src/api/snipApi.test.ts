import { afterEach, describe, expect, it, vi } from 'vitest'
import { snipApi } from './snipApi'
import {
  EXECUTION_PERMISSION_HEADER,
  ExecutionPermission,
  SIMULATOR_EXECUTION_TARGET_ID,
} from '../types/execution'
import { PLAN_PERMISSION_HEADER, PlanPermission } from '../types/plan'
import { PROPOSAL_PERMISSION_HEADER, ProposalPermission } from '../types/proposal'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Increment 2 API client', () => {
  it('sends exact proposal, plan, and execution permission headers', async () => {
    const fetchMock = vi.fn().mockImplementation(async () =>
      new Response(JSON.stringify({}), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    await snipApi.generateChangeProposal(
      { targetEntityType: 'CELL', targetEntityId: 'CELL-001', parameterName: 'txPower' },
      ProposalPermission.GENERATE,
    )
    await snipApi.getChangeProposal('p1', ProposalPermission.VIEW)
    await snipApi.approveChangeProposal('p1', { reviewer: 'demo' }, ProposalPermission.APPROVE)
    await snipApi.rejectChangeProposal('p1', { reviewer: 'demo', reasonCode: 'NOT_SUITABLE' }, ProposalPermission.REJECT)
    await snipApi.createChangePlan({ proposalId: 'p1' }, PlanPermission.CREATE)
    await snipApi.reviewChangePlan('plan-1', { reviewer: 'demo' }, PlanPermission.REVIEW)
    await snipApi.authorizeChangePlan('plan-1', { authorizer: 'demo' }, PlanPermission.AUTHORIZE)
    await snipApi.assessChangePlanReadiness('plan-1', PlanPermission.AUTHORIZE)
    await snipApi.requestSandboxExecution(
      { planId: 'plan-1', executionTargetId: SIMULATOR_EXECUTION_TARGET_ID },
      ExecutionPermission.REQUEST,
    )
    await snipApi.executeSandboxExecution('e1', ExecutionPermission.AUTHORIZE)

    const calls = fetchMock.mock.calls as [string, RequestInit][]
    const headerOf = (path: string) => {
      const call = calls.find(([url]) => url.endsWith(path))
      return new Headers(call?.[1].headers)
    }
    expect(headerOf('/api/v1/change-intelligence/proposals').get(PROPOSAL_PERMISSION_HEADER)).toBe(
      ProposalPermission.GENERATE,
    )
    expect(headerOf('/api/v1/change-intelligence/proposals/p1').get(PROPOSAL_PERMISSION_HEADER)).toBe(
      ProposalPermission.VIEW,
    )
    expect(headerOf('/api/v1/change-intelligence/proposals/p1/approve').get(PROPOSAL_PERMISSION_HEADER)).toBe(
      ProposalPermission.APPROVE,
    )
    expect(headerOf('/api/v1/change-intelligence/proposals/p1/reject').get(PROPOSAL_PERMISSION_HEADER)).toBe(
      ProposalPermission.REJECT,
    )
    expect(headerOf('/api/v1/change-planning/plans').get(PLAN_PERMISSION_HEADER)).toBe(PlanPermission.CREATE)
    expect(headerOf('/api/v1/change-planning/plans/plan-1/review').get(PLAN_PERMISSION_HEADER)).toBe(
      PlanPermission.REVIEW,
    )
    expect(headerOf('/api/v1/change-planning/plans/plan-1/authorize').get(PLAN_PERMISSION_HEADER)).toBe(
      PlanPermission.AUTHORIZE,
    )
    expect(headerOf('/api/v1/change-planning/plans/plan-1/readiness').get(PLAN_PERMISSION_HEADER)).toBe(
      PlanPermission.AUTHORIZE,
    )
    expect(headerOf('/api/v1/change-execution/executions').get(EXECUTION_PERMISSION_HEADER)).toBe(
      ExecutionPermission.REQUEST,
    )
    expect(headerOf('/api/v1/change-execution/executions/e1/execute').get(EXECUTION_PERMISSION_HEADER)).toBe(
      ExecutionPermission.AUTHORIZE,
    )
  })

  it('does not reference production change or campaign endpoints', () => {
    const source = Object.values(snipApi).map(String).join('\n')
    expect(source).not.toContain('/api/v1/production-changes')
    expect(source).not.toContain('/api/v1/production-campaigns')
    expect(source).not.toContain('/mcp')
  })
})
