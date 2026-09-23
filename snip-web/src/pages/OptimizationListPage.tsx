import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { DemoPermissionBanner } from '../features/optimization/DemoPermissionBanner'
import type { ChangeProposalSummaryDto } from '../types/proposal'
import { ProposalPermission } from '../types/proposal'
import { formatTimestamp } from '../utils/format'

export function OptimizationListPage() {
  const [proposals, setProposals] = useState<ChangeProposalSummaryDto[] | null>(null)
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    snipApi.listChangeProposals(ProposalPermission.VIEW).then(setProposals).catch(setError)
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Optimization</h1>
          <p className="muted">
            Governed txPower proposals. Ask SNIP remains a separate advisory explanation.
          </p>
        </div>
      </header>
      <DemoPermissionBanner />
      {error ? <ErrorState error={error} onRetry={load} /> : null}
      {!error && !proposals ? <LoadingState label="Loading proposals…" /> : null}
      {proposals && proposals.length === 0 ? (
        <EmptyState
          title="No optimization proposals"
          detail="Open a cell workspace and choose Propose txPower optimization."
        />
      ) : null}
      {proposals && proposals.length > 0 ? (
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Cell</th>
              <th scope="col">Status</th>
              <th scope="col">Current</th>
              <th scope="col">Proposed</th>
              <th scope="col">Created</th>
            </tr>
          </thead>
          <tbody>
            {proposals.map((proposal) => (
              <tr key={proposal.id}>
                <td>
                  <Link to={`/optimization/proposals/${proposal.id}`}>
                    {proposal.targetEntityId}
                  </Link>
                </td>
                <td>
                  <StatusBadge status={proposal.status} />
                </td>
                <td>
                  {proposal.currentValue ?? '—'} {proposal.unit ?? ''}
                </td>
                <td>
                  {proposal.proposedValue ?? '—'} {proposal.unit ?? ''}
                </td>
                <td>{formatTimestamp(proposal.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : null}
    </div>
  )
}
