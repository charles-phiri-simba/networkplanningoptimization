import type { KpiObservationDto } from '../../types/network'
import { formatNumber } from '../../utils/format'

export function TelemetryChart({
  metric,
  points,
}: {
  metric: string
  points: KpiObservationDto[]
}) {
  const values = points
    .map((point) => point.value)
    .filter((value): value is number => value !== null && !Number.isNaN(value))
  if (values.length === 0) {
    return <p className="muted">No numeric history for {metric}.</p>
  }

  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const width = 280
  const height = 64
  const path = values
    .map((value, index) => {
      const x = values.length === 1 ? width / 2 : (index / (values.length - 1)) * width
      const y = height - ((value - min) / span) * (height - 8) - 4
      return `${index === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`
    })
    .join(' ')

  return (
    <figure className="sparkline">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label={`${metric} recent history from ${formatNumber(min)} to ${formatNumber(max)}`}
      >
        <path d={path} fill="none" stroke="currentColor" strokeWidth="2" />
      </svg>
      <figcaption className="muted">
        Bounded recent history ({values.length} point{values.length === 1 ? '' : 's'}), not long-term analytics.
      </figcaption>
    </figure>
  )
}
