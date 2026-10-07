import { Link } from 'react-router-dom'
import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellContextDto, CellDto } from '../../types/network'
import { findTxPower } from '../optimization/txPower'
import {
  UNAVAILABLE_LABEL,
  contextIsSynthetic,
  formatMetricDisplay,
  highestActiveSeverityForCell,
  relatedAssuranceLabel,
  txPowerDisplay,
} from './comparisonMetrics'
import { relationshipLabel, type RelationshipKind } from './relatedCells'

export interface ComparisonRowModel {
  cell: CellDto
  context: CellContextDto | null
  failed: boolean
  pending?: boolean
  relationships: RelationshipKind[]
}

export function CellComparisonTable({
  rows,
  selectedCellId,
  showRelationships,
  truncated,
  cases,
  casesUnavailable,
}: {
  rows: ComparisonRowModel[]
  selectedCellId?: string
  showRelationships: boolean
  truncated?: boolean
  cases: AssuranceCaseDto[] | null
  casesUnavailable?: boolean
}) {
  return (
    <div>
      {truncated ? (
        <p className="muted" role="status">
          Related-cell context is limited to 12 cells in this workspace.
        </p>
      ) : null}
      <div className="table-wrap">
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Cell</th>
              <th scope="col">gNB</th>
              {showRelationships ? <th scope="col">Relationship</th> : null}
              <th scope="col">txPower</th>
              <th scope="col">BLER_DL</th>
              <th scope="col">PRB_UTILIZATION_DL</th>
              <th scope="col">Assurance</th>
              <th scope="col">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <ComparisonRow
                key={row.cell.cellId}
                row={row}
                selected={row.cell.cellId === selectedCellId}
                showRelationships={showRelationships}
                cases={cases}
                casesUnavailable={casesUnavailable}
              />
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function ComparisonRow({
  row,
  selected,
  showRelationships,
  cases,
  casesUnavailable,
}: {
  row: ComparisonRowModel
  selected: boolean
  showRelationships: boolean
  cases: AssuranceCaseDto[] | null
  casesUnavailable?: boolean
}) {
  const pending = Boolean(row.pending)
  const context = row.failed || pending ? null : row.context
  const txPower = context ? findTxPower(context.radioConfiguration) : undefined
  const synthetic = contextIsSynthetic(context)
  const metricLabel = pending ? 'Loading…' : row.failed || !context ? UNAVAILABLE_LABEL : null
  const assurance = casesUnavailable
    ? UNAVAILABLE_LABEL
    : cases === null
      ? '—'
      : relatedAssuranceLabel(highestActiveSeverityForCell(cases, row.cell.cellId))

  return (
    <tr className={selected ? 'comparison-row-selected' : undefined} aria-current={selected ? 'true' : undefined}>
      <td>
        {row.cell.name} <span className="muted">{row.cell.cellId}</span>
        {synthetic ? (
          <p className="muted">Synthetic demo observation — not live network data.</p>
        ) : null}
      </td>
      <td>{row.cell.gnbId || UNAVAILABLE_LABEL}</td>
      {showRelationships ? (
        <td>
          {row.relationships.length === 0 ? (
            <span className="muted">—</span>
          ) : (
            <ul className="related-labels">
              {row.relationships.map((kind) => (
                <li key={kind}>{relationshipLabel(kind)}</li>
              ))}
            </ul>
          )}
        </td>
      ) : null}
      <td>{metricLabel ?? txPowerDisplay(context)}</td>
      <td>{metricLabel ?? formatMetricDisplay(context, 'BLER_DL')}</td>
      <td>{metricLabel ?? formatMetricDisplay(context, 'PRB_UTILIZATION_DL')}</td>
      <td>{assurance}</td>
      <td>
        {selected ? (
          <span className="muted">Current workspace</span>
        ) : (
          <>
            <p>
              <Link to={`/network/cells/${encodeURIComponent(row.cell.cellId)}`}>Open cell</Link>
            </p>
            {txPower?.parameterValue ? (
              <p>
                <Link
                  to={`/network/cells/${encodeURIComponent(row.cell.cellId)}/optimize`}
                  aria-label={`Propose txPower optimization (${row.cell.cellId})`}
                >
                  Propose txPower optimization
                </Link>
              </p>
            ) : null}
          </>
        )}
      </td>
    </tr>
  )
}
