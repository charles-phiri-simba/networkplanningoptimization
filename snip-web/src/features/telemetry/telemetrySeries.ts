import type { KpiObservationDto, KpiSeriesDto } from '../../types/network'

export function telemetryChartPoints(series: KpiSeriesDto): KpiObservationDto[] {
  return series.history.length > 0 ? series.history : [series.current]
}
