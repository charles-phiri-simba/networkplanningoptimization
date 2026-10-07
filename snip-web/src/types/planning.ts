import type { MetricComparisonDto } from './simulation'

export const MAX_CELLS_PER_SCENARIO = 4
export const MAX_ALTERNATIVES_PER_SCENARIO = 4
export const MAX_INTENTS_PER_ALTERNATIVE = 4
export const MAX_SIMULATION_RUNS_PER_EVALUATION = 16

export interface PlanningTruthDto {
  independentCellLocal: boolean
  jointSiteSimulation: boolean
  crossCellEffectsModelled: boolean
  synthetic: boolean
  modelId: string
  confidence: string
}

export interface PlanningScenarioCellDto {
  id: string
  cellId: string
  ordinal: number
}

export interface PlanningCellIntentDto {
  id: string
  cellId: string
  parameterId: string
  intendedValue: number
}

export interface PlanningAlternativeDto {
  id: string
  name: string
  ordinal: number
  intents: PlanningCellIntentDto[]
}

export interface PlanningScenarioSummaryDto {
  id: string
  name: string
  createdBy: string
  createdAt: string
  updatedAt: string
  rowVersion: number
  cellCount: number
  alternativeCount: number
  evaluationView: string
  truth: PlanningTruthDto
}

export interface PlanningScenarioDetailDto {
  id: string
  name: string
  description: string
  createdBy: string
  createdAt: string
  updatedAt: string
  rowVersion: number
  cells: PlanningScenarioCellDto[]
  alternatives: PlanningAlternativeDto[]
  evaluationView: string
  currentEvaluationId: string | null
  truth: PlanningTruthDto
}

export interface PlanningPrerequisiteDto {
  cellId: string
  twinPresent: boolean
  twinId: string | null
  latestVersion: number | null
  freshness: string
  observedTxPower: number | null
  canEvaluate: boolean
}

export interface PlanningEvaluationItemDto {
  id: string
  evaluationId: string
  alternativeId: string
  alternativeName: string
  alternativeOrdinal: number
  cellId: string
  parameterId: string
  intendedValue: number
  outcome: string
  failureCode: string | null
  failureMessage: string | null
  twinId: string | null
  twinVersion: number | null
  pinnedBaselineTxPower: number | null
  configurationFingerprint: string | null
  simulationScenarioId: string | null
  simulationRunId: string | null
  modelId: string | null
  modelVersion: string | null
  synthetic: boolean | null
  confidence: string | null
  reusedExistingRun: boolean
}

export interface PlanningEvaluationDto {
  id: string
  scenarioId: string
  status: string
  intentFingerprint: string
  admissionFingerprint: string | null
  createdBy: string
  createdAt: string
  completedAt: string | null
  itemCount: number
  succeededCount: number
  failedCount: number
  historical: boolean
  items: PlanningEvaluationItemDto[]
  truth: PlanningTruthDto
}

export interface PlanningComparisonRowDto {
  alternativeId: string
  alternativeName: string
  alternativeOrdinal: number
  cellId: string
  parameterId: string
  baselineTxPower: number | null
  intendedTxPower: number
  metrics: MetricComparisonDto[]
  confidence: string | null
  synthetic: boolean | null
  modelId: string | null
  modelVersion: string | null
  twinId: string | null
  twinVersion: number | null
  simulationRunId: string | null
  outcome: string
}

export interface PlanningComparisonDto {
  scenarioId: string
  evaluationId: string
  evaluationStatus: string
  intentFingerprint: string
  historical: boolean
  rows: PlanningComparisonRowDto[]
  truth: PlanningTruthDto
}

export interface CreatePlanningScenarioRequest {
  name: string
  description: string
  createdBy: string
  cells: { cellId: string }[]
  alternatives: {
    name: string
    ordinal: number
    intents: { cellId: string; parameterId: string; intendedValue: number }[]
  }[]
}

export interface ReplacePlanningScenarioRequest {
  name: string
  description: string
  rowVersion: number
  cells: { cellId: string }[]
  alternatives: {
    name: string
    ordinal: number
    intents: { cellId: string; parameterId: string; intendedValue: number }[]
  }[]
}
