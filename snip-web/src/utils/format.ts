export function formatNumber(value: number | null | undefined, digits = 3): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—'
  }
  return Number(value).toLocaleString(undefined, { maximumFractionDigits: digits })
}

export function formatTimestamp(value: string | null | undefined): string {
  if (!value) {
    return '—'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toISOString().replace('T', ' ').replace('Z', ' UTC')
}

export function formatCoordinate(value: number | null | undefined): string {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return '—'
  }
  return value.toFixed(5)
}

export function formatStatusLabel(status: string | null | undefined): string {
  if (!status) {
    return 'Unknown'
  }
  return status.replaceAll('_', ' ')
}
