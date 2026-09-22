import type { ContextProvenanceDto, RadioParameterDto } from '../../types/network'
import { EmptyState } from '../../components/EmptyState'
import { formatTimestamp } from '../../utils/format'

export function ConfigurationPanel({
  parameters,
  provenance,
}: {
  parameters: RadioParameterDto[]
  provenance: ContextProvenanceDto
}) {
  if (parameters.length === 0) {
    return <EmptyState title="No radio configuration" detail="The backend returned an empty radioConfiguration list." />
  }

  return (
    <section className="panel" aria-labelledby="config-heading">
      <header className="panel-header">
        <h2 id="config-heading">Configuration</h2>
        <p className="muted">
          Read-only canonical parameters · source {provenance.source}
          {provenance.synthetic ? ' · synthetic/demo' : ''}
        </p>
      </header>
      <table className="data-table">
        <thead>
          <tr>
            <th scope="col">Parameter</th>
            <th scope="col">Current value</th>
            <th scope="col">Unit</th>
            <th scope="col">Effective from</th>
            <th scope="col">Provenance</th>
          </tr>
        </thead>
        <tbody>
          {parameters.map((parameter) => (
            <tr key={parameter.parameterName}>
              <td>{parameter.parameterName}</td>
              <td>{parameter.parameterValue}</td>
              <td>{parameter.unit ?? '—'}</td>
              <td>{formatTimestamp(parameter.effectiveFrom)}</td>
              <td>{provenance.synthetic ? 'Synthetic / demo' : provenance.source}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
