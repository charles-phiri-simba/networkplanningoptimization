import { apiGet, apiPost } from './client'
import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import type {
  CancelExecutionRequest,
  CreateExecutionRequest,
  ExecutionDetailDto,
  ExecutionPermissionValue,
  ReviewExecutionRequest,
  AuthorizeExecutionRequest,
} from '../types/execution'
import { EXECUTION_PERMISSION_HEADER } from '../types/execution'
import type { CellContextDto, CellDto, GnbDto, SiteDto } from '../types/network'
import type {
  AuthorizeChangePlanRequest,
  CancelChangePlanRequest,
  ChangePlanDetailDto,
  CreateChangePlanRequest,
  PlanPermissionValue,
  ReviewChangePlanRequest,
} from '../types/plan'
import { PLAN_PERMISSION_HEADER } from '../types/plan'
import type {
  ChangeProposalDetailDto,
  ChangeProposalSummaryDto,
  GenerateChangeProposalRequest,
  ProposalPermissionValue,
  ReviewChangeProposalRequest,
} from '../types/proposal'
import { PROPOSAL_PERMISSION_HEADER } from '../types/proposal'
import type { RecommendationRequest, RecommendationResponse } from '../types/recommendation'
import type { SimulationDetailDto } from '../types/simulation'

function proposalHeaders(permission: ProposalPermissionValue): HeadersInit {
  return { [PROPOSAL_PERMISSION_HEADER]: permission }
}

function planHeaders(permission: PlanPermissionValue): HeadersInit {
  return { [PLAN_PERMISSION_HEADER]: permission }
}

function executionHeaders(permission: ExecutionPermissionValue): HeadersInit {
  return { [EXECUTION_PERMISSION_HEADER]: permission }
}

