import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../../api/snipApi'
import { EmptyState } from '../../components/EmptyState'
import { StatusBadge } from '../../components/StatusBadge'
import type { CellDto, NeighbourDto } from '../../types/network'

export function NeighbourPanel({ neighbours }: { neighbours: NeighbourDto[] }) {
  const [knownCellIds, setKnownCellIds] = useState<Set<string>>(new Set())

  useEffect(() => {
    let cancelled = false
    snipApi
      .listCells()
      .then((cells: CellDto[]) => {
        if (!cancelled) {
          setKnownCellIds(new Set(cells.map((cell) => cell.cellId)))
        }
      })
      .catch(() => {
        if (!cancelled) {
          setKnownCellIds(new Set())
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <section className="panel" aria-labelledby="neighbour-heading">
      <header className="panel-header">
        <h2 id="neighbour-heading">Neighbours</h2>
        <p className="muted">
          Configured outgoing neighbour relations returned with this cell context. Direction is
          preserved; a listed target is not an interference or handover-quality measurement.
        </p>
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
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {neighbours.map((neighbour) => {
              const known = knownCellIds.has(neighbour.targetCellId)
              return (
                <tr key={`${neighbour.targetCellId}-${neighbour.relationType}`}>
                  <td>{neighbour.targetCellId}</td>
                  <td>{neighbour.relationType}</td>
                  <td>
                    <StatusBadge status={neighbour.status} />
                  </td>
                  <td>
                    {known ? (
                      <Link to={`/network/cells/${encodeURIComponent(neighbour.targetCellId)}`}>
                        Open cell
                      </Link>
                    ) : (
                      <span className="muted">Not in inventory</span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </section>
  )
}
