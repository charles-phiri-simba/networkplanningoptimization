export interface SiteDto {
  siteId: string
  name: string
  latitude: number | null
  longitude: number | null
  status: string
}

export interface GnbDto {
  gnbId: string
  name: string
  siteId: string
  vendor: string
  model: string
  status: string
}

export interface CellDto {
  cellId: string
  name: string
  gnbId: string
  siteId: string
  technology: string
  band: string
  arfcn: number | null
  pci: number | null
  bandwidthMhz: number | null
  duplexMode: string
  status: string
}

export interface RadioParameterDto {
  parameterName: string
  parameterValue: string
  unit: string | null
  effectiveFrom: string | null
}

export interface ContextProvenanceDto {
  source: string
  synthetic: boolean
}

export interface NeighbourDto {
  targetCellId: string
  relationType: string
  status: string
}

export interface CellContextDto {
  cell: CellDto
  gnb: GnbDto
  site: SiteDto
  radioConfiguration: RadioParameterDto[]
  kpis: KpiObservationDto[]
  neighbours: NeighbourDto[]
  telemetry: KpiSeriesDto[]
  provenance: ContextProvenanceDto
}

export interface KpiObservationDto {
  metric: string
  value: number | null
  unit: string | null
  observedAt: string | null
  eventTime: string | null
  ingestedAt: string | null
  eventId: string | null
  source: string | null
  synthetic: boolean
}

export interface KpiSeriesDto {
  metric: string
  current: KpiObservationDto
  history: KpiObservationDto[]
  trend: string
}
