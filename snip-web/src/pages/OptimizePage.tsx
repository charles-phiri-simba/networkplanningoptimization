import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useAuth } from '../features/auth/AuthContext'
import { DemoPermissionBanner } from '../features/optimization/DemoPermissionBanner'
import { findTxPower } from '../features/optimization/txPower'
import type { CellContextDto } from '../types/network'
import { ProposalPermission } from '../types/proposal'

export function OptimizePage() {
  const { cellId = '' } = useParams()
  const navigate = useNavigate()
  const { identity } = useAuth()
  const [context, setContext] = useState<CellContextDto | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)

  useEffect(() => {
    setContext(null)
    setError(null)
    snipApi.getCellContext(cellId).then(setContext).catch(setError)
  }, [cellId])

  const txPower = context ? findTxPower(context.radioConfiguration) : undefined

  async function generate() {
    if (busy || !txPower) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const detail = await snipApi.generateChangeProposal(
        {
          targetEntityType: 'CELL',
          targetEntityId: cellId,
          parameterName: 'txPower',
          generationInitiator: 'MANUAL',
          requestedBy: identity?.actorId ?? null,
        },
        ProposalPermission.GENERATE,
      )
      navigate(`/optimization/proposals/${detail.proposal.id}`, { replace: true })
    } catch (caught) {
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return <ErrorState error={error} />
  }
  if (!context) {
    return <LoadingState label="Loading cell for optimization…" />
  }

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/network">Network</Link> /{' '}
        <Link to={`/network/cells/${encodeURIComponent(cellId)}`}>{cellId}</Link> / optimize
      </p>
      <header className="page-header">
        <div>
          <h1>Propose txPower optimization</h1>
          <p className="muted">
            Creates a governed Phase 13 proposal. SNIP selects the proposed dBm. This does not
            change the live network.
          </p>
        </div>
      </header>
      <DemoPermissionBanner />
      <p className="banner-demo" role="note">
        A new generate may supersede an existing RECOMMENDED proposal for this cell and parameter.
      </p>
      <dl className="kv">
        <div>
          <dt>Cell</dt>
          <dd>{cellId}</dd>
        </div>
        <div>
          <dt>Parameter</dt>
          <dd>txPower</dd>
        </div>
        <div>
          <dt>Current txPower</dt>
          <dd>
            {txPower ? `${txPower.parameterValue} ${txPower.unit ?? 'dBm'}` : 'Not present'}
          </dd>
        </div>
        <div>
          <dt>Audit requestedBy</dt>
          <dd>{identity?.actorId ?? '—'} · demo actor metadata, not authenticated identity</dd>
        </div>
      </dl>
      {actionError instanceof ApiError ? <ErrorState error={actionError} /> : null}
      <p>
        <button type="button" className="btn btn-primary" onClick={generate} disabled={busy || !txPower}>
          {busy ? 'Generating proposal…' : 'Generate txPower proposal'}
        </button>
      </p>
      {!txPower ? (
        <p className="muted">Generation is unavailable because radioConfiguration has no txPower.</p>
      ) : null}
    </div>
  )
}
