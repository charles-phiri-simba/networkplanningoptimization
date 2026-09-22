import { EmptyState } from '../../components/EmptyState'
import { StatusBadge } from '../../components/StatusBadge'
import type { NeighbourDto } from '../../types/network'

export function NeighbourPanel({ neighbours }: { neighbours: NeighbourDto[] }) {
  return (
    <section className="panel" aria-labelledby="neighbour-heading">
      <header className="panel-header">
        <h2 id="neighbour-heading">Neighbours</h2>
        <p className="muted">Read-only neighbour relations returned with this cell context.</p>
      </header>
      {neighbours.length === 0 ? (
        <EmptyState title="No neighbours" detail="No neighbour relations were returned for this cell." />
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Target cell</th>
              <th scope="col">Relation</th>
              <th scope="col">Status</th>
            </tr>
          </thead>
          <tbody>
            {neighbours.map((neighbour) => (
              <tr key={`${neighbour.targetCellId}-${neighbour.relationType}`}>
                <td>{neighbour.targetCellId}</td>
                <td>{neighbour.relationType}</td>
                <td>
                  <StatusBadge status={neighbour.status} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  )
}
