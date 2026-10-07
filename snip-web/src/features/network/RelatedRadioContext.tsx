import { useEffect, useMemo, useState } from 'react'
import { snipApi } from '../../api/snipApi'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellContextDto, CellDto } from '../../types/network'
import { CellComparisonTable, type ComparisonRowModel } from './CellComparisonTable'
import { loadBoundedCellContexts, relationshipsForCell, selectRelatedCellIds } from './relatedCells'

export function RelatedRadioContext({ selected }: { selected: CellContextDto }) {
  const [cells, setCells] = useState<CellDto[] | null>(null)
  const [cellsError, setCellsError] = useState<unknown>(null)
  const [cases, setCases] = useState<AssuranceCaseDto[] | null>(null)
  const [casesUnavailable, setCasesUnavailable] = useState(false)
  const [relatedContexts, setRelatedContexts] = useState<Record<string, CellContextDto>>({})
  const [relatedFailures, setRelatedFailures] = useState<Record<string, boolean>>({})
  const [relatedLoading, setRelatedLoading] = useState(false)

  function loadInventory() {
    setCellsError(null)
    snipApi
      .listCells()
      .then(setCells)
      .catch((error) => {
        setCells(null)
        setCellsError(error)
      })
  }

  useEffect(() => {
    loadInventory()
    snipApi
      .listAssuranceCases()
      .then((next) => {
        setCases(next)
        setCasesUnavailable(false)
      })
      .catch(() => {
        setCases([])
        setCasesUnavailable(true)
      })
  }, [selected.cell.cellId])

  const selection = useMemo(
    () =>
      cells
        ? selectRelatedCellIds(
            selected.cell.cellId,
            cells,
            selected.neighbours.map((neighbour) => neighbour.targetCellId),
          )
        : null,
    [cells, selected],
  )

  const extraIds = useMemo(
    () => (selection ? selection.ids.filter((id) => id !== selected.cell.cellId) : []),
    [selection, selected.cell.cellId],
  )
  const extraKey = extraIds.join('|')

  useEffect(() => {
    if (!cells) {
      return
    }
    if (extraIds.length === 0) {
      setRelatedContexts({})
      setRelatedFailures({})
      setRelatedLoading(false)
      return
    }
    let cancelled = false
    setRelatedLoading(true)
    loadBoundedCellContexts(extraIds, (id) => snipApi.getCellContext(id)).then((result) => {
      if (cancelled) {
        return
      }
      setRelatedContexts(result.contexts)
      setRelatedFailures(result.failures)
      setRelatedLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [cells, extraIds, extraKey])

  const outgoing = useMemo(
    () => new Set(selected.neighbours.map((neighbour) => neighbour.targetCellId)),
    [selected.neighbours],
  )

  const rows: ComparisonRowModel[] = useMemo(() => {
    if (!cells || !selection) {
      return []
    }
    const byId = new Map(cells.map((cell) => [cell.cellId, cell]))
    return selection.ids.flatMap((id) => {
      const cell = id === selected.cell.cellId ? selected.cell : byId.get(id)
      if (!cell) {
        return []
      }
      const extraPending =
        id !== selected.cell.cellId && relatedLoading && !relatedContexts[id] && !relatedFailures[id]
      return [
        {
          cell,
          context: id === selected.cell.cellId ? selected : relatedContexts[id] ?? null,
          failed: Boolean(relatedFailures[id]),
          pending: extraPending,
          relationships: relationshipsForCell(cell, selected.cell, outgoing),
        },
      ]
    })
  }, [cells, outgoing, relatedContexts, relatedFailures, relatedLoading, selected, selection])

  return (
    <section className="panel" aria-labelledby="related-radio-heading">
      <header className="panel-header">
        <h2 id="related-radio-heading">Related radio context</h2>
        <p className="muted">
          Engineering comparison of the selected cell, same-site cells, same-gNB cells, and configured
          outgoing neighbours. This is not coordinated multi-cell optimization, site-aware ranking, or
          simulated cross-cell effects.
        </p>
      </header>
      {cellsError ? (
        <ErrorState error={cellsError} onRetry={loadInventory} />
      ) : !cells ? (
        <LoadingState label="Loading related cells…" />
      ) : rows.length === 0 ? (
        <p className="muted">No related cells were identified.</p>
      ) : (
        <CellComparisonTable
          rows={rows}
          selectedCellId={selected.cell.cellId}
          showRelationships
          truncated={selection?.truncated}
          cases={cases}
          casesUnavailable={casesUnavailable}
        />
      )}
    </section>
  )
}
