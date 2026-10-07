import { useEffect, useMemo, useState } from 'react'
import { snipApi } from '../../api/snipApi'
import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellContextDto, CellDto } from '../../types/network'
import { CellComparisonTable, type ComparisonRowModel } from './CellComparisonTable'
import { loadBoundedCellContexts, selectSiteComparisonCellIds } from './relatedCells'

export function SiteCellComparison({
  siteId,
  cells,
  cases,
  casesUnavailable,
}: {
  siteId: string
  cells: CellDto[]
  cases: AssuranceCaseDto[] | null
  casesUnavailable: boolean
}) {
  const selection = useMemo(() => selectSiteComparisonCellIds(siteId, cells), [cells, siteId])
  const selectionKey = selection.ids.join('|')
  const [contexts, setContexts] = useState<Record<string, CellContextDto>>({})
  const [failures, setFailures] = useState<Record<string, boolean>>({})
  const [loading, setLoading] = useState(selection.ids.length > 0)

  useEffect(() => {
    if (selection.ids.length === 0) {
      setContexts({})
      setFailures({})
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    loadBoundedCellContexts(selection.ids, (id) => snipApi.getCellContext(id)).then((result) => {
      if (cancelled) {
        return
      }
      setContexts(result.contexts)
      setFailures(result.failures)
      setLoading(false)
    })
    return () => {
      cancelled = true
    }
  }, [selection.ids, selectionKey])

  const byId = useMemo(() => new Map(cells.map((cell) => [cell.cellId, cell])), [cells])
  const rows: ComparisonRowModel[] = selection.ids.flatMap((id) => {
    const cell = byId.get(id)
    if (!cell) {
      return []
    }
    return [
      {
        cell,
        context: contexts[id] ?? null,
        failed: Boolean(failures[id]),
        pending: loading && !contexts[id] && !failures[id],
        relationships: [],
      },
    ]
  })

  return (
    <section className="panel" aria-labelledby="site-comparison-heading">
      <header className="panel-header">
        <h2 id="site-comparison-heading">Site cell comparison</h2>
        <p className="muted">
          Bounded comparison of cells at this site using canonical inventory and cell context. This is
          not site optimization, a site Digital Twin, or a site health score.
        </p>
      </header>
      {rows.length === 0 ? (
        <p className="muted">No cells at this site to compare.</p>
      ) : (
        <CellComparisonTable
          rows={rows}
          showRelationships={false}
          truncated={selection.truncated}
          cases={cases}
          casesUnavailable={casesUnavailable}
        />
      )}
    </section>
  )
}
