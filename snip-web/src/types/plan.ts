export const PLAN_PERMISSION_HEADER = 'X-SNIP-CHANGE-PLAN-PERMISSION'

export const PlanPermission = {
  VIEW: 'VIEW_NETWORK_CHANGE_PLAN',
  CREATE: 'CREATE_NETWORK_CHANGE_PLAN',
  REVIEW: 'REVIEW_NETWORK_CHANGE_PLAN',
  AUTHORIZE: 'AUTHORIZE_NETWORK_CHANGE_PLAN',
  CANCEL: 'CANCEL_NETWORK_CHANGE_PLAN',
} as const

export type PlanPermissionValue = (typeof PlanPermission)[keyof typeof PlanPermission]

export const PlanStatus = {
  DRAFT: 'DRAFT',
  VALIDATING: 'VALIDATING',
  PLANNED: 'PLANNED',
  SAFETY_EVALUATING: 'SAFETY_EVALUATING',
  READY_FOR_REVIEW: 'READY_FOR_REVIEW',
  AUTHORIZED: 'AUTHORIZED',
  READY_FOR_EXECUTION: 'READY_FOR_EXECUTION',
  INVALID: 'INVALID',
  BLOCKED: 'BLOCKED',
  INVALIDATED: 'INVALIDATED',
  EXPIRED: 'EXPIRED',
  SUPERSEDED: 'SUPERSEDED',
  CANCELLED: 'CANCELLED',
} as const

export type PlanStatusValue = (typeof PlanStatus)[keyof typeof PlanStatus]

export interface CreateChangePlanRequest {
  proposalId: string
}

export interface ReviewChangePlanRequest {
  reviewer?: string | null
  comment?: string | null
}

export interface AuthorizeChangePlanRequest {
  authorizer?: string | null
}

export interface CancelChangePlanRequest {
  actor?: string | null
  reason?: string | null
}

export interface ChangePlanSummaryDto {
  id: string
  proposalId: string
  status: string
  targetEntityType: string
  targetEntityId: string
  parameterName: string
  expectedCurrentValue: string | null
  desiredValue: string | null
  impactLevel: string | null
  createdAt: string | null
  expiresAt: string | null
}

export interface ChangePlanOperationDto {
  sequenceNumber: number
  operationType: string
  targetEntityType: string
  targetEntityId: string
  parameterName: string
  expectedCurrentValue: string | null
  desiredValue: string | null
}

export interface ChangePlanRollbackDto {
  sequenceNumber: number
  operationType: string
  targetEntityType: string
  targetEntityId: string
  parameterName: string
  expectedCurrentValue: string | null
  desiredValue: string | null
}

export interface ChangePlanPreconditionDto {
  preconditionType: string
  expectedCondition: string | null
  observedValue: string | null
  result: string | null
  reasonCode: string | null
  checkedAt: string | null
}

export interface ChangePlanReadinessDto {
  assessedAt: string | null
  result: string | null
  assessedFingerprint: string | null
  reasonCodes: string | null
}

export interface ChangePlanDetailDto {
  plan: ChangePlanSummaryDto
  fingerprint: string | null
  authorizedFingerprint: string | null
  knowledgeConfidenceAtCreation: string | null
  riskLevel: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  authorizedBy: string | null
  authorizedAt: string | null
  cancelledBy: string | null
  cancelledAt: string | null
  invalidationReason: string | null
  invalidatedAt: string | null
  operations: ChangePlanOperationDto[]
  rollbackOperations: ChangePlanRollbackDto[]
  preconditions: ChangePlanPreconditionDto[]
  readinessAssessments: ChangePlanReadinessDto[]
}
