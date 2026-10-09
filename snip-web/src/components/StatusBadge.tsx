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
  if (value === 'HIGH') return 'high'
  if (value === 'LOW') return 'low'
  if (value === 'CRITICAL') return 'critical'
  if (value === 'OPEN') return 'open'
  if (value === 'APPROVED') return 'approved'
  if (value === 'AUTHORIZED') return 'authorized'
  if (value === 'READY' || value === 'READY_FOR_EXECUTION' || value === 'READY_FOR_SANDBOX_ADMISSION') {
    return 'ready'
  }
  if (value === 'APPLIED') return 'applied'
  if (value === 'VERIFIED') return 'verified'
  if (value === 'NOT_REQUIRED' || value === 'NOT_REQUESTED') return 'not-required'
  if (value === 'FAILED' || value.includes('FAILED')) return 'failed'
  if (value === 'BLOCKED') return 'blocked'
  if (value === 'ACTIVE' || value === 'CURRENT' || value === 'RECOMMENDED') {
    return 'ok'
  }
  if (
    value === 'STALE' ||
    value === 'DEGRADED' ||
    value === 'EVALUATED' ||
    value === 'RECOVERY_REQUIRED'
  ) {
    return 'warning'
  }
  if (value === 'DOWN' || value === 'EXPIRED' || value === 'UNKNOWN') {
    return 'critical'
  }
  return 'neutral'
}
