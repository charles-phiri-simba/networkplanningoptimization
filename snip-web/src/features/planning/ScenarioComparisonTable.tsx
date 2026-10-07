import type { PlanningComparisonDto } from '../../types/planning'

export function ScenarioComparisonTable({ comparison }: { comparison: PlanningComparisonDto }) {
  return (
    <section className="panel" aria-labelledby="compare-heading">
      <header className="panel-header">
        <h2 id="compare-heading">Per-cell comparison</h2>
        <p className="muted">
          Independent cell-local synthetic deltas. Combined site or network scores are not computed.
          {comparison.historical ? ' This is a historical evaluation against pinned twin versions.' : ''}
        </p>
      </header>
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Alternative</th>
            <th scope="col">Cell</th>
            <th scope="col">Baseline txPower</th>
            <th scope="col">Intended txPower</th>
            <th scope="col">Outcome</th>
            <th scope="col">Confidence</th>
            <th scope="col">Twin version</th>
          </tr>
        </thead>
        <tbody>
          {comparison.rows.map((row) => (
            <tr key={`${row.alternativeId}-${row.cellId}`}>
              <td>{row.alternativeName}</td>
              <td>{row.cellId}</td>
              <td>{row.baselineTxPower ?? '—'}</td>
              <td>{row.intendedTxPower}</td>
              <td>{row.outcome}</td>
              <td>
                {row.synthetic ? 'SYNTHETIC' : ''} {row.confidence ?? ''}
              </td>
              <td>{row.twinVersion ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
