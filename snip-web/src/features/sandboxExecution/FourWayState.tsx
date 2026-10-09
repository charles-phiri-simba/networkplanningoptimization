import { forwardVerification } from './VerificationReadback'
import type { ExecutionEvidenceDto } from '../../types/execution'

export function FourWayState({
  recommendedValue,
  canonicalValue,
  unit,
  evidence,
}: {
  recommendedValue: string | null | undefined
  canonicalValue: string | null | undefined
  unit: string
  evidence: ExecutionEvidenceDto | null
}) {
  const forward = forwardVerification(evidence)
  const sandboxValue = forward?.observedValue ?? null
  const verified = Boolean(forward?.outcome && /VERIFIED|MATCH|SUCCESS/i.test(forward.outcome))
  return (
    <section className="panel" aria-labelledby="four-way-heading">
      <h2 id="four-way-heading">Recommended, sandbox, canonical, and real network</h2>
      <p className="muted">
        Sandbox values come from simulator readback. Canonical values come from SNIP configuration.
        Real-network state is not inferred from the simulator.
      </p>
      <div className="compare-grid compare-grid-4">
        <div className="compare-card compare-proposed">
          <p className="eyebrow">Recommended</p>
          <p className="compare-value">
            {recommendedValue ?? '—'} {unit}
          </p>
          <p className="muted">Value selected by SNIP&apos;s governed optimization proposal.</p>
        </div>
        <div className="compare-card compare-sandbox">
          <p className="eyebrow">Sandbox</p>
          <p className="compare-value">
            {sandboxValue ?? '—'} {unit}
            {verified ? ' — verified' : ''}
          </p>
          <p className="muted">Observed in snip-simulator.</p>
        </div>
        <div className="compare-card compare-canonical">
          <p className="eyebrow">Canonical</p>
          <p className="compare-value">
            {canonicalValue ?? '—'} {unit}
          </p>
          <p className="muted">Current SNIP canonical network configuration.</p>
        </div>
        <div className="compare-card compare-real">
          <p className="eyebrow">Real network</p>
          <p className="compare-value">Unchanged</p>
          <p>No real-network execution was performed by this workflow.</p>
        </div>
      </div>
    </section>
  )
}
