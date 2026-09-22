import { useEffect, useState } from 'react'
import { snipApi } from '../api/snipApi'
import { AskSnip } from '../features/ai/AskSnip'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import type { CellDto } from '../types/network'

export function AiPage() {
  const [cells, setCells] = useState<CellDto[] | null>(null)
  const [cellId, setCellId] = useState('')
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    snipApi
      .listCells()
      .then((next) => {
        setCells(next)
        setCellId((current) => current || next[0]?.cellId || '')
      })
      .catch(setError)
  }

  useEffect(() => {
    load()
  }, [])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!cells) {
    return <LoadingState label="Loading cells…" />
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>AI explanation</h1>
          <p className="muted">
            Uses POST /api/v1/recommendations. Output is decision support, not an approved change
            and not a live-network action.
          </p>
        </div>
      </header>
      <label className="field">
        <span>Cell</span>
        <select value={cellId} onChange={(event) => setCellId(event.target.value)}>
          {cells.map((cell) => (
            <option key={cell.cellId} value={cell.cellId}>
              {cell.cellId} — {cell.name}
            </option>
          ))}
        </select>
      </label>
      {cellId ? <AskSnip key={cellId} cellId={cellId} /> : <p className="muted">No cells available.</p>}
    </div>
  )
}
