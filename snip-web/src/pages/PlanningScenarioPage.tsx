import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useAuth } from '../features/auth/AuthContext'
import { EvaluationEvidencePanel } from '../features/planning/EvaluationEvidencePanel'
import { IntentEditor } from '../features/planning/IntentEditor'
import { PrerequisitePanel } from '../features/planning/PrerequisitePanel'
import { ScenarioComparisonTable } from '../features/planning/ScenarioComparisonTable'
import { ScenarioTruthBanner } from '../features/planning/ScenarioTruthBanner'
import { evaluationViewLabel } from '../features/planning/planningCopy'
import { optimizeHref } from '../features/planning/planningGuards'
import type {
  PlanningComparisonDto,
  PlanningEvaluationDto,
  PlanningPrerequisiteDto,
  PlanningScenarioDetailDto,
} from '../types/planning'

export function PlanningScenarioPage() {
  const { scenarioId = '' } = useParams()
  const { identity } = useAuth()
  const [scenario, setScenario] = useState<PlanningScenarioDetailDto | null>(null)
  const [prereqs, setPrereqs] = useState<PlanningPrerequisiteDto[] | null>(null)
  const [evaluation, setEvaluation] = useState<PlanningEvaluationDto | null>(null)
  const [comparison, setComparison] = useState<PlanningComparisonDto | null>(null)
  const [comparisonError, setComparisonError] = useState<string | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [busyCell, setBusyCell] = useState<string | null>(null)
  const [evaluating, setEvaluating] = useState(false)
  const [handoffCell, setHandoffCell] = useState<string>('')

  function load() {
    setError(null)
    Promise.all([
      snipApi.getPlanningScenario(scenarioId),
      snipApi.getPlanningPrerequisites(scenarioId),
    ])
      .then(([nextScenario, nextPrereqs]) => {
        setScenario(nextScenario)
        setPrereqs(nextPrereqs.cells)
        setHandoffCell((current) => current || nextScenario.cells[0]?.cellId || '')
        if (nextScenario.currentEvaluationId && nextScenario.evaluationView !== 'NOT_EVALUATED') {
          return Promise.all([
            snipApi.getPlanningEvaluation(scenarioId, nextScenario.currentEvaluationId),
            nextScenario.evaluationView === 'STALE'
              ? Promise.resolve(null)
              : snipApi.getPlanningComparison(scenarioId).then((value) => {
                  setComparisonError(null)
                  return value
                }).catch((cause: unknown) => {
                  setComparisonError(cause instanceof Error ? cause.message : 'Comparison could not be loaded.')
                  return null
                }),
          ]).then(([nextEval, nextCompare]) => {
            setEvaluation(nextEval)
            setComparison(nextCompare)
          })
        }
        setEvaluation(null)
        setComparison(null)
        setComparisonError(null)
        return undefined
      })
      .catch(setError)
  }

  useEffect(() => {
    load()
  }, [scenarioId])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!scenario || !prereqs) {
    return <LoadingState label="Loading scenario…" />
  }

  function evaluate() {
    if (!scenario || evaluating) {
      return
    }
    setEvaluating(true)
    void snipApi
      .evaluatePlanningScenario(scenario.id, identity?.actorId ?? 'demo-network-engineer', scenario.rowVersion)
      .then(() => load())
      .catch(setError)
      .finally(() => setEvaluating(false))
  }

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/planning">Planning</Link> / {scenario.name}
      </p>
      <header className="page-header">
        <div>
          <h1>{scenario.name}</h1>
          <p className="muted">{evaluationViewLabel(scenario.evaluationView)}</p>
        </div>
      </header>
      <ScenarioTruthBanner />
      <p className="muted" role="note">
        Select up to 4 cells. Featured story uses CELL-001 and CELL-002. Each alternative is an
        independent cell-local txPower what-if (20–50 dBm). Evaluate requires a CURRENT cell Digital
        Twin. Demo startup synchronizes featured cells; Synchronize remains available. Evaluate runs
        the synthetic cell-parameter model, not joint RF. Optimize handoff opens Optimize for one
        cell and does not generate a proposal from Planning.
      </p>
      <section className="panel">
        <h2>Alternatives and txPower intents</h2>
        {scenario.alternatives.map((alt) => (
          <article key={alt.id}>
            <h3>
              {alt.ordinal}. {alt.name}
            </h3>
            {alt.intents.map((intent) => (
              <IntentEditor
                key={intent.id}
                cellId={intent.cellId}
                value={intent.intendedValue}
                onChange={(value) => {
                  const nextAlts = scenario.alternatives.map((item) =>
                    item.id === alt.id
                      ? {
                          ...item,
                          intents: item.intents.map((row) =>
                            row.id === intent.id ? { ...row, intendedValue: value } : row,
                          ),
                        }
                      : item,
                  )
                  void snipApi
                    .replacePlanningScenario(scenario.id, {
                      name: scenario.name,
                      description: scenario.description,
                      rowVersion: scenario.rowVersion,
                      cells: scenario.cells.map((cell) => ({ cellId: cell.cellId })),
                      alternatives: nextAlts.map((item) => ({
                        name: item.name,
                        ordinal: item.ordinal,
                        intents: item.intents.map((row) => ({
                          cellId: row.cellId,
                          parameterId: row.parameterId,
                          intendedValue: row.intendedValue,
                        })),
                      })),
                    })
                    .then(load)
                    .catch(setError)
                }}
              />
            ))}
          </article>
        ))}
      </section>
      <PrerequisitePanel
        cells={prereqs}
        busyCell={busyCell}
        onBusy={setBusyCell}
        onSynchronized={() => {
          setBusyCell(null)
          load()
        }}
      />
      <p>
        <button type="button" className="btn" disabled={evaluating || scenario.evaluationView === 'EVALUATING'} onClick={evaluate}>
          {evaluating ? 'Evaluating…' : 'Evaluate'}
        </button>
      </p>
      {evaluation ? <EvaluationEvidencePanel evaluation={evaluation} /> : null}
      {comparisonError ? (
        <p role="alert">
          Planning comparison could not be loaded. Retry Evaluate or refresh this page. {comparisonError}
        </p>
      ) : null}
      {comparison ? <ScenarioComparisonTable comparison={comparison} /> : null}
      <section className="panel" aria-labelledby="handoff-heading">
        <h2 id="handoff-heading">Optimize handoff</h2>
        <p className="muted">Select exactly one cell. This does not generate a proposal.</p>
        <label>
          Cell
          <select value={handoffCell} onChange={(event) => setHandoffCell(event.target.value)}>
            {scenario.cells.map((cell) => (
              <option key={cell.cellId} value={cell.cellId}>
                {cell.cellId}
              </option>
            ))}
          </select>
        </label>
        {handoffCell ? (
          <p>
            <Link className="btn" to={optimizeHref(handoffCell)}>
              Open Optimize for this cell
            </Link>
          </p>
        ) : null}
      </section>
    </div>
  )
}
