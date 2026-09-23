import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { snipApi } from '../api/snipApi'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../features/auth/AuthContext'
import { DemoPermissionBanner } from '../features/optimization/DemoPermissionBanner'
import {
  canAuthorizeExecution,
  canCancelExecution,
  canExecuteInSandbox,
  canReviewExecution,
  canVerifyExecution,
  isSimulatorReadbackVerified,
} from '../features/sandboxExecution/executionGuards'
import type { ExecutionDetailDto } from '../types/execution'
import { ExecutionPermission, ExecutionStatus, SIMULATOR_EXECUTION_TARGET_ID } from '../types/execution'
import { formatTimestamp } from '../utils/format'

type ConfirmKind = 'authorize' | 'execute' | null

export function SandboxExecutionPage() {
  const { executionId = '' } = useParams()
  const { identity } = useAuth()
  const [execution, setExecution] = useState<ExecutionDetailDto | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const [confirm, setConfirm] = useState<ConfirmKind>(null)

  function load() {
    setError(null)
    snipApi.getSandboxExecution(executionId, ExecutionPermission.VIEW).then(setExecution).catch(setError)
  }

  useEffect(() => {
    setExecution(null)
    load()
  }, [executionId])

  async function review() {
    if (!execution || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.reviewSandboxExecution(
        executionId,
        { reviewer: identity?.actorId ?? null, comment: 'Sandbox execution review' },
        ExecutionPermission.REVIEW,
      )
      setExecution(next)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        load()
      }
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  async function authorize() {
    if (!execution || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.authorizeSandboxExecution(
        executionId,
        { authorizer: identity?.actorId ?? null },
        ExecutionPermission.AUTHORIZE,
      )
      setExecution(next)
      setConfirm(null)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        load()
      }
      setActionError(caught)
      setConfirm(null)
    } finally {
      setBusy(false)
    }
  }

  async function execute() {
    if (!execution || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.executeSandboxExecution(executionId, ExecutionPermission.AUTHORIZE)
      setExecution(next)
      setConfirm(null)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        load()
      }
      setActionError(caught)
      setConfirm(null)
    } finally {
      setBusy(false)
    }
  }

  async function verify() {
    if (!execution || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.verifySandboxExecution(executionId, ExecutionPermission.VIEW)
      setExecution(next)
    } catch (caught) {
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  async function cancel() {
    if (!execution || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.cancelSandboxExecution(
        executionId,
        { actor: identity?.actorId ?? null, reason: 'Cancelled before mutation' },
        ExecutionPermission.CANCEL,
      )
      setExecution(next)
    } catch (caught) {
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!execution) {
    return <LoadingState label="Loading sandbox execution…" />
  }

  const operation = execution.operations[0]

  return (
    <div className="page">
      <p className="crumb">
        <Link to={`/change-plans/${execution.planId}`}>Change plan</Link> / sandbox
      </p>
      <header className="page-header">
        <div>
          <h1>Sandbox execution</h1>
          <p className="muted">Phase 15 simulator execution only. Target is fixed to {SIMULATOR_EXECUTION_TARGET_ID}.</p>
        </div>
        <StatusBadge status={execution.status} />
      </header>
      <p className="banner-sandbox" role="alert">
        <strong>SANDBOX ONLY · SIMULATOR · NO REAL NETWORK CHANGE</strong>
      </p>
      <DemoPermissionBanner />
      {isSimulatorReadbackVerified(execution) ? (
        <p className="banner-ok" role="status">
          Simulator readback verified. Sandbox parameter confirmed against the planned desired
          value. This is not network verification and not production verification.
        </p>
      ) : null}
      <section className="panel" aria-labelledby="exec-meta">
        <h2 id="exec-meta">Execution details</h2>
        <dl className="kv">
          <div>
            <dt>Execution ID</dt>
            <dd className="diagnostic">{execution.executionId}</dd>
          </div>
          <div>
            <dt>Plan ID</dt>
            <dd>
              <Link to={`/change-plans/${execution.planId}`}>{execution.planId}</Link>
            </dd>
          </div>
          <div>
            <dt>Plan version</dt>
            <dd>{execution.planVersion}</dd>
          </div>
          <div>
            <dt>Target</dt>
            <dd>{execution.executionTargetId}</dd>
          </div>
          <div>
            <dt>Target type</dt>
            <dd>{execution.executionTargetType ?? '—'}</dd>
          </div>
          <div>
            <dt>Environment</dt>
            <dd>{execution.executionTargetEnvironment ?? '—'}</dd>
          </div>
          <div>
            <dt>Cell</dt>
            <dd>{execution.cellId}</dd>
          </div>
          <div>
            <dt>Parameter</dt>
            <dd>{execution.parameterName}</dd>
          </div>
          <div>
            <dt>Verification status</dt>
            <dd>{execution.verificationStatus ?? '—'}</dd>
          </div>
          <div>
            <dt>Recovery status</dt>
            <dd>{execution.recoveryStatus ?? '—'}</dd>
          </div>
          <div>
            <dt>Window</dt>
            <dd>
              {formatTimestamp(execution.executionWindowOpensAt)} →{' '}
              {formatTimestamp(execution.executionWindowClosesAt)}
            </dd>
          </div>
          <div>
            <dt>Reviewed</dt>
            <dd>
              {execution.reviewedBy ?? '—'} · {formatTimestamp(execution.reviewedAt)}
            </dd>
          </div>
          <div>
            <dt>Authorized</dt>
            <dd>
              {execution.authorizedBy ?? '—'} · {formatTimestamp(execution.authorizedAt)}
            </dd>
          </div>
        </dl>
        {execution.failureCode || execution.failureDetailSafe ? (
          <p className="muted">
            {execution.failureCode ? `${execution.failureCode}: ` : ''}
            {execution.failureDetailSafe}
          </p>
        ) : null}
      </section>
      <section className="panel" aria-labelledby="exec-ops">
        <h2 id="exec-ops">Simulator operation</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Parameter</th>
              <th scope="col">Expected current</th>
              <th scope="col">Desired</th>
              <th scope="col">Cell</th>
            </tr>
          </thead>
          <tbody>
            {execution.operations.map((row) => (
              <tr key={row.sequenceNumber}>
                <td>{row.parameterName}</td>
                <td>{row.expectedCurrentValue ?? '—'}</td>
                <td>{row.desiredValue ?? '—'}</td>
                <td>{row.targetEntityId}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      {actionError ? <ErrorState error={actionError} /> : null}
      <section className="panel" aria-labelledby="exec-actions">
        <h2 id="exec-actions">Sandbox governance</h2>
        <div className="action-row">
          {canReviewExecution(execution) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => void review()}>
              Review sandbox execution
            </button>
          ) : null}
          {canAuthorizeExecution(execution) ? (
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm('authorize')}>
              Authorize sandbox execution
            </button>
          ) : null}
          {canExecuteInSandbox(execution) ? (
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm('execute')}>
              Execute in Sandbox
            </button>
          ) : null}
          {canVerifyExecution(execution) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => void verify()}>
              Confirm simulator readback
            </button>
          ) : null}
          {canCancelExecution(execution) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => void cancel()}>
              Cancel before mutation
            </button>
          ) : null}
          <button type="button" className="btn btn-quiet" disabled={busy} onClick={load}>
            Refresh
          </button>
        </div>
        {execution.status === ExecutionStatus.AUTHORIZED ? (
          <p className="muted">AUTHORIZED FOR SANDBOX. Not authorized for the network.</p>
        ) : null}
      </section>
      {confirm === 'authorize' ? (
        <ConfirmDialog
          title="Authorize sandbox execution?"
          confirmLabel="Authorize sandbox execution"
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => void authorize()}
        >
          <p>Authorization applies only to {SIMULATOR_EXECUTION_TARGET_ID}. NO REAL NETWORK CHANGE.</p>
        </ConfirmDialog>
      ) : null}
      {confirm === 'execute' ? (
        <ConfirmDialog
          title="Execute in sandbox?"
          confirmLabel="Execute in Sandbox"
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => void execute()}
        >
          <dl className="kv">
            <div>
              <dt>Target</dt>
              <dd>{SIMULATOR_EXECUTION_TARGET_ID}</dd>
            </div>
            <div>
              <dt>Parameter</dt>
              <dd>{execution.parameterName}</dd>
            </div>
            <div>
              <dt>Cell</dt>
              <dd>{execution.cellId}</dd>
            </div>
            <div>
              <dt>Expected current</dt>
              <dd>{operation?.expectedCurrentValue ?? '—'}</dd>
            </div>
            <div>
              <dt>Desired</dt>
              <dd>{operation?.desiredValue ?? '—'}</dd>
            </div>
            <div>
              <dt>Environment</dt>
              <dd>SIMULATOR</dd>
            </div>
          </dl>
          <p>
            <strong>NO REAL NETWORK CHANGE.</strong>
          </p>
        </ConfirmDialog>
      ) : null}
    </div>
  )
}
