import { ExecutionStatus } from '../../types/execution'
import type { ExecutionDetailDto } from '../../types/execution'

export function canReviewExecution(execution: ExecutionDetailDto): boolean {
  return execution.status === ExecutionStatus.READY_FOR_REVIEW
}

export function canAuthorizeExecution(execution: ExecutionDetailDto): boolean {
  return execution.status === ExecutionStatus.READY_FOR_EXECUTION_AUTHORIZATION
}

export function canExecuteInSandbox(execution: ExecutionDetailDto): boolean {
  return execution.status === ExecutionStatus.AUTHORIZED
}

export function canVerifyExecution(execution: ExecutionDetailDto): boolean {
  return (
    execution.status === ExecutionStatus.APPLIED ||
    execution.status === ExecutionStatus.EXECUTION_OUTCOME_UNKNOWN ||
    execution.status === ExecutionStatus.VERIFYING ||
    execution.status === ExecutionStatus.ROLLBACK_APPLIED ||
    execution.status === ExecutionStatus.ROLLBACK_OUTCOME_UNKNOWN
  )
}

export function canCancelExecution(execution: ExecutionDetailDto): boolean {
  return (
    execution.status === ExecutionStatus.REQUESTED ||
    execution.status === ExecutionStatus.PRELIMINARY_ADMISSION_CHECKING ||
    execution.status === ExecutionStatus.READY_FOR_REVIEW ||
    execution.status === ExecutionStatus.REVIEWED ||
    execution.status === ExecutionStatus.READY_FOR_EXECUTION_AUTHORIZATION ||
    execution.status === ExecutionStatus.AUTHORIZED ||
    execution.status === ExecutionStatus.FINAL_PREFLIGHT_CHECKING
  )
}

export function isSimulatorReadbackVerified(execution: ExecutionDetailDto): boolean {
  return (
    execution.status === ExecutionStatus.VERIFIED ||
    execution.verificationStatus === ExecutionStatus.VERIFIED
  )
}
