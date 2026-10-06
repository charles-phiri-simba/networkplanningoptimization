import { useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../../api/snipApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorState } from '../../components/ErrorState'
import { formatTimestamp } from '../../utils/format'
import type { ChangeProposalSummaryDto } from '../../types/proposal'
import type { TwinDetailDto } from '../../types/twin'

export function TwinPrerequisitePanel({ proposal }: { proposal: ChangeProposalSummaryDto }) {
  const [confirm, setConfirm] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [twin, setTwin] = useState<TwinDetailDto | null>(null)

  const stale = proposal.failureCode === 'TWIN_STATE_STALE'
  const heading = stale ? 'Digital Twin is not current' : 'Digital Twin required'
  const explanation = stale
    ? 'SNIP cannot evaluate optimization candidates because the Digital Twin for this cell is not CURRENT.'
    : 'SNIP cannot evaluate optimization candidates because a CURRENT Digital Twin is not available for this cell.'

  async function synchronize() {
    if (busy) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      const result = await snipApi.synchronizeCellTwin(proposal.targetEntityId)
      setTwin(result)
      setConfirm(false)
    } catch (caught) {
      setConfirm(false)
      setError(caught)
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel" aria-labelledby="twin-prerequisite-heading">
      <h2 id="twin-prerequisite-heading">{heading}</h2>
      <p>
        Status remains {proposal.status}. This historical result is not rewritten after Digital Twin
        synchronization.
      </p>
      <p>{explanation}</p>
      <p>
        Synchronization prepares or updates SNIP&apos;s internal Digital Twin representation. It
        supports simulation and optimization evaluation. It does not modify the real network and
        does not authorize a network change.
      </p>
      <details>
        <summary>Technical details</summary>
        <p className="muted diagnostic">
          {proposal.failureCode ?? 'unspecified'}
          {proposal.failureReason ? ` — ${proposal.failureReason}` : ''}
        </p>
      </details>
      {twin ? (
        <div className="banner-ok" role="status">
          <p>
            Digital Twin is {twin.freshness}
            {twin.freshness === 'CURRENT' ? ' and ready for a new optimization proposal.' : '.'}
          </p>
          <dl className="kv">
            <div>
              <dt>Freshness</dt>
              <dd>{twin.freshness}</dd>
            </div>
            <div>
              <dt>Version</dt>
              <dd>{twin.latestVersion}</dd>
            </div>
            <div>
              <dt>Synchronized</dt>
              <dd>{formatTimestamp(twin.synchronizedAt)}</dd>
            </div>
          </dl>
          <p>
            This INVALID proposal remains INVALID. Generate a new optimization proposal. Do not
            reuse this result as a recommendation.
          </p>
          <p>
            <Link className="btn btn-primary" to={`/network/cells/${proposal.targetEntityId}/optimize`}>
              Generate a new optimization proposal
            </Link>
          </p>
        </div>
      ) : (
        <p>
          Next legal action: synchronize the Digital Twin, then generate a new proposal. Do not
          reuse this INVALID proposal as a recommendation.
        </p>
      )}
      {error ? <ErrorState error={error} /> : null}
      {!twin ? (
        <p>
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm(true)}>
            Synchronize Digital Twin
          </button>
        </p>
      ) : null}
      {confirm ? (
        <ConfirmDialog
          title="Synchronize Digital Twin?"
          confirmLabel="Synchronize Digital Twin"
          busy={busy}
          onCancel={() => setConfirm(false)}
          onConfirm={() => void synchronize()}
        >
          <p>
            Synchronization prepares SNIP&apos;s internal Digital Twin for this cell. It supports
            simulation and optimization evaluation. It does not modify the real network and does
            not authorize a network change.
          </p>
        </ConfirmDialog>
      ) : null}
    </section>
  )
}
