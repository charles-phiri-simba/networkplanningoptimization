import { formatStatusLabel } from '../utils/format'

export function StatusBadge({
  status,
  kind = 'status',
}: {
  status: string
  kind?: 'status' | 'severity'
}) {
  const tone = toneFor(status, kind)
  return (
    <span className={`badge badge-${tone}`}>
      <span className="badge-dot" aria-hidden="true" />
      <span>{formatStatusLabel(status)}</span>
    </span>
  )
}

function toneFor(status: string, kind: 'status' | 'severity'): string {
  const value = status.toUpperCase()
  if (kind === 'severity') {
    if (value.includes('CRITICAL') || value.includes('MAJOR')) return 'critical'
    if (value.includes('WARNING') || value.includes('MINOR')) return 'warning'
    if (value.includes('INFO') || value.includes('LOW')) return 'info'
    return 'neutral'
  }
  if (value === 'ACTIVE' || value === 'OPEN' || value === 'CURRENT') return 'ok'
  if (value === 'STALE' || value === 'DEGRADED') return 'warning'
  if (value === 'FAILED' || value === 'DOWN' || value === 'EXPIRED') return 'critical'
  return 'neutral'
}
