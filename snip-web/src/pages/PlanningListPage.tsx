import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { EmptyState } from '../components/EmptyState'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { ScenarioTruthBanner } from '../features/planning/ScenarioTruthBanner'
import { evaluationViewLabel } from '../features/planning/planningCopy'
import type { PlanningScenarioSummaryDto } from '../types/planning'

export function PlanningListPage() {
  const [scenarios, setScenarios] = useState<PlanningScenarioSummaryDto[] | null>(null)
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    snipApi.listPlanningScenarios().then(setScenarios).catch(setError)
  }

  useEffect(() => {
    load()
  }, [])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!scenarios) {
    return <LoadingState label="Loading what-if scenarios…" />
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>What-if configuration scenarios</h1>
          <p className="muted">Persistent independent-cell synthetic what-if workspace.</p>
        </div>
        <Link className="btn" to="/planning/new">
          Create what-if scenario
        </Link>
      </header>
      <ScenarioTruthBanner />
      {scenarios.length === 0 ? (
        <EmptyState title="No what-if scenarios yet" />
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Cells</th>
              <th scope="col">Alternatives</th>
              <th scope="col">Evaluation</th>
            </tr>
          </thead>
          <tbody>
            {scenarios.map((scenario) => (
              <tr key={scenario.id}>
                <td>
                  <Link to={`/planning/scenarios/${scenario.id}`}>{scenario.name}</Link>
                </td>
                <td>{scenario.cellCount}</td>
                <td>{scenario.alternativeCount}</td>
                <td>{evaluationViewLabel(scenario.evaluationView)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
