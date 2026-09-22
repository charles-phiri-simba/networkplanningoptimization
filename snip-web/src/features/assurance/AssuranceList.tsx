import { Link } from 'react-router-dom'
import type { AssuranceCaseDto } from '../../types/assurance'
import { EmptyState } from '../../components/EmptyState'
import { StatusBadge } from '../../components/StatusBadge'
import { formatTimestamp } from '../../utils/format'

export function AssuranceList({ cases }: { cases: AssuranceCaseDto[] }) {
  if (cases.length === 0) {
    return <EmptyState title="No assurance cases" detail="No findings were returned for this scope." />
  }

  return (
    <table className="data-table">
      <thead>
        <tr>
          <th scope="col">Type</th>
          <th scope="col">Severity</th>
          <th scope="col">Status</th>
          <th scope="col">Object</th>
          <th scope="col">Detected</th>
          <th scope="col">Case</th>
        </tr>
      </thead>
      <tbody>
        {cases.map((item) => (
          <tr key={item.id}>
            <td>{item.caseType}</td>
            <td>
              <StatusBadge status={item.severity} kind="severity" />
            </td>
            <td>
              <StatusBadge status={item.status} />
            </td>
            <td>
              {item.affectedEntityType} {item.affectedEntityId}
            </td>
            <td>{formatTimestamp(item.detectedAt)}</td>
            <td>
              <Link to={`/assurance/${item.id}`}>Open case</Link>
              {item.synthetic ? <span className="muted"> · synthetic</span> : null}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
