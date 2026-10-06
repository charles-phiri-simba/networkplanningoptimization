import type { ChangePlanPreconditionDto } from '../../types/plan'
import { readinessLabel } from '../operator/operatorMessages'

export function ReadinessChecklist({ preconditions }: { preconditions: ChangePlanPreconditionDto[] }) {
  if (preconditions.length === 0) {
    return <p className="muted">No readiness preconditions were returned.</p>
  }
  return (
    <table className="data-table">
      <thead>
        <tr>
          <th scope="col">Check</th>
          <th scope="col">Result</th>
          <th scope="col">Expected</th>
          <th scope="col">Observed</th>
          <th scope="col">Reason</th>
        </tr>
      </thead>
      <tbody>
        {preconditions.map((item, index) => (
          <tr key={`${item.preconditionType}-${index}`}>
            <td>
              {readinessLabel(item.preconditionType)}
              <p className="muted diagnostic">{item.preconditionType}</p>
            </td>
            <td>{item.result ?? '—'}</td>
            <td>{item.expectedCondition ?? '—'}</td>
            <td>{item.observedValue ?? '—'}</td>
            <td>{item.reasonCode ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
