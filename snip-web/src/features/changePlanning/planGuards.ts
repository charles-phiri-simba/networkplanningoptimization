import { PlanStatus } from '../../types/plan'
import type { ChangePlanDetailDto } from '../../types/plan'

export function canReviewPlan(detail: ChangePlanDetailDto): boolean {
  return detail.plan.status === PlanStatus.READY_FOR_REVIEW
}

export function canAuthorizePlan(detail: ChangePlanDetailDto): boolean {
  return detail.plan.status === PlanStatus.READY_FOR_REVIEW && Boolean(detail.reviewedAt)
}

export function canAssessReadiness(detail: ChangePlanDetailDto): boolean {
  return (
    detail.plan.status === PlanStatus.AUTHORIZED ||
    detail.plan.status === PlanStatus.READY_FOR_EXECUTION
  )
}

export function canCancelPlan(detail: ChangePlanDetailDto): boolean {
  return (
    detail.plan.status === PlanStatus.DRAFT ||
    detail.plan.status === PlanStatus.VALIDATING ||
    detail.plan.status === PlanStatus.PLANNED ||
    detail.plan.status === PlanStatus.SAFETY_EVALUATING ||
    detail.plan.status === PlanStatus.READY_FOR_REVIEW ||
    detail.plan.status === PlanStatus.AUTHORIZED ||
    detail.plan.status === PlanStatus.READY_FOR_EXECUTION
  )
}

export function canRequestSandbox(detail: ChangePlanDetailDto): boolean {
  return detail.plan.status === PlanStatus.READY_FOR_EXECUTION
}
