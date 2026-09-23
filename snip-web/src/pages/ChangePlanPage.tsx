import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { snipApi } from '../api/snipApi'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../features/auth/AuthContext'
import {
  canAssessReadiness,
  canAuthorizePlan,
  canCancelPlan,
  canRequestSandbox,
  canReviewPlan,
} from '../features/changePlanning/planGuards'
import { DemoPermissionBanner } from '../features/optimization/DemoPermissionBanner'
import { ExecutionPermission, SIMULATOR_EXECUTION_TARGET_ID } from '../types/execution'
import type { ChangePlanDetailDto } from '../types/plan'
import { PlanPermission, PlanStatus } from '../types/plan'
import { formatTimestamp } from '../utils/format'

type ConfirmKind = 'authorize' | 'cancel' | null

export function ChangePlanPage() {
  const { planId = '' } = useParams()
  const navigate = useNavigate()
  const { identity } = useAuth()
  const [detail, setDetail] = useState<ChangePlanDetailDto | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const [confirm, setConfirm] = useState<ConfirmKind>(null)

  function load() {
    setError(null)
    snipApi.getChangePlan(planId, PlanPermission.VIEW).then(setDetail).catch(setError)
  }

  useEffect(() => {
    setDetail(null)
    load()
  }, [planId])

  async function review() {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.reviewChangePlan(
        planId,
        { reviewer: identity?.actorId ?? null, comment: 'Engineering review recorded' },
        PlanPermission.REVIEW,
      )
      setDetail(next)
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
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.authorizeChangePlan(
        planId,
        { authorizer: identity?.actorId ?? null },
        PlanPermission.AUTHORIZE,
      )
      setDetail(next)
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

  async function readiness() {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.assessChangePlanReadiness(planId, PlanPermission.AUTHORIZE)
      setDetail(next)
    } catch (caught) {
      if (caught instanceof ApiError && caught.status === 409) {
        load()
      }
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  async function cancel() {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const next = await snipApi.cancelChangePlan(
        planId,
        { actor: identity?.actorId ?? null, reason: 'Cancelled from Increment 2 UI' },
        PlanPermission.CANCEL,
      )
      setDetail(next)
      setConfirm(null)
    } catch (caught) {
      setActionError(caught)
      setConfirm(null)
    } finally {
      setBusy(false)
    }
  }

  async function requestSandbox() {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const execution = await snipApi.requestSandboxExecution(
        { planId: detail.plan.id, executionTargetId: SIMULATOR_EXECUTION_TARGET_ID },
        ExecutionPermission.REQUEST,
      )
      navigate(`/sandbox/executions/${execution.executionId}`)
    } catch (caught) {
      setActionError(caught)
    } finally {
      setBusy(false)
    }
  }

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!detail) {
    return <LoadingState label="Loading change plan…" />
  }

  const { plan } = detail
  const latestReadiness = detail.readinessAssessments.at(-1)
  const sandboxDisabled =
    actionError instanceof ApiError && actionError.failureCode === 'CHANGE_EXECUTION_DISABLED'

  return (
    <div className="page">
      <p className="crumb">
        <Link to={`/optimization/proposals/${plan.proposalId}`}>Proposal</Link> / plan
      </p>
      <header className="page-header">
        <div>
          <h1>Change plan</h1>
          <p className="muted">
            Phase 14 governed plan. Engineering Review maps to POST /plans/{'{planId}'}/review and
            does not authorize execution.
          </p>
        </div>
        <StatusBadge status={plan.status} />
      </header>
      <DemoPermissionBanner />
      {plan.status === PlanStatus.READY_FOR_EXECUTION ? (
        <p className="banner-sandbox" role="note">
          <strong>READY FOR SANDBOX ADMISSION.</strong> Backend status remains READY_FOR_EXECUTION.
          This is not production execution authorization. NO REAL NETWORK CHANGE.
        </p>
      ) : null}
      <section className="panel" aria-labelledby="plan-meta">
        <h2 id="plan-meta">Plan details</h2>
        <dl className="kv">
          <div>
            <dt>Plan ID</dt>
            <dd className="diagnostic">{plan.id}</dd>
          </div>
          <div>
            <dt>Proposal ID</dt>
            <dd>
              <Link to={`/optimization/proposals/${plan.proposalId}`}>{plan.proposalId}</Link>
            </dd>
          </div>
          <div>
            <dt>Target</dt>
            <dd>
              {plan.targetEntityType} {plan.targetEntityId}
            </dd>
          </div>
          <div>
            <dt>Parameter</dt>
            <dd>{plan.parameterName}</dd>
          </div>
          <div>
            <dt>Expected current</dt>
            <dd>{plan.expectedCurrentValue ?? '—'}</dd>
          </div>
          <div>
            <dt>Desired</dt>
            <dd>{plan.desiredValue ?? '—'}</dd>
          </div>
          <div>
            <dt>Impact</dt>
            <dd>{plan.impactLevel ?? '—'}</dd>
          </div>
          <div>
            <dt>Risk</dt>
            <dd>{detail.riskLevel ?? '—'}</dd>
          </div>
          <div>
            <dt>Knowledge at creation</dt>
            <dd>{detail.knowledgeConfidenceAtCreation ?? '—'}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatTimestamp(plan.createdAt)}</dd>
          </div>
          <div>
            <dt>Expires</dt>
            <dd>{formatTimestamp(plan.expiresAt)}</dd>
          </div>
          <div>
            <dt>Fingerprint</dt>
            <dd className="diagnostic">{detail.fingerprint ?? '—'}</dd>
          </div>
          <div>
            <dt>Authorized fingerprint</dt>
            <dd className="diagnostic">{detail.authorizedFingerprint ?? '—'}</dd>
          </div>
          <div>
            <dt>Reviewed</dt>
            <dd>
              {detail.reviewedBy ?? '—'} · {formatTimestamp(detail.reviewedAt)}
            </dd>
          </div>
          <div>
            <dt>Authorized</dt>
            <dd>
              {detail.authorizedBy ?? '—'} · {formatTimestamp(detail.authorizedAt)}
            </dd>
          </div>
        </dl>
      </section>
      <section className="panel" aria-labelledby="ops-heading">
        <h2 id="ops-heading">Forward operation</h2>
        <OperationTable rows={detail.operations} />
      </section>
      <section className="panel" aria-labelledby="rollback-heading">
        <h2 id="rollback-heading">Rollback operation</h2>
        <p className="muted">Rollback values are derived by the backend. Increment 2 does not execute rollback from this page.</p>
        <OperationTable rows={detail.rollbackOperations} />
      </section>
      <section className="panel" aria-labelledby="precond-heading">
        <h2 id="precond-heading">Preconditions</h2>
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Type</th>
              <th scope="col">Expected</th>
              <th scope="col">Observed</th>
              <th scope="col">Result</th>
              <th scope="col">Reason</th>
            </tr>
          </thead>
          <tbody>
            {detail.preconditions.map((item, index) => (
              <tr key={`${item.preconditionType}-${index}`}>
                <td>{item.preconditionType}</td>
                <td>{item.expectedCondition ?? '—'}</td>
                <td>{item.observedValue ?? '—'}</td>
                <td>{item.result ?? '—'}</td>
                <td>{item.reasonCode ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
      <section className="panel" aria-labelledby="ready-heading">
        <h2 id="ready-heading">Readiness assessments</h2>
        {latestReadiness ? (
          <p>
            Latest result: <strong>{latestReadiness.result ?? '—'}</strong>
            {latestReadiness.reasonCodes ? ` · ${latestReadiness.reasonCodes}` : ''}
          </p>
        ) : (
          <p className="muted">No readiness assessment has been recorded yet.</p>
        )}
      </section>
      {sandboxDisabled ? (
        <p className="banner-sandbox" role="alert">
          Sandbox execution is disabled in this runtime. NO REAL NETWORK CHANGE. The plan remains
          visible. Enable only with a local runtime override:{' '}
          <code>--snip.change-execution.enabled=true</code>. Do not change the committed default.
        </p>
      ) : null}
      {actionError && !sandboxDisabled ? <ErrorState error={actionError} /> : null}
      {actionError && sandboxDisabled ? <ErrorState error={actionError} /> : null}
      <section className="panel" aria-labelledby="plan-actions">
        <h2 id="plan-actions">Governance</h2>
        <div className="action-row">
          {canReviewPlan(detail) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => void review()}>
              Engineering review
            </button>
          ) : null}
          {canAuthorizePlan(detail) ? (
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm('authorize')}>
              Authorize plan
            </button>
          ) : null}
          {canAssessReadiness(detail) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => void readiness()}>
              Assess readiness
            </button>
          ) : null}
          {canRequestSandbox(detail) ? (
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => void requestSandbox()}>
              Request sandbox execution
            </button>
          ) : null}
          {canCancelPlan(detail) ? (
            <button type="button" className="btn" disabled={busy} onClick={() => setConfirm('cancel')}>
              Cancel plan
            </button>
          ) : null}
        </div>
        {!canReviewPlan(detail) && !canAuthorizePlan(detail) && !canAssessReadiness(detail) ? (
          <p className="muted">No plan mutations are available for status {plan.status}.</p>
        ) : null}
      </section>
      {confirm === 'authorize' ? (
        <ConfirmDialog
          title="Authorize plan?"
          confirmLabel="Authorize plan"
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => void authorize()}
        >
          <p>Plan authorized — not executed. This is not approved for production.</p>
        </ConfirmDialog>
      ) : null}
      {confirm === 'cancel' ? (
        <ConfirmDialog
          title="Cancel plan?"
          confirmLabel="Cancel plan"
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => void cancel()}
        >
          <p>Cancel is the plan abandonment path. There is no plan reject API.</p>
        </ConfirmDialog>
      ) : null}
    </div>
  )
}

function OperationTable({
  rows,
}: {
  rows: ChangePlanDetailDto['operations'] | ChangePlanDetailDto['rollbackOperations']
}) {
  if (rows.length === 0) {
    return <p className="muted">None returned.</p>
  }
  return (
    <table className="data-table">
      <thead>
        <tr>
          <th scope="col">Seq</th>
          <th scope="col">Type</th>
          <th scope="col">Target</th>
          <th scope="col">Parameter</th>
          <th scope="col">Expected current</th>
          <th scope="col">Desired</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={`${row.sequenceNumber}-${row.parameterName}`}>
            <td>{row.sequenceNumber}</td>
            <td>{row.operationType}</td>
            <td>
              {row.targetEntityType} {row.targetEntityId}
            </td>
            <td>{row.parameterName}</td>
            <td>{row.expectedCurrentValue ?? '—'}</td>
            <td>{row.desiredValue ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
