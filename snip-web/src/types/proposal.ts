export const PROPOSAL_PERMISSION_HEADER = 'X-SNIP-CHANGE-PROPOSAL-PERMISSION'

export const ProposalPermission = {
  VIEW: 'VIEW_NETWORK_CHANGE_PROPOSALS',
  GENERATE: 'GENERATE_NETWORK_CHANGE_PROPOSAL',
  REVIEW: 'REVIEW_NETWORK_CHANGE_PROPOSAL',
  APPROVE: 'APPROVE_NETWORK_CHANGE_PROPOSAL',
  REJECT: 'REJECT_NETWORK_CHANGE_PROPOSAL',
} as const

export type ProposalPermissionValue = (typeof ProposalPermission)[keyof typeof ProposalPermission]

export const ProposalStatus = {
  DRAFT: 'DRAFT',
  VALIDATING: 'VALIDATING',
  INVALID: 'INVALID',
  SIMULATING: 'SIMULATING',
  SIMULATION_FAILED: 'SIMULATION_FAILED',
  EVALUATED: 'EVALUATED',
  RECOMMENDED: 'RECOMMENDED',
  REJECTED: 'REJECTED',
  APPROVED: 'APPROVED',
  EXPIRED: 'EXPIRED',
  SUPERSEDED: 'SUPERSEDED',
  INVALIDATED: 'INVALIDATED',
} as const

export type ProposalStatusValue = (typeof ProposalStatus)[keyof typeof ProposalStatus]

export interface GenerateChangeProposalRequest {
  targetEntityType: 'CELL'
  targetEntityId: string
  parameterName: 'txPower'
  assuranceCaseId?: string | null
  decisionReference?: string | null
  generationInitiator?: 'MANUAL' | null
  requestedBy?: string | null
}

export interface ReviewChangeProposalRequest {
  reviewer?: string | null
  reasonCode?: string | null
  comment?: string | null
}

export interface ChangeProposalSummaryDto {
  id: string
  proposalType: string
  status: string
  targetEntityType: string
  targetEntityId: string
  parameterName: string
  currentValue: string | null
  proposedValue: string | null
  unit: string | null
  networkKnowledgeConfidence: string | null
  assuranceConfidence: string | null
  simulationConfidence: string | null
  riskLevel: string | null
  benefitSummary: string | null
  proposalScore: number | string | null
  failureCode: string | null
  failureReason: string | null
  createdAt: string | null
  evaluatedAt: string | null
  expiresAt: string | null
  invalidationReason: string | null
  version: number
}

export interface CandidateEvidenceDto {
  candidateValue: string | null
  baselineCandidate: boolean
  validationOutcome: string | null
  validationReason: string | null
  simulationRunId: string | null
  simulationConfidence: string | null
  benefitScore: number | string | null
  riskLevel: string | null
  proposalScore: number | string | null
  rankOrder: number | null
}

export interface ChangeProposalDetailDto {
  proposal: ChangeProposalSummaryDto
  candidates: CandidateEvidenceDto[]
}
