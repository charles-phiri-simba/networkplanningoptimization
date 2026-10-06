import { metricLabel } from '../operator/operatorMessages'

const CASE_TYPE_LABELS: Record<string, string> = {
  DEGRADING_RADIO_QUALITY: 'Degrading radio quality',
}

const STATUS_LABELS: Record<string, string> = {
  OPEN: 'Open',
  ACKNOWLEDGED: 'Acknowledged',
  RESOLVED: 'Resolved',
}

const SEVERITY_LABELS: Record<string, string> = {
  CRITICAL: 'Critical',
  MAJOR: 'Major',
  WARNING: 'Warning',
  INFO: 'Info',
}

export function issueTypeLabel(caseType: string): string {
  return CASE_TYPE_LABELS[caseType] ?? caseType.replaceAll('_', ' ')
}

export function issueStatusLabel(status: string): string {
  return STATUS_LABELS[status] ?? status.replaceAll('_', ' ')
}

export function issueSeverityLabel(severity: string): string {
  return SEVERITY_LABELS[severity] ?? severity.replaceAll('_', ' ')
}

export function issueMetricLabel(metric: string | null | undefined): string {
  if (!metric) {
    return '—'
  }
  return metricLabel(metric)
}
