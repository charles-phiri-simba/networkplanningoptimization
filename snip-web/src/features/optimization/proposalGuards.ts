import { ProposalStatus } from '../../types/proposal'
import type { ChangeProposalSummaryDto } from '../../types/proposal'

export function canApproveOrReject(proposal: ChangeProposalSummaryDto): boolean {
  return proposal.status === ProposalStatus.RECOMMENDED
}

export function canCreatePlan(proposal: ChangeProposalSummaryDto): boolean {
  return proposal.status === ProposalStatus.APPROVED
}

export function selectedCandidateRank(rankOrder: number | null): boolean {
  return rankOrder === 1
}
