import { useEffect, useState } from 'react'
import { snipApi } from '../../api/snipApi'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { StatusBadge } from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/format'
import { VendorImportPermission } from '../../types/sync'
import type { SynchronizationSourceStateDto, SynchronizationSourceSummaryDto } from '../../types/sync'
import { firstEnabledSource } from './firstEnabledSource'

function primaryKnowledgeMessage(state: SynchronizationSourceStateDto | null): {
  title: string
  detail: string
} {
  if (!state || !state.present) {
    return {
      title: 'Network knowledge has not been established',
      detail:
        'SNIP has no materialized knowledge for the source used in optimization eligibility. This is source-scoped, not cell-specific.',
    }
  }
  const reasons = state.confidenceReasonCodes ?? ''
  const recoveryRequired = state.recoveryRequired === true
  if (recoveryRequired || reasons.includes('RECOVERY_REQUIRED')) {
    return {
      title: 'Network knowledge needs recovery',
      detail:
        'SNIP has a trusted baseline, but synchronization continuity must be restored before it can issue an optimization recommendation.',
    }
  }
  if (state.knowledgeConfidence === 'HIGH') {
    return {
      title: 'Network knowledge current',
      detail: 'Trusted network knowledge is current for optimization eligibility.',
    }
  }
  if (state.knowledgeConfidence === 'LOW' || state.knowledgeConfidence === 'UNKNOWN') {
    return {
      title: 'Network knowledge is not sufficient for recommendation',
      detail: 'SNIP can evaluate a proposal, but it will not recommend a change until knowledge is restored.',
    }
  }
  return {
    title: 'Network knowledge',
    detail: 'This is the authoritative source used for optimization eligibility. It is source-scoped, not cell-specific.',
  }
}

export function NetworkKnowledgePanel() {
  const [source, setSource] = useState<SynchronizationSourceSummaryDto | null>(null)
  const [state, setState] = useState<SynchronizationSourceStateDto | null>(null)
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [confirm, setConfirm] = useState(false)
  const [recoveryNote, setRecoveryNote] = useState<string | null>(null)

  function load() {
    setError(null)
    setLoaded(false)
    setRecoveryNote(null)
    snipApi
      .listSynchronizationSources(VendorImportPermission.VIEW)
      .then((sources) => {
        const enabled = firstEnabledSource(sources)
        setSource(enabled)
        if (!enabled) {
          setState(null)
          return null
        }
        return snipApi.getSynchronizationSourceState(
          enabled.sourceSystem,
          enabled.sourceScope,
          VendorImportPermission.VIEW,
        )
      })
      .then((next) => {
        if (next) {
          setState(next)
        }
      })
      .catch(setError)
      .finally(() => setLoaded(true))
  }

  useEffect(() => {
    load()
  }, [])

  async function recover() {
    if (!source || busy) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      const result = await snipApi.recoverNetworkKnowledge(
        source.connectorId,
        VendorImportPermission.RECOVERY,
      )
      setConfirm(false)
      setRecoveryNote(
        result.status
          ? `Recovery finished with status ${result.status}.`
          : 'Recovery request completed.',
      )
      const refreshed = await snipApi.getSynchronizationSourceState(
        source.sourceSystem,
        source.sourceScope,
        VendorImportPermission.VIEW,
      )
      setState(refreshed)
    } catch (caught) {
      setConfirm(false)
      setError(caught)
    } finally {
      setBusy(false)
    }
  }

  const copy = primaryKnowledgeMessage(state)
  const recoveryRequired = state?.present === true && state.recoveryRequired === true

  return (
    <section className="panel" aria-labelledby="knowledge-heading">
      <header className="panel-header">
        <h2 id="knowledge-heading">Network knowledge</h2>
        <p className="muted">
          Authoritative source used for optimization eligibility. This is not a per-cell measurement.
        </p>
      </header>
      {error ? <ErrorState error={error} onRetry={load} /> : null}
      {!error && !loaded ? <LoadingState label="Loading network knowledge…" /> : null}
      {!error && loaded && source === null ? (
        <p>No enabled synchronization source is configured.</p>
      ) : null}
      {state ? (
        <>
          <p>
            <strong>{copy.title}</strong>
          </p>
          <p>{copy.detail}</p>
          {state.present ? (
            <dl className="kv">
              <div>
                <dt>Confidence</dt>
                <dd>
                  <StatusBadge status={state.knowledgeConfidence ?? 'UNKNOWN'} />
                </dd>
              </div>
              <div>
                <dt>Freshness</dt>
                <dd>{state.freshness ?? '—'}</dd>
              </div>
              <div>
                <dt>Source health</dt>
                <dd>{state.sourceHealth ?? '—'}</dd>
              </div>
              <div>
                <dt>Recovery required</dt>
                <dd>{state.recoveryRequired ? 'Yes' : 'No'}</dd>
              </div>
              <div>
                <dt>Last trusted synchronization</dt>
                <dd>{formatTimestamp(state.lastTrustedSynchronizationAt)}</dd>
              </div>
            </dl>
          ) : null}
          <details>
            <summary>Technical details</summary>
            <dl className="kv">
              <div>
                <dt>Source system</dt>
                <dd>{state.sourceSystem}</dd>
              </div>
              <div>
                <dt>Scope</dt>
                <dd>{state.sourceScope}</dd>
              </div>
              <div>
                <dt>Connector</dt>
                <dd className="diagnostic">{state.connectorId}</dd>
              </div>
              <div>
                <dt>Confidence reasons</dt>
                <dd>{state.confidenceReasonCodes ?? '—'}</dd>
              </div>
              <div>
                <dt>Checkpoint</dt>
                <dd>{state.checkpointStatus ?? '—'}</dd>
              </div>
            </dl>
          </details>
        </>
      ) : null}
      {recoveryNote ? <p className="banner-ok" role="status">{recoveryNote}</p> : null}
      {recoveryRequired ? (
        <p>
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm(true)}>
            Recover network knowledge
          </button>
        </p>
      ) : null}
      {confirm ? (
        <ConfirmDialog
          title="Recover network knowledge?"
          confirmLabel="Recover network knowledge"
          busy={busy}
          onCancel={() => setConfirm(false)}
          onConfirm={() => void recover()}
        >
          <p>
            Recovery performs a read-only resynchronization of SNIP&apos;s network knowledge. It does
            not modify the network and does not authorize a network change.
          </p>
        </ConfirmDialog>
      ) : null}
    </section>
  )
}
