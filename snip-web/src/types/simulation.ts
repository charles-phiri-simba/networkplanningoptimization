export interface MetricComparisonDto {
  metric: string
  baselineValue: number
  candidateValue: number
  delta: number
  unit: string | null
}

export interface TwinProvenanceDto {
  source: string | null
  sourceCellId: string | null
  sourceContextVersion: string | null
  sourceTelemetryTimestamp: string | null
  capturedAt: string | null
  synthetic: boolean
}

export interface SimulationDetailDto {
  id: string
  scenarioId: string
  twinId: string
  baselineTwinVersion: number
  modelId: string | null
  modelVersion: string | null
  modelType: string | null
  status: string
  startedAt: string | null
  completedAt: string | null
  synthetic: boolean
  confidence: string | null
  assumptions: string[]
  limitations: string[]
  metrics: MetricComparisonDto[]
  provenance: TwinProvenanceDto | null
  actionId: string | null
}
