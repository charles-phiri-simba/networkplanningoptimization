import type { KpiObservationDto, KpiSeriesDto } from '../../types/network'
import { EmptyState } from '../../components/EmptyState'
import { formatNumber, formatTimestamp } from '../../utils/format'
import { TelemetryChart } from './TelemetryChart'

export function KpiPanel({
  kpis,
  telemetry,
}: {
  kpis: KpiObservationDto[]
  telemetry: KpiSeriesDto[]
}) {
  return (
    <div className="stack">
      <section className="panel" aria-labelledby="kpi-heading">
        <header className="panel-header">
          <h2 id="kpi-heading">KPI summary</h2>
          <p className="muted">Latest observations returned by the backend.</p>
        </header>
        {kpis.length === 0 ? (
          <EmptyState title="No KPI observations" detail="GET /api/v1/cells/{cellId}/kpis returned no rows." />
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Metric</th>
                <th scope="col">Value</th>
                <th scope="col">Unit</th>
                <th scope="col">Observed</th>
                <th scope="col">Provenance</th>
              </tr>
            </thead>
            <tbody>
              {kpis.map((kpi) => (
                <tr key={`${kpi.metric}-${kpi.eventId ?? kpi.observedAt}`}>
                  <td>{kpi.metric}</td>
                  <td>{formatNumber(kpi.value)}</td>
                  <td>{kpi.unit ?? '—'}</td>
                  <td>{formatTimestamp(kpi.observedAt ?? kpi.eventTime)}</td>
                  <td>
                    {kpi.source ?? '—'}
                    {kpi.synthetic ? ' · synthetic' : ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel" aria-labelledby="telemetry-heading">
        <header className="panel-header">
          <h2 id="telemetry-heading">Telemetry</h2>
          <p className="muted">Latest value, bounded history, and trend from GET /telemetry.</p>
        </header>
        {telemetry.length === 0 ? (
          <EmptyState title="No telemetry series" />
        ) : (
          <div className="telemetry-grid">
            {telemetry.map((series) => (
              <article key={series.metric} className="telemetry-card">
                <h3>{series.metric}</h3>
                <p>
                  {formatNumber(series.current.value)} {series.current.unit ?? ''}
                </p>
                <p className="muted">
                  Trend {series.trend} · {formatTimestamp(series.current.observedAt)}
                  {series.current.synthetic ? ' · synthetic' : ''}
                </p>
                <TelemetryChart
                  metric={series.metric}
                  points={[...series.history, series.current]}
                />
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
