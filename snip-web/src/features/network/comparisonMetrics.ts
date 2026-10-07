import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellContextDto } from '../../types/network'
import { findTxPower } from '../optimization/txPower'
import { activeCases, severityRank } from '../operations/operationsModel'

export const UNAVAILABLE_LABEL = 'Unavailable'

export function currentMetric(
  context: CellContextDto | null | undefined,
  metric: string,
): { value: number; unit: string | null; synthetic: boolean } | null {
  if (!context) {
    return null
  }
  const series = context.telemetry.find((item) => item.metric === metric)
  const observation = series?.current ?? context.kpis.find((item) => item.metric === metric)
  if (!observation || observation.value === null || Number.isNaN(observation.value)) {
    return null
  }
  return {
    value: observation.value,
    unit: observation.unit,
    synthetic: observation.synthetic || context.provenance.synthetic,
  }
}

export function formatMetricDisplay(
  context: CellContextDto | null | undefined,
  metric: string,
): string {
  const observation = currentMetric(context, metric)
  if (!observation) {
    return UNAVAILABLE_LABEL
  }
  const unit = observation.unit ? ` ${observation.unit}` : ''
  return `${observation.value}${unit}`
}

export function txPowerDisplay(context: CellContextDto | null | undefined): string {
  if (!context) {
    return UNAVAILABLE_LABEL
  }
  const txPower = findTxPower(context.radioConfiguration)
  if (!txPower?.parameterValue) {
    return UNAVAILABLE_LABEL
  }
  const unit = txPower.unit ? ` ${txPower.unit}` : ''
  return `${txPower.parameterValue}${unit}`
}

export function highestActiveSeverityForCell(
  cases: AssuranceCaseDto[] | null | undefined,
  cellId: string,
): string | null {
  if (!cases) {
    return null
  }
  let highest: string | null = null
  let rank = 0
  for (const item of activeCases(cases)) {
    if (item.affectedEntityId !== cellId) {
      continue
    }
    const next = severityRank(item.severity)
    if (next > rank) {
      rank = next
      highest = item.severity
    }
  }
  return highest
}

export function relatedAssuranceLabel(highestActiveSeverity: string | null): string {
  if (highestActiveSeverity === 'CRITICAL') {
    return 'Critical Assurance finding'
  }
  if (highestActiveSeverity === 'MAJOR') {
    return 'Major Assurance finding'
  }
  if (highestActiveSeverity === 'WARNING') {
    return 'Warning Assurance finding'
  }
  if (highestActiveSeverity === 'INFO') {
    return 'Info Assurance finding'
  }
  if (highestActiveSeverity) {
    return `${highestActiveSeverity.replaceAll('_', ' ')} Assurance finding`
  }
  return 'No active Assurance finding'
}

export function contextIsSynthetic(context: CellContextDto | null | undefined): boolean {
  if (!context) {
    return false
  }
  if (context.provenance.synthetic) {
    return true
  }
  return context.kpis.some((item) => item.synthetic) || context.telemetry.some((item) => item.current.synthetic)
}
