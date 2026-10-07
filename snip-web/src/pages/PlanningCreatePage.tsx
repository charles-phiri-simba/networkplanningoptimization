import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { useAuth } from '../features/auth/AuthContext'
import { AlternativeEditor } from '../features/planning/AlternativeEditor'
import { IntentEditor } from '../features/planning/IntentEditor'
import { ScenarioTruthBanner } from '../features/planning/ScenarioTruthBanner'
import { suggestedPlanningCells, validatePlanningDraft, validateTxPower } from '../features/planning/planningGuards'
import { MAX_ALTERNATIVES_PER_SCENARIO, MAX_CELLS_PER_SCENARIO } from '../types/planning'
import type { CellDto } from '../types/network'

interface DraftAlternative {
  name: string
  intents: Record<string, number>
}

export function PlanningCreatePage() {
  const { identity } = useAuth()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const suggested = suggestedPlanningCells((params.get('cells') ?? '').split(',').filter(Boolean))
  const [cells, setCells] = useState<CellDto[] | null>(null)
  const [error, setError] = useState<unknown>(null)
  const [selected, setSelected] = useState<string[]>(suggested)
  const [name, setName] = useState('What-if scenario')
  const [alternatives, setAlternatives] = useState<DraftAlternative[]>([
    { name: 'Alternative A', intents: Object.fromEntries(suggested.map((id) => [id, 44])) },
  ])
  const [submitError, setSubmitError] = useState<string | null>(null)

  useEffect(() => {
    snipApi.listCells().then(setCells).catch(setError)
  }, [])

  const selectedSet = useMemo(() => new Set(selected), [selected])

  if (error) {
    return <ErrorState error={error} />
  }
  if (!cells) {
    return <LoadingState label="Loading cells…" />
  }

  function toggle(cellId: string) {
    setSelected((current) => {
      if (current.includes(cellId)) {
        setAlternatives((alts) =>
          alts.map((alt) => {
            const { [cellId]: _removed, ...intents } = alt.intents
            return { ...alt, intents }
          }),
        )
        return current.filter((id) => id !== cellId)
      }
      if (current.length >= MAX_CELLS_PER_SCENARIO) {
        return current
      }
      setAlternatives((alts) =>
        alts.map((alt) => ({ ...alt, intents: { ...alt.intents, [cellId]: alt.intents[cellId] ?? 44 } })),
      )
      return [...current, cellId]
    })
  }

  function submit() {
    const draftError = validatePlanningDraft({
      cells: selected,
      alternativeCount: alternatives.length,
      intentCount: alternatives.reduce((sum, alt) => sum + selected.filter((id) => alt.intents[id] != null).length, 0),
      maxIntentsInOneAlternative: Math.max(0, ...alternatives.map((alt) => selected.filter((id) => alt.intents[id] != null).length)),
    })
    if (draftError) {
      setSubmitError(draftError)
      return
    }
    for (const alt of alternatives) {
      for (const cellId of selected) {
        const value = alt.intents[cellId]
        if (value == null) {
          continue
        }
        const txError = validateTxPower(value)
        if (txError) {
          setSubmitError(txError)
          return
        }
      }
    }
    setSubmitError(null)
    void snipApi
      .createPlanningScenario({
        name,
        description: '',
        createdBy: identity?.actorId ?? 'demo-network-engineer',
        cells: selected.map((cellId) => ({ cellId })),
        alternatives: alternatives.map((alt, index) => ({
          name: alt.name,
          ordinal: index + 1,
          intents: selected.map((cellId) => ({
              cellId,
              parameterId: 'txPower',
              intendedValue: alt.intents[cellId] ?? 44,
            })),
        })),
      })
      .then((created) => navigate(`/planning/scenarios/${created.id}`))
      .catch((cause: unknown) => setSubmitError(cause instanceof Error ? cause.message : 'Create failed'))
  }

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/planning">Planning</Link> / New
      </p>
      <header className="page-header">
        <div>
          <h1>Create what-if scenario</h1>
          <p className="muted">Review the bounded cell selection before creating. Suggested cells are not automatic membership.</p>
        </div>
      </header>
      <ScenarioTruthBanner />
      <label>
        Scenario name
        <input value={name} onChange={(event) => setName(event.target.value)} />
      </label>
      <section className="panel" aria-labelledby="cell-select-heading">
        <h2 id="cell-select-heading">Cells (max {MAX_CELLS_PER_SCENARIO})</h2>
        {cells.map((cell) => (
          <label key={cell.cellId}>
            <input
              type="checkbox"
              checked={selectedSet.has(cell.cellId)}
              onChange={() => toggle(cell.cellId)}
            />{' '}
            {cell.cellId} {cell.name}
          </label>
        ))}
      </section>
      {alternatives.map((alt, index) => (
        <section key={index} className="panel">
          <AlternativeEditor
            name={alt.name}
            ordinal={index + 1}
            canRemove={alternatives.length > 1}
            onRename={(next) =>
              setAlternatives((current) => current.map((item, i) => (i === index ? { ...item, name: next } : item)))
            }
            onRemove={() => setAlternatives((current) => current.filter((_, i) => i !== index))}
          />
          {selected.map((cellId) => (
            <IntentEditor
              key={cellId}
              cellId={cellId}
              value={alt.intents[cellId] ?? 44}
              onChange={(value) =>
                setAlternatives((current) =>
                  current.map((item, i) =>
                    i === index ? { ...item, intents: { ...item.intents, [cellId]: value } } : item,
                  ),
                )
              }
            />
          ))}
        </section>
      ))}
      {alternatives.length < MAX_ALTERNATIVES_PER_SCENARIO ? (
        <button
          type="button"
          className="btn btn-quiet"
          onClick={() =>
            setAlternatives((current) => [
              ...current,
              { name: `Alternative ${String.fromCharCode(65 + current.length)}`, intents: Object.fromEntries(selected.map((id) => [id, 44])) },
            ])
          }
        >
          Add alternative
        </button>
      ) : null}
      {submitError ? <p role="alert">{submitError}</p> : null}
      <p>
        <button type="button" className="btn" onClick={submit}>
          Create scenario
        </button>
      </p>
    </div>
  )
}
