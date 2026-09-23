import type { ChangeProposalSummaryDto } from '../../types/proposal'

export function CurrentProposedPanel({ proposal }: { proposal: ChangeProposalSummaryDto }) {
  const unit = proposal.unit ?? 'dBm'
  return (
    <section className="panel" aria-labelledby="compare-heading">
      <header className="panel-header">
        <h2 id="compare-heading">Current versus proposed</h2>
        <p className="muted">
          Deterministic optimization selected the proposed txPower. This is not an AI command.
          No real network change has occurred.
        </p>
      </header>
      <div className="compare-grid">
        <div className="compare-card">
          <p className="eyebrow">Current</p>
          <p className="compare-value">
            txPower = {proposal.currentValue ?? '—'} {unit}
          </p>
        </div>
        <div className="compare-card compare-proposed">
          <p className="eyebrow">Proposed</p>
          <p className="compare-value">
            txPower = {proposal.proposedValue ?? '—'} {unit}
          </p>
        </div>
      </div>
    </section>
  )
}
