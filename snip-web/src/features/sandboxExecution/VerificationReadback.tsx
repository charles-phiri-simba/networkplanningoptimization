import type { ExecutionEvidenceDto, ExecutionVerificationEvidenceDto } from '../../types/execution'

export function forwardVerification(
  evidence: ExecutionEvidenceDto | null,
): ExecutionVerificationEvidenceDto | null {
  const items = evidence?.verifications ?? []
  return items.find((item) => item.direction === 'FORWARD') ?? items[0] ?? null
}

export function VerificationReadback({ evidence }: { evidence: ExecutionEvidenceDto | null }) {
  const forward = forwardVerification(evidence)
  if (!forward) {
    return (
      <section className="panel" aria-labelledby="verify-heading">
        <h2 id="verify-heading">Simulator readback</h2>
        <p className="muted">No FORWARD verification evidence is available yet.</p>
      </section>
    )
  }
  return (
    <section className="panel" aria-labelledby="verify-heading">
      <h2 id="verify-heading">Simulator readback</h2>
      <p className="banner-sandbox" role="note">
        This verifies simulator readback only. It is not vendor, network, or production verification.
      </p>
      <dl className="kv">
        <div>
          <dt>Expected simulator value</dt>
          <dd>
            {forward.expectedValue ?? '—'} {forward.expectedValue ? 'dBm' : ''}
          </dd>
        </div>
        <div>
          <dt>Observed simulator value</dt>
          <dd>
            {forward.observedValue ?? '—'} {forward.observedValue ? 'dBm' : ''}
          </dd>
        </div>
        <div>
          <dt>Result</dt>
          <dd>{forward.outcome ?? '—'}</dd>
        </div>
        <div>
          <dt>Direction</dt>
          <dd>{forward.direction ?? '—'}</dd>
        </div>
      </dl>
    </section>
  )
}
