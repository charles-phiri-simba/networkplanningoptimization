import type { CandidateEvidenceDto } from '../../types/proposal'
import { selectedCandidateRank } from './proposalGuards'

export function CandidateTable({ candidates }: { candidates: CandidateEvidenceDto[] }) {
  if (candidates.length === 0) {
    return <p className="muted">No candidates were returned for this proposal.</p>
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th scope="col">Rank</th>
          <th scope="col">Candidate (dBm)</th>
          <th scope="col">Baseline</th>
          <th scope="col">Validation</th>
          <th scope="col">Simulation ID</th>
          <th scope="col">Simulation confidence</th>
          <th scope="col">Benefit score</th>
          <th scope="col">Risk</th>
          <th scope="col">Proposal score</th>
        </tr>
      </thead>
      <tbody>
        {candidates.map((candidate, index) => {
          const selected = selectedCandidateRank(candidate.rankOrder)
          return (
            <tr key={`${candidate.candidateValue}-${index}`} className={selected ? 'row-selected' : undefined}>
              <td>{candidate.rankOrder ?? '—'}</td>
              <td>{candidate.candidateValue ?? '—'}</td>
              <td>{candidate.baselineCandidate ? 'Yes' : 'No'}</td>
              <td>
                {candidate.validationOutcome ?? '—'}
                {candidate.validationReason ? ` · ${candidate.validationReason}` : ''}
              </td>
              <td className="diagnostic">{candidate.simulationRunId ?? '—'}</td>
              <td>{candidate.simulationConfidence ?? '—'}</td>
              <td>{candidate.benefitScore ?? '—'}</td>
              <td>{candidate.riskLevel ?? '—'}</td>
              <td>{candidate.proposalScore ?? '—'}</td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
