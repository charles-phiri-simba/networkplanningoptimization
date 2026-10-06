import { ProposalStatus } from '../../types/proposal'
import type { ChangeProposalSummaryDto } from '../../types/proposal'
import { isTwinPrerequisiteFailure } from '../operator/operatorMessages'
import { TwinPrerequisitePanel } from './TwinPrerequisitePanel'

export function EligibilityBanner({ proposal }: { proposal: ChangeProposalSummaryDto }) {
  if (proposal.status === ProposalStatus.INVALID && isTwinPrerequisiteFailure(proposal.failureCode)) {
    return <TwinPrerequisitePanel proposal={proposal} />
  }
  if (proposal.status !== ProposalStatus.EVALUATED) {
    return null
  }
  const knowledgeRelated =
    proposal.failureCode === 'NETWORK_KNOWLEDGE_LOW' ||
    proposal.failureCode === 'NETWORK_KNOWLEDGE_UNKNOWN'
  return (
    <section className="panel" aria-labelledby="eligibility-heading">
      <h2 id="eligibility-heading">Recommendation withheld</h2>
      <p>
        SNIP evaluated this proposal but did not recommend a change. This historical result is not
        rewritten if network knowledge later recovers. Generate a new proposal after the legal next
        action.
      </p>
      <p>
        Reason: {proposal.failureCode ?? 'unspecified'}
        {proposal.failureReason ? ` — ${proposal.failureReason}` : ''}
      </p>
      {knowledgeRelated ? (
        <p>
          Next legal action: recover network knowledge if recovery is required, then generate a new
          proposal. Do not reuse this EVALUATED proposal as a recommendation.
        </p>
      ) : (
        <p>Next legal action: review the backend reason. Do not treat this as a recommendation.</p>
      )}
    </section>
  )
}
