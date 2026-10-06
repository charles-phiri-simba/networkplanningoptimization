import { ApiError } from '../../api/client'

export interface OperatorMessage {
  heading: string
  message: string
}

const MESSAGES: Record<string, OperatorMessage> = {
  NETWORK_KNOWLEDGE_LOW: {
    heading: 'Recommendation withheld',
    message:
      'Network knowledge is not sufficient for a recommendation. Recover network knowledge if recovery is required, then generate a new proposal.',
  },
  NETWORK_KNOWLEDGE_UNKNOWN: {
    heading: 'Recommendation withheld',
    message:
      'Network knowledge is unknown. Establish or recover trusted network knowledge, then generate a new proposal.',
  },
  RECOVERY_REQUIRED: {
    heading: 'Network knowledge needs recovery',
    message:
      'Synchronization continuity must be restored before SNIP can issue an optimization recommendation.',
  },
  TWIN_STATE_UNAVAILABLE: {
    heading: 'Digital Twin required',
    message:
      'SNIP cannot evaluate optimization candidates because a CURRENT Digital Twin is not available for this cell. Synchronize the Digital Twin, then generate a new proposal.',
  },
  TWIN_STATE_STALE: {
    heading: 'Digital Twin is not current',
    message:
      'SNIP cannot evaluate optimization candidates because the Digital Twin for this cell is not CURRENT. Synchronize the Digital Twin, then generate a new proposal.',
  },
  ACTIVE_PLAN_EXISTS: {
    heading: 'Active change plan already exists',
    message: 'An active change plan already exists for this proposal. Open the existing plan instead of creating another.',
  },
  CHANGE_EXECUTION_DISABLED: {
    heading: 'Sandbox execution is disabled',
    message:
      'Sandbox execution is not enabled in this runtime. The change plan remains visible. No real network change occurred.',
  },
  PLAN_PROPOSAL_INVALID: {
    heading: 'Optimization proposal is no longer valid',
    message: 'The proposal is stale or invalid. Review the proposal status before continuing.',
  },
  PLAN_PROPOSAL_NOT_APPROVED: {
    heading: 'Proposal is not approved',
    message: 'A change plan can be created only from an approved optimization proposal.',
  },
  PLAN_AUTHORIZATION_STALE: {
    heading: 'Authorization is no longer current',
    message: 'Plan authorization no longer matches the current plan. Re-assess readiness after reviewing the plan.',
  },
  PLAN_AUTHORIZATION_MISSING: {
    heading: 'Plan authorization is required',
    message: 'Authorize the change plan before requesting sandbox execution.',
  },
  PLAN_EXPIRED: {
    heading: 'Change plan has expired',
    message: 'This plan is no longer valid. Create a new plan from a current approved proposal.',
  },
  PROPOSAL_EXPIRED: {
    heading: 'Proposal has expired',
    message: 'This proposal is no longer current. Generate a new optimization proposal.',
  },
  PROPOSAL_INVALIDATED: {
    heading: 'Proposal is no longer valid',
    message: 'The proposal was invalidated. Generate a new optimization proposal if optimization is still required.',
  },
  PROPOSAL_SUPERSEDED: {
    heading: 'Proposal was superseded',
    message: 'A newer proposal replaced this one. Continue from the current proposal.',
  },
  SIMULATION_FAILED: {
    heading: 'Synthetic simulation failed',
    message: 'The synthetic simulation did not complete. This is not a live RF result.',
  },
  INVALID_PLAN_STATE: {
    heading: 'Plan is not in a usable state',
    message: 'The requested action is not available for the current plan status.',
  },
  CONCURRENT_REVIEW_CONFLICT: {
    heading: 'The record changed while you were working',
    message: 'Refresh and review the current state before repeating the action.',
  },
  CONCURRENT_PLAN_CONFLICT: {
    heading: 'The plan changed while you were working',
    message: 'Refresh the plan and continue from its current state.',
  },
}

export function translateOperatorError(error: unknown): OperatorMessage | null {
  if (!(error instanceof ApiError) || !error.failureCode) {
    if (error instanceof ApiError && error.status === 0) {
      return {
        heading: 'Backend unavailable',
        message: 'SNIP backend is unavailable. Retry when the service is reachable.',
      }
    }
    return null
  }
  return MESSAGES[error.failureCode] ?? null
}

const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

export function existingPlanIdFromError(error: unknown): string | null {
  if (!(error instanceof ApiError) || error.failureCode !== 'ACTIVE_PLAN_EXISTS') {
    return null
  }
  const candidate = error.message.trim()
  return UUID_PATTERN.test(candidate) ? candidate : null
}

export const READINESS_LABELS: Record<string, string> = {
  AUTHORIZATION_CURRENT: 'Authorization is current',
  NETWORK_KNOWLEDGE_CONFIDENCE: 'Network knowledge confidence',
  SOURCE_SYNCHRONIZATION_FRESHNESS: 'Source synchronization freshness',
  TWIN_COMPATIBILITY: 'Digital twin compatibility',
  EXPECTED_PARAMETER_VALUE: 'Current parameter matches expected value',
  PROPOSAL_STILL_VALID: 'Optimization proposal remains valid',
  ROLLBACK_AVAILABLE: 'Recovery value available',
  NO_RELEVANT_DRIFT: 'No conflicting configuration drift',
  TARGET_EXISTS: 'Target cell exists',
  DEPENDENCY_GRAPH_VALID: 'Change dependencies valid',
  FINGERPRINT_CURRENT: 'Authorized plan has not changed',
}

export function isTwinPrerequisiteFailure(failureCode: string | null | undefined): boolean {
  return failureCode === 'TWIN_STATE_UNAVAILABLE' || failureCode === 'TWIN_STATE_STALE'
}

export function readinessLabel(preconditionType: string): string {
  return READINESS_LABELS[preconditionType] ?? preconditionType.replaceAll('_', ' ')
}

export const METRIC_LABELS: Record<string, string> = {
  txPower: 'Transmit power',
  BLER_DL: 'Downlink BLER',
  PRB_UTILIZATION_DL: 'Downlink PRB utilization',
  THROUGHPUT_DL: 'Downlink throughput',
}

export function metricLabel(metric: string): string {
  return METRIC_LABELS[metric] ?? metric
}
