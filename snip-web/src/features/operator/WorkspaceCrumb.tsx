import { Link } from 'react-router-dom'

export function WorkspaceCrumb({
  cellId,
  proposalId,
  planId,
  executionId,
}: {
  cellId?: string | null
  proposalId?: string | null
  planId?: string | null
  executionId?: string | null
}) {
  return (
    <p className="crumb">
      <Link to="/network">Network</Link>
      {cellId ? (
        <>
          {' / '}
          <Link to={`/network/cells/${encodeURIComponent(cellId)}`}>{cellId}</Link>
        </>
      ) : null}
      {cellId ? (
        <>
          {' / '}
          <Link to={`/network/cells/${encodeURIComponent(cellId)}/optimize`}>Optimization</Link>
        </>
      ) : (
        <>
          {' / '}
          <Link to="/optimization">Optimization</Link>
        </>
      )}
      {proposalId ? (
        <>
          {' / '}
          <Link to={`/optimization/proposals/${encodeURIComponent(proposalId)}`}>Proposal</Link>
        </>
      ) : null}
      {planId ? (
        <>
          {' / '}
          <Link to={`/change-plans/${encodeURIComponent(planId)}`}>Change plan</Link>
        </>
      ) : null}
      {executionId ? <> / Sandbox execution</> : null}
    </p>
  )
}
