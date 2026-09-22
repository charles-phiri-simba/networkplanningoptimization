import type { CitationDto } from './assurance'

export interface RecommendationRequest {
  question: string
  cellId?: string | null
  contextId?: string | null
}

export interface ContextUsedDto {
  id: string
  kpis: Record<string, unknown>
}

export interface ContextEvidenceDto {
  cellId: string
  gnbId: string
  siteId: string
  source: string
  synthetic: boolean
}

export interface RecommendationResponse {
  recommendation: string
  citations: CitationDto[]
  contextUsed: ContextUsedDto | null
  retrievalEmpty: boolean
  retrievalMode: string
  retrievalLatencyMs: number | null
  generationLatencyMs: number | null
  totalLatencyMs: number | null
  retrievalHitCount: number | null
  contextEvidence: ContextEvidenceDto | null
  contextResolutionLatencyMs: number | null
  contextCellId: string | null
  contextFound: boolean | null
  kpiObservationCount: number | null
  neighbourCount: number | null
  historyObservationCount: number | null
  lastEventTime: string | null
}