export const snipApi = {
  listSites: () => apiGet<SiteDto[]>('/api/v1/sites'),
  getSite: (siteId: string) => apiGet<SiteDto>(`/api/v1/sites/${encodeURIComponent(siteId)}`),
  listGnbs: () => apiGet<GnbDto[]>('/api/v1/gnbs'),
  listCells: () => apiGet<CellDto[]>('/api/v1/cells'),
  getCellContext: (cellId: string) =>
    apiGet<CellContextDto>(`/api/v1/cells/${encodeURIComponent(cellId)}/context`),
  listAssuranceCases: () => apiGet<AssuranceCaseDto[]>('/api/v1/assurance/cases'),
  getAssuranceForCell: (cellId: string) =>
    apiGet<AssuranceCaseDto[]>(`/api/v1/cells/${encodeURIComponent(cellId)}/assurance`),
  getAssuranceCase: (caseId: string) =>
    apiGet<AssuranceCaseDto>(`/api/v1/assurance/cases/${encodeURIComponent(caseId)}`),
  getAssuranceAssessment: (caseId: string) =>
    apiGet<DecisionAssessmentDto>(`/api/v1/assurance/cases/${encodeURIComponent(caseId)}/assessment`),
  recommend: (request: RecommendationRequest) =>
    apiPost<RecommendationResponse>('/api/v1/recommendations', request),

  listChangeProposals: (permission: ProposalPermissionValue) =>
    apiGet<ChangeProposalSummaryDto[]>('/api/v1/change-intelligence/proposals', proposalHeaders(permission)),
  generateChangeProposal: (
    request: GenerateChangeProposalRequest,
    permission: ProposalPermissionValue,
  ) =>
    apiPost<ChangeProposalDetailDto>(
      '/api/v1/change-intelligence/proposals',
      request,
      proposalHeaders(permission),
    ),
  getChangeProposal: (proposalId: string, permission: ProposalPermissionValue) =>
    apiGet<ChangeProposalDetailDto>(
      `/api/v1/change-intelligence/proposals/${encodeURIComponent(proposalId)}`,
      proposalHeaders(permission),
    ),
  getChangeProposalEvidence: (proposalId: string, permission: ProposalPermissionValue) =>
    apiGet<Record<string, unknown>>(
      `/api/v1/change-intelligence/proposals/${encodeURIComponent(proposalId)}/evidence`,
      proposalHeaders(permission),
    ),
  approveChangeProposal: (
    proposalId: string,
    request: ReviewChangeProposalRequest,
    permission: ProposalPermissionValue,
  ) =>
    apiPost<ChangeProposalDetailDto>(
      `/api/v1/change-intelligence/proposals/${encodeURIComponent(proposalId)}/approve`,
      request,
      proposalHeaders(permission),
    ),
  rejectChangeProposal: (
    proposalId: string,
    request: ReviewChangeProposalRequest,
    permission: ProposalPermissionValue,
  ) =>
    apiPost<ChangeProposalDetailDto>(
      `/api/v1/change-intelligence/proposals/${encodeURIComponent(proposalId)}/reject`,
      request,
      proposalHeaders(permission),
    ),

  getSimulation: (simulationId: string) =>
    apiGet<SimulationDetailDto>(`/api/v1/simulations/${encodeURIComponent(simulationId)}`),

  createChangePlan: (request: CreateChangePlanRequest, permission: PlanPermissionValue) =>
    apiPost<ChangePlanDetailDto>('/api/v1/change-planning/plans', request, planHeaders(permission)),
  getChangePlan: (planId: string, permission: PlanPermissionValue) =>
    apiGet<ChangePlanDetailDto>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}`,
      planHeaders(permission),
    ),
  getChangePlanEvidence: (planId: string, permission: PlanPermissionValue) =>
    apiGet<Record<string, unknown>>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}/evidence`,
      planHeaders(permission),
    ),
  reviewChangePlan: (planId: string, request: ReviewChangePlanRequest, permission: PlanPermissionValue) =>
    apiPost<ChangePlanDetailDto>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}/review`,
      request,
      planHeaders(permission),
    ),
  authorizeChangePlan: (
    planId: string,
    request: AuthorizeChangePlanRequest,
    permission: PlanPermissionValue,
  ) =>
    apiPost<ChangePlanDetailDto>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}/authorize`,
      request,
      planHeaders(permission),
    ),
  assessChangePlanReadiness: (planId: string, permission: PlanPermissionValue) =>
    apiPost<ChangePlanDetailDto>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}/readiness`,
      undefined,
      planHeaders(permission),
    ),
  cancelChangePlan: (planId: string, request: CancelChangePlanRequest, permission: PlanPermissionValue) =>
    apiPost<ChangePlanDetailDto>(
      `/api/v1/change-planning/plans/${encodeURIComponent(planId)}/cancel`,
      request,
      planHeaders(permission),
    ),

  requestSandboxExecution: (request: CreateExecutionRequest, permission: ExecutionPermissionValue) =>
    apiPost<ExecutionDetailDto>(
      '/api/v1/change-execution/executions',
      request,
      executionHeaders(permission),
    ),
  getSandboxExecution: (executionId: string, permission: ExecutionPermissionValue) =>
    apiGet<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}`,
      executionHeaders(permission),
    ),
  getSandboxExecutionEvidence: (executionId: string, permission: ExecutionPermissionValue) =>
    apiGet<Record<string, unknown>>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/evidence`,
      executionHeaders(permission),
    ),
  reviewSandboxExecution: (
    executionId: string,
    request: ReviewExecutionRequest,
    permission: ExecutionPermissionValue,
  ) =>
    apiPost<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/review`,
      request,
      executionHeaders(permission),
    ),
  authorizeSandboxExecution: (
    executionId: string,
    request: AuthorizeExecutionRequest,
    permission: ExecutionPermissionValue,
  ) =>
    apiPost<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/authorize`,
      request,
      executionHeaders(permission),
    ),
  executeSandboxExecution: (executionId: string, permission: ExecutionPermissionValue) =>
    apiPost<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/execute`,
      undefined,
      executionHeaders(permission),
    ),
  verifySandboxExecution: (executionId: string, permission: ExecutionPermissionValue) =>
    apiPost<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/verify`,
      undefined,
      executionHeaders(permission),
    ),
  cancelSandboxExecution: (
    executionId: string,
    request: CancelExecutionRequest,
    permission: ExecutionPermissionValue,
  ) =>
    apiPost<ExecutionDetailDto>(
      `/api/v1/change-execution/executions/${encodeURIComponent(executionId)}/cancel`,
      request,
      executionHeaders(permission),
    ),
}
