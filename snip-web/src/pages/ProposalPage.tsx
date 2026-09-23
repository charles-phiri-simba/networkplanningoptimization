import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { snipApi } from '../api/snipApi'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { useAuth } from '../features/auth/AuthContext'
import { CandidateTable } from '../features/optimization/CandidateTable'
import { CurrentProposedPanel } from '../features/optimization/CurrentProposedPanel'
import { DemoPermissionBanner } from '../features/optimization/DemoPermissionBanner'
import { canApproveOrReject, canCreatePlan } from '../features/optimization/proposalGuards'
import { SimulationEvidence } from '../features/optimization/SimulationEvidence'
import type { ChangeProposalDetailDto } from '../types/proposal'
import { PlanPermission } from '../types/plan'
import { ProposalPermission } from '../types/proposal'
import { formatTimestamp } from '../utils/format'

type ConfirmKind = 'approve' | 'reject' | null

export function ProposalPage() {
  const { proposalId = '' } = useParams()
  const navigate = useNavigate()
  const { identity } = useAuth()
  const [detail, setDetail] = useState<ChangeProposalDetailDto | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<unknown>(null)
  const [confirm, setConfirm] = useState<ConfirmKind>(null)

  function load() {
    setError(null)
    snipApi.getChangeProposal(proposalId, ProposalPermission.VIEW).then(setDetail).catch(setError)
  }

  useEffect(() => {
    setDetail(null)
    load()
  }, [proposalId])

  async function runGovernance(kind: 'approve' | 'reject') {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const request = {
        reviewer: identity?.actorId ?? null,
        comment: kind === 'approve' ? 'Demo proposal approval' : 'Demo proposal rejection',
        reasonCode: kind === 'reject' ? 'NOT_SUITABLE' : null,
      }
      const next =
        kind === 'approve'
          ? await snipApi.approveChangeProposal(proposalId, request, ProposalPermission.APPROVE)
          : await snipApi.rejectChangeProposal(proposalId, request, ProposalPermission.REJECT)
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

  async function createPlan() {
    if (!detail || busy) {
      return
    }
    setBusy(true)
    setActionError(null)
    try {
      const plan = await snipApi.createChangePlan(
        { proposalId: detail.proposal.id },
        PlanPermission.CREATE,
      )
      navigate(`/change-plans/${plan.plan.id}`)
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
    return <LoadingState label="Loading optimization proposal…" />
  }

  const { proposal, candidates } = detail

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/optimization">Optimization</Link> / {proposal.id}
      </p>
      <header className="page-header">
        <div>
          <h1>Optimization proposal</h1>
          <p className="muted">
            Formal Phase 13 artifact for {proposal.targetEntityId}. Ask SNIP text is not the source
            of the proposed value.
          </p>
        </div>
        <StatusBadge status={proposal.status} />
      </header>
      <DemoPermissionBanner />
      <p className="banner-demo" role="note">
        Proposal APPROVED only makes the artifact eligible for change-plan creation. It is not
        production approval and not execution.
      </p>
      <CurrentProposedPanel proposal={proposal} />
      <section className="panel" aria-labelledby="proposal-meta">
        <h2 id="proposal-meta">Proposal details</h2>
        <dl className="kv">
          <div>
            <dt>Proposal ID</dt>
            <dd className="diagnostic">{proposal.id}</dd>
          </div>
          <div>
            <dt>Type</dt>
            <dd>{proposal.proposalType}</dd>
          </div>
          <div>
            <dt>Parameter</dt>
            <dd>{proposal.parameterName}</dd>
          </div>
          <div>
            <dt>Knowledge confidence</dt>
            <dd>{proposal.networkKnowledgeConfidence ?? '—'}</dd>
          </div>
          <div>
            <dt>Assurance confidence</dt>
            <dd>{proposal.assuranceConfidence ?? '—'}</dd>
          </div>
          <div>
            <dt>Simulation confidence</dt>
            <dd>{proposal.simulationConfidence ?? '—'}</dd>
          </div>
          <div>
            <dt>Risk</dt>
            <dd>{proposal.riskLevel ?? '—'}</dd>
          </div>
          <div>
            <dt>Benefit summary</dt>
            <dd>{proposal.benefitSummary ?? '—'}</dd>
          </div>
          <div>
            <dt>Proposal score</dt>
            <dd>{proposal.proposalScore ?? '—'}</dd>
          </div>
          <div>
            <dt>Version</dt>
            <dd>{proposal.version}</dd>
          </div>
          <div>
            <dt>Created</dt>
            <dd>{formatTimestamp(proposal.createdAt)}</dd>
          </div>
          <div>
            <dt>Evaluated</dt>
            <dd>{formatTimestamp(proposal.evaluatedAt)}</dd>
          </div>
          <div>
            <dt>Expires</dt>
            <dd>{formatTimestamp(proposal.expiresAt)}</dd>
          </div>
        </dl>
        {proposal.failureCode || proposal.failureReason ? (
          <p className="muted">
            {proposal.failureCode ? `${proposal.failureCode}: ` : ''}
            {proposal.failureReason}
          </p>
        ) : null}
        {proposal.invalidationReason ? (
          <p className="muted">Invalidation: {proposal.invalidationReason}</p>
        ) : null}
      </section>
      <section className="panel" aria-labelledby="candidates-heading">
        <h2 id="candidates-heading">Candidates</h2>
        <p className="muted">Rank 1 is the selected candidate when the backend assigned that rank.</p>
        <CandidateTable candidates={candidates} />
      </section>
      <SimulationEvidence candidates={candidates} />
      {actionError ? <ErrorState error={actionError} /> : null}
      <section className="panel" aria-labelledby="proposal-actions">
        <h2 id="proposal-actions">Governance</h2>
        {canApproveOrReject(proposal) ? (
          <div className="action-row">
            <button type="button" className="btn btn-primary" disabled={busy} onClick={() => setConfirm('approve')}>
              Approve proposal
            </button>
            <button type="button" className="btn" disabled={busy} onClick={() => setConfirm('reject')}>
              Reject proposal
            </button>
          </div>
        ) : (
          <p className="muted">Approve and reject are available only when status is RECOMMENDED.</p>
        )}
        {canCreatePlan(proposal) ? (
          <p>
            <button type="button" className="btn btn-primary" disabled={busy} onClick={createPlan}>
              {busy ? 'Creating plan…' : 'Create change plan'}
            </button>
          </p>
        ) : null}
      </section>
      {confirm ? (
        <ConfirmDialog
          title={confirm === 'approve' ? 'Approve proposal?' : 'Reject proposal?'}
          confirmLabel={confirm === 'approve' ? 'Approve proposal' : 'Reject proposal'}
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => void runGovernance(confirm)}
        >
          <p>
            This records Phase 13 governance only. It does not execute a network or sandbox change.
          </p>
        </ConfirmDialog>
      ) : null}
    </div>
  )
}
