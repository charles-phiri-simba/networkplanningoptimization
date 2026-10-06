import type { SynchronizationSourceSummaryDto } from '../../types/sync'

export function firstEnabledSource(
  sources: SynchronizationSourceSummaryDto[],
): SynchronizationSourceSummaryDto | null {
  return sources.find((source) => source.enabled) ?? null
}
