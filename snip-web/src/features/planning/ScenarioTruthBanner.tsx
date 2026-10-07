import { PLANNING_TRUTH } from './planningCopy'

export function ScenarioTruthBanner() {
  return (
    <p className="banner-synthetic" role="note">
      <strong>What-if configuration scenario.</strong> {PLANNING_TRUTH}
    </p>
  )
}
