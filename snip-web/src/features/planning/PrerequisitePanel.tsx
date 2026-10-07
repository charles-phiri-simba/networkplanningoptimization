import { snipApi } from '../../api/snipApi'
import type { PlanningPrerequisiteDto } from '../../types/planning'

export function PrerequisitePanel({
  cells,
  busyCell,
  onBusy,
  onSynchronized,
}: {
  cells: PlanningPrerequisiteDto[]
  busyCell: string | null
  onBusy?: (cellId: string | null) => void
  onSynchronized: () => void
}) {
  return (
    <section className="panel" aria-labelledby="prereq-heading">
      <header className="panel-header">
        <h2 id="prereq-heading">Digital Twin prerequisites</h2>
        <p className="muted">
          Evaluation requires a CURRENT cell Digital Twin. Featured demo cells are synchronized at
          demo startup. Synchronization remains an explicit action, not a continuous background twin.
        </p>
      </header>
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Cell</th>
            <th scope="col">Freshness</th>
            <th scope="col">Current observed txPower</th>
            <th scope="col">Can evaluate</th>
            <th scope="col" />
          </tr>
        </thead>
        <tbody>
          {cells.map((cell) => (
            <tr key={cell.cellId}>
              <td>{cell.cellId}</td>
              <td>{cell.freshness}</td>
              <td>{cell.observedTxPower == null ? '—' : `${cell.observedTxPower} dBm (not pinned)`}</td>
              <td>{cell.canEvaluate ? 'Yes' : 'No'}</td>
              <td>
                {cell.canEvaluate ? null : (
                  <button
                    type="button"
                    className="btn btn-quiet"
                    disabled={busyCell === cell.cellId}
                    onClick={() => {
                      onBusy?.(cell.cellId)
                      void snipApi
                        .synchronizeCellTwin(cell.cellId)
                        .then(onSynchronized)
                        .catch(() => onBusy?.(null))
                    }}
                  >
                    {busyCell === cell.cellId ? 'Synchronizing…' : 'Synchronize twin'}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
