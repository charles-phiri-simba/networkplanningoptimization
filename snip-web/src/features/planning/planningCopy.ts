export const PLANNING_TRUTH = [
  'Independent cell-local synthetic evaluation.',
  'Not a joint site RF simulation.',
  'Neighbour coupling is not modelled.',
  'Interference is not modelled.',
  'Handover is not modelled.',
  'Load redistribution is not modelled.',
  'Coverage propagation is not modelled.',
  'Synthetic model confidence is LOW.',
].join(' ')

export const FORBIDDEN_CAPABILITY_PHRASES = [
  'site simulation',
  'multi-cell rf simulation',
  'joint simulation',
  'interference-aware',
  'handover-aware',
  'coverage prediction',
  'best site configuration',
  'site optimum',
  'network optimum',
  'site health score',
  'site benefit score',
] as const

export function containsForbiddenCapabilityClaim(text: string): boolean {
  const lower = text.toLowerCase()
  return FORBIDDEN_CAPABILITY_PHRASES.some((phrase) => lower.includes(phrase))
}

export function evaluationViewLabel(view: string): string {
  switch (view) {
    case 'EVALUATING':
      return 'Evaluating'
    case 'EVALUATED':
      return 'Evaluated'
    case 'PARTIAL':
      return 'Partially evaluated'
    case 'FAILED':
      return 'Evaluation failed'
    case 'STALE':
      return 'Evaluation stale — intents changed'
    default:
      return 'Not evaluated'
  }
}
