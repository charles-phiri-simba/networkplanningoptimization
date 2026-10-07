import {
  MAX_ALTERNATIVES_PER_SCENARIO,
  MAX_CELLS_PER_SCENARIO,
  MAX_INTENTS_PER_ALTERNATIVE,
  MAX_SIMULATION_RUNS_PER_EVALUATION,
} from '../../types/planning'

export const TX_POWER_MIN = 20
export const TX_POWER_MAX = 50

export function suggestedPlanningCells(cellIds: string[]): string[] {
  const unique: string[] = []
  for (const cellId of cellIds) {
    if (!unique.includes(cellId) && unique.length < MAX_CELLS_PER_SCENARIO) {
      unique.push(cellId)
    }
  }
  return unique
}

export function optimizeHref(cellId: string): string {
  return `/network/cells/${encodeURIComponent(cellId)}/optimize`
}

export function planningCreateHref(cellIds: string[]): string {
  const selected = suggestedPlanningCells(cellIds)
  const query = selected.length ? `?cells=${selected.map(encodeURIComponent).join(',')}` : ''
  return `/planning/new${query}`
}

export function validateTxPower(value: number): string | null {
  if (!Number.isFinite(value)) {
    return 'txPower is required'
  }
  if (value < TX_POWER_MIN || value > TX_POWER_MAX) {
    return `txPower must be between ${TX_POWER_MIN} and ${TX_POWER_MAX} dBm`
  }
  return null
}

export function validatePlanningDraft(input: {
  cells: string[]
  alternativeCount: number
  intentCount: number
  maxIntentsInOneAlternative: number
}): string | null {
  if (input.cells.length === 0) {
    return 'Select at least one cell'
  }
  if (input.cells.length > MAX_CELLS_PER_SCENARIO) {
    return `At most ${MAX_CELLS_PER_SCENARIO} cells`
  }
  if (input.alternativeCount < 1) {
    return 'Add at least one alternative'
  }
  if (input.alternativeCount > MAX_ALTERNATIVES_PER_SCENARIO) {
    return `At most ${MAX_ALTERNATIVES_PER_SCENARIO} alternatives`
  }
  if (input.maxIntentsInOneAlternative > MAX_INTENTS_PER_ALTERNATIVE) {
    return `At most ${MAX_INTENTS_PER_ALTERNATIVE} intents per alternative`
  }
  if (input.intentCount > MAX_SIMULATION_RUNS_PER_EVALUATION) {
    return `At most ${MAX_SIMULATION_RUNS_PER_EVALUATION} intents in one scenario`
  }
  if (input.intentCount === 0) {
    return 'Add at least one txPower intent'
  }
  return null
}
