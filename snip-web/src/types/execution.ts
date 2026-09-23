export const EXECUTION_PERMISSION_HEADER = 'X-SNIP-CHANGE-EXECUTION-PERMISSION'

export const ExecutionPermission = {
  VIEW: 'VIEW_NETWORK_CHANGE_EXECUTION',
  REQUEST: 'REQUEST_NETWORK_CHANGE_EXECUTION',
  REVIEW: 'REVIEW_NETWORK_CHANGE_EXECUTION',
  AUTHORIZE: 'AUTHORIZE_NETWORK_CHANGE_EXECUTION',
  CANCEL: 'CANCEL_NETWORK_CHANGE_EXECUTION',
  VIEW_EVIDENCE: 'VIEW_NETWORK_CHANGE_EXECUTION_EVIDENCE',
} as const

export type ExecutionPermissionValue = (typeof ExecutionPermission)[keyof typeof ExecutionPermission]

export const SIMULATOR_EXECUTION_TARGET_ID = 'snip-simulator'

export const ExecutionStatus = {
  REQUESTED: 'REQUESTED',
  PRELIMINARY_ADMISSION_CHECKING: 'PRELIMINARY_ADMISSION_CHECKING',
  PRELIMINARY_ADMISSION_REJECTED: 'PRELIMINARY_ADMISSION_REJECTED',
  READY_FOR_REVIEW: 'READY_FOR_REVIEW',
  REVIEWED: 'REVIEWED',
  READY_FOR_EXECUTION_AUTHORIZATION: 'READY_FOR_EXECUTION_AUTHORIZATION',
  AUTHORIZED: 'AUTHORIZED',
  FINAL_PREFLIGHT_CHECKING: 'FINAL_PREFLIGHT_CHECKING',
  EXECUTING: 'EXECUTING',
  APPLIED: 'APPLIED',
  EXECUTION_OUTCOME_UNKNOWN: 'EXECUTION_OUTCOME_UNKNOWN',
  VERIFYING: 'VERIFYING',
  VERIFIED: 'VERIFIED',
  EXECUTION_FAILED: 'EXECUTION_FAILED',
  VERIFICATION_FAILED: 'VERIFICATION_FAILED',
  RECOVERY_REQUIRED: 'RECOVERY_REQUIRED',
  ROLLBACK_REQUESTED: 'ROLLBACK_REQUESTED',
  ROLLBACK_REVIEWED: 'ROLLBACK_REVIEWED',
  ROLLBACK_AUTHORIZED: 'ROLLBACK_AUTHORIZED',
  ROLLING_BACK: 'ROLLING_BACK',
  ROLLBACK_APPLIED: 'ROLLBACK_APPLIED',
  ROLLBACK_OUTCOME_UNKNOWN: 'ROLLBACK_OUTCOME_UNKNOWN',
  ROLLED_BACK: 'ROLLED_BACK',
  ROLLBACK_FAILED: 'ROLLBACK_FAILED',
  MANUAL_INTERVENTION_REQUIRED: 'MANUAL_INTERVENTION_REQUIRED',
  CANCELLED_BEFORE_MUTATION: 'CANCELLED_BEFORE_MUTATION',
} as const

export type ExecutionStatusValue = (typeof ExecutionStatus)[keyof typeof ExecutionStatus]

export interface CreateExecutionRequest {
  planId: string
  executionTargetId: typeof SIMULATOR_EXECUTION_TARGET_ID
}

export interface ReviewExecutionRequest {
  reviewer?: string | null
  comment?: string | null
}

export interface AuthorizeExecutionRequest {
  authorizer?: string | null
}

export interface CancelExecutionRequest {
  actor?: string | null
  reason?: string | null
}

export interface ExecutionOperationDto {
  sequenceNumber: number
  operationType: string
  targetEntityType: string
  targetEntityId: string
  parameterName: string
  expectedCurrentValue: string | null
  desiredValue: string | null
}

export interface ExecutionDetailDto {
  executionId: string
  planId: string
  planVersion: number
  planFingerprint: string | null
  executionTargetId: string
  executionTargetType: string | null
  executionTargetEnvironment: string | null
  adapterProfileId: string | null
  capabilityProfileVersion: string | null
  cellId: string
  parameterName: string
  executionFingerprint: string | null
  authorizedExecutionFingerprint: string | null
  status: string
  requestedBy: string | null
  requestedAt: string | null
  reviewedBy: string | null
  reviewedAt: string | null
  authorizedBy: string | null
  authorizedAt: string | null
  executionWindowOpensAt: string | null
  executionWindowClosesAt: string | null
  startedAt: string | null
  completedAt: string | null
  failureCode: string | null
  failureDetailSafe: string | null
  verificationStatus: string | null
  recoveryStatus: string | null
  rollbackStatus: string | null
  operations: ExecutionOperationDto[]
}
