export interface AssuranceEvidenceDto {
  id: string
  evidenceType: string
  metric: string | null
  value: number | null
  unit: string | null
  trend: string | null
  observedAt: string | null
  source: string | null
  synthetic: boolean
  description: string | null
}

export interface AssuranceCaseDto {
  id: string
  caseType: string
  affectedEntityType: string
  affectedEntityId: string
  severity: string
  confidence: string
  status: string
  detectedAt: string | null
  firstObservedAt: string | null
  lastObservedAt: string | null
  ruleId: string
  synthetic: boolean
  evidence: AssuranceEvidenceDto[]
}

export interface CitationDto {
  sourceId: string
  locator: string | null
  snippet: string | null
  chunkId: string | null
  score: number | null
}

export interface DecisionAssessmentDto {
  assuranceCaseId: string
  summary: string
  likelyContributors: string[]
  recommendedChecks: string[]
  missingEvidence: string[]
  urgency: string
  humanReviewRequired: boolean
  severity: string
  confidence: string
  caseType: string
  status: string
  operationalEvidence: AssuranceEvidenceDto[]
  citations: CitationDto[]
  retrievalEmpty: boolean
  retrievalMode: string
  retrievalLatencyMs: number
  generationLatencyMs: number
  totalLatencyMs: number
}
