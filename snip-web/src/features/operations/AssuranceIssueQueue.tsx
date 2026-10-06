import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { StatusBadge } from '../../components/StatusBadge'
import type { AssuranceCaseDto } from '../../types/assurance'
import type { CellDto, SiteDto } from '../../types/network'
import { formatNumber, formatTimestamp } from '../../utils/format'
import { issueMetricLabel, issueTypeLabel } from './issueLabels'
import {
  evidencePreview,
  filterIssues,
  sortIssues,
  type SeverityFilter,
  type SortKey,
  type StatusFilter,
} from './operationsModel'
import { siteLabelForCell } from './OperationalSummary'

export function AssuranceIssueQueue({
  cases,
  cells,
  sites,
  casesError,
  onRetryCases,
}: {
  cases: AssuranceCaseDto[] | null
  cells: CellDto[] | null
  sites: SiteDto[]
  casesError: unknown
  onRetryCases: () => void
}) {
  const [status, setStatus] = useState<StatusFilter>('ACTIVE')
  const [severity, setSeverity] = useState<SeverityFilter>('ALL')
  const [siteId, setSiteId] = useState('')
  const [sort, setSort] = useState<SortKey>('SEVERITY')

  const rows = useMemo(() => {
    if (!cases) {
      return []
    }
    return sortIssues(
      filterIssues(cases, status, severity, siteId || null, cells),
      sort,
    )
  }, [cases, cells, severity, siteId, sort, status])

  if (casesError) {
    return (
      <section className="panel" aria-labelledby="issue-queue-heading">
        <h2 id="issue-queue-heading">Active Assurance issues</h2>
        <ErrorState error={casesError} onRetry={onRetryCases} />
      </section>
    )
  }

  if (!cases) {
    return (
      <section className="panel" aria-labelledby="issue-queue-heading">
        <h2 id="issue-queue-heading">Active Assurance issues</h2>
        <p className="muted">Loading Assurance issues…</p>
      </section>
    )
  }

  return (
    <section className="panel" aria-labelledby="issue-queue-heading">
      <h2 id="issue-queue-heading">Active Assurance issues</h2>
      <div className="ops-filters">
        <label>
          Status
          <select
            aria-label="Issue status"
            value={status}
            onChange={(event) => setStatus(event.target.value as StatusFilter)}
          >
            <option value="ACTIVE">Active</option>
            <option value="OPEN">Open</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
          </select>
        </label>
        <label>
          Severity
          <select
            aria-label="Issue severity"
            value={severity}
            onChange={(event) => setSeverity(event.target.value as SeverityFilter)}
          >
            <option value="ALL">All</option>
            <option value="CRITICAL">Critical</option>
            <option value="MAJOR">Major</option>
            <option value="WARNING">Warning</option>
            <option value="INFO">Info</option>
          </select>
        </label>
        <label>
          Site
          <select
            aria-label="Issue site"
            value={siteId}
            onChange={(event) => setSiteId(event.target.value)}
            disabled={!cells}
          >
            <option value="">All sites</option>
            {sites.map((site) => (
              <option key={site.siteId} value={site.siteId}>
                {site.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Sort
          <select
            aria-label="Issue sort"
            value={sort}
            onChange={(event) => setSort(event.target.value as SortKey)}
          >
            <option value="SEVERITY">Severity, then last observed</option>
            <option value="LAST_OBSERVED">Last observed</option>
          </select>
        </label>
      </div>
      {rows.length === 0 ? (
        <EmptyState
          title="No active Assurance cases"
          detail={
            status !== 'ACTIVE' || severity !== 'ALL' || siteId
              ? 'No matching active Assurance findings for the current filters.'
              : 'There are currently no open or acknowledged Assurance cases.'
          }
        />
      ) : (
        <div className="table-wrap">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Issue</th>
                <th scope="col">Cell</th>
                <th scope="col">Site</th>
                <th scope="col">Severity</th>
                <th scope="col">Status</th>
                <th scope="col">Evidence</th>
                <th scope="col">First observed</th>
                <th scope="col">Last observed</th>
                <th scope="col" />
              </tr>
            </thead>
            <tbody>
              {rows.map((item) => {
                const preview = evidencePreview(item)
                return (
                  <tr key={item.id}>
                    <td>
                      <strong>{issueTypeLabel(item.caseType)}</strong>
                      <p className="muted">{item.caseType}</p>
                    </td>
                    <td>{item.affectedEntityId}</td>
                    <td>{siteLabelForCell(item.affectedEntityId, cells, sites)}</td>
                    <td>
                      <StatusBadge status={item.severity} kind="severity" />
                    </td>
                    <td>
                      <StatusBadge status={item.status} />
                    </td>
                    <td>
                      {preview ? (
                        <span>
                          {issueMetricLabel(preview.metric)}{' '}
                          {preview.value !== null ? formatNumber(preview.value) : '—'}
                          {preview.unit ? ` ${preview.unit}` : ''}
                          {preview.trend ? ` · ${preview.trend}` : ''}
                          {preview.description ? ` · ${preview.description}` : ''}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>{formatTimestamp(item.firstObservedAt)}</td>
                    <td>{formatTimestamp(item.lastObservedAt)}</td>
                    <td>
                      <Link to={`/assurance/${encodeURIComponent(item.id)}`}>Open case</Link>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
