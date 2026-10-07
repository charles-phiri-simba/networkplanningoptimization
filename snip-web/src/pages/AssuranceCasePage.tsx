import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import { issueTypeLabel } from '../features/operations/issueLabels'
import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import { formatNumber, formatTimestamp } from '../utils/format'

interface Partition<T> {
  data: T | null
  error: unknown
  settled: boolean
}

function idlePartition<T>(): Partition<T> {
  return { data: null, error: null, settled: false }
}

export function AssuranceCasePage() {
  const { caseId = '' } = useParams()
  const [casePart, setCasePart] = useState<Partition<AssuranceCaseDto>>(idlePartition)
  const [analysis, setAnalysis] = useState<Partition<DecisionAssessmentDto>>(idlePartition)

  function loadCase() {
    setCasePart(idlePartition())
    snipApi
      .getAssuranceCase(caseId)
      .then((data) => setCasePart({ data, error: null, settled: true }))
      .catch((error) => setCasePart({ data: null, error, settled: true }))
  }

  function loadAnalysis() {
    setAnalysis(idlePartition())
    snipApi
      .getAssuranceAssessment(caseId)
      .then((data) => setAnalysis({ data, error: null, settled: true }))
      .catch((error) => setAnalysis({ data: null, error, settled: true }))
  }

  useEffect(() => {
    loadCase()
    loadAnalysis()
  }, [caseId])

  if (casePart.error && !casePart.data) {
    return <ErrorState error={casePart.error} onRetry={loadCase} />
  }
  if (!casePart.data) {
    return <LoadingState label="Loading assurance case…" />
  }

  const item = casePart.data
  const cellLink =
    item.affectedEntityType === 'CELL'
      ? `/network/cells/${encodeURIComponent(item.affectedEntityId)}`
      : null

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/assurance">Assurance</Link> / {item.id}
      </p>

      <section className="panel" aria-labelledby="finding-heading">
        <p className="eyebrow">Finding</p>
        <header className="page-header">
          <div>
            <h1 id="finding-heading">{issueTypeLabel(item.caseType)}</h1>
            <p className="muted">
              {item.affectedEntityType} {item.affectedEntityId}
              {cellLink ? (
                <>
                  {' '}
                  · <Link to={cellLink}>Open cell</Link>
                </>
              ) : null}
            </p>
            <p className="muted">{item.caseType}</p>
          </div>
          <div className="badge-row">
            <StatusBadge status={item.severity} kind="severity" />
            <StatusBadge status={item.status} />
          </div>
        </header>
        {item.synthetic ? (
          <p className="banner-demo">Synthetic demo observation — not live network data.</p>
        ) : null}
        <dl className="kv">
          <div>
            <dt>Case ID</dt>
            <dd>{item.id}</dd>
          </div>
          <div>
            <dt>Confidence</dt>
            <dd>{item.confidence}</dd>
          </div>
          <div>
            <dt>Rule</dt>
            <dd>{item.ruleId}</dd>
          </div>
          <div>
            <dt>Detected</dt>
            <dd>{formatTimestamp(item.detectedAt)}</dd>
          </div>
          <div>
            <dt>First observed</dt>
            <dd>{formatTimestamp(item.firstObservedAt)}</dd>
          </div>
          <div>
            <dt>Last observed</dt>
            <dd>{formatTimestamp(item.lastObservedAt)}</dd>
          </div>
        </dl>
      </section>

      <section className="panel" aria-labelledby="observed-evidence-heading">
        <h2 id="observed-evidence-heading">Observed evidence</h2>
        <p className="muted">Authoritative operational observations for this finding. This is not interpretation.</p>
        {item.evidence.length === 0 ? (
          <p className="muted">No observed evidence rows.</p>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">Type</th>
                  <th scope="col">Metric</th>
                  <th scope="col">Value</th>
                  <th scope="col">Trend</th>
                  <th scope="col">Observed</th>
                  <th scope="col">Source</th>
                  <th scope="col">Description</th>
                </tr>
              </thead>
              <tbody>
                {item.evidence.map((evidence) => (
                  <tr key={evidence.id}>
                    <td>{evidence.evidenceType}</td>
                    <td>{evidence.metric ?? '—'}</td>
                    <td>
                      {formatNumber(evidence.value)} {evidence.unit ?? ''}
                    </td>
                    <td>{evidence.trend ?? '—'}</td>
                    <td>{formatTimestamp(evidence.observedAt)}</td>
                    <td>
                      {evidence.source ?? '—'}
                      {evidence.synthetic ? (
                        <p className="muted">Synthetic demo observation — not live network data.</p>
                      ) : null}
                    </td>
                    <td>{evidence.description ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <AnalysisSections analysis={analysis} onRetry={loadAnalysis} />

      <section className="panel" aria-labelledby="investigation-actions-heading">
        <h2 id="investigation-actions-heading">Investigation actions</h2>
        {cellLink ? (
          <p>
            <Link className="btn" to={cellLink}>
              Open cell
            </Link>
          </p>
        ) : (
          <p className="muted">No cell navigation is available for this affected entity type.</p>
        )}
        <p className="muted">
          Continue on the cell workspace. Governed optimization remains the existing Optimize workflow.
        </p>
      </section>
    </div>
  )
}

function AnalysisSections({
  analysis,
  onRetry,
}: {
  analysis: Partition<DecisionAssessmentDto>
  onRetry: () => void
}) {
  if (!analysis.settled) {
    return (
      <section className="panel" aria-labelledby="snip-analysis-heading">
        <h2 id="snip-analysis-heading">SNIP analysis</h2>
        <p className="muted">Retrieved knowledge and investigation guidance</p>
        <LoadingState label="Loading SNIP analysis…" />
      </section>
    )
  }

  if (analysis.error || !analysis.data) {
    return (
      <section className="panel" aria-labelledby="snip-analysis-heading">
        <h2 id="snip-analysis-heading">SNIP analysis</h2>
        <p className="muted">Retrieved knowledge and investigation guidance</p>
        <p role="status">Analysis unavailable</p>
        <ErrorState error={analysis.error ?? new Error('Assessment unavailable')} onRetry={onRetry} />
      </section>
    )
  }

  const assessment = analysis.data
  return (
    <>
      <section className="panel" aria-labelledby="snip-analysis-heading">
        <h2 id="snip-analysis-heading">SNIP analysis</h2>
        <p className="muted">Retrieved knowledge and investigation guidance</p>
        <p>{assessment.summary}</p>
        {assessment.humanReviewRequired ? (
          <p>
            <strong>Human engineering review required.</strong>
          </p>
        ) : null}
        {assessment.retrievalEmpty ? (
          <p role="status">
            No supporting knowledge source was retrieved for this assessment. Observed evidence remains
            valid.
          </p>
        ) : null}
        <p className="muted">
          Urgency {assessment.urgency}
          {assessment.retrievalMode ? ` · retrieval ${assessment.retrievalMode}` : ''}
        </p>
      </section>

      <section className="panel" aria-labelledby="contributors-heading">
        <h2 id="contributors-heading">Likely contributors to investigate</h2>
        <p className="muted">
          Rule-based investigation guidance. These are investigation hypotheses for human engineering
          review.
        </p>
        <GuidanceList
          items={assessment.likelyContributors}
          empty="No likely contributors were identified."
        />
      </section>

      <section className="panel" aria-labelledby="checks-heading">
        <h2 id="checks-heading">Recommended engineering checks</h2>
        <p className="muted">These are investigation prompts, not automatic network actions.</p>
        <GuidanceList
          items={assessment.recommendedChecks}
          empty="No recommended engineering checks were identified."
        />
      </section>

      <section className="panel" aria-labelledby="missing-evidence-heading">
        <h2 id="missing-evidence-heading">Missing evidence</h2>
        <p className="muted">Information gaps reported by investigation guidance. These are not fabricated facts.</p>
        <GuidanceList
          items={assessment.missingEvidence}
          empty="No additional missing evidence was identified."
        />
      </section>

      <section className="panel" aria-labelledby="knowledge-sources-heading">
        <h2 id="knowledge-sources-heading">Knowledge sources</h2>
        <p className="muted">Retrieved sources supplied by the knowledge retriever. These are not vendor or 3GPP standards.</p>
        {assessment.citations.length === 0 ? (
          <p className="muted">No retrieved sources.</p>
        ) : (
          <ul>
            {assessment.citations.map((citation, index) => (
              <li key={`${citation.sourceId}-${citation.chunkId ?? index}`}>
                <strong>{citation.sourceId}</strong>
                {citation.locator ? <span className="muted"> · {citation.locator}</span> : null}
                {citation.score !== null ? <span className="muted"> · score {citation.score}</span> : null}
                {citation.snippet ? <div className="muted">{citation.snippet}</div> : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  )
}

function GuidanceList({ items, empty }: { items: string[]; empty: string }) {
  if (items.length === 0) {
    return <p className="muted">{empty}</p>
  }
  return (
    <ul>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  )
}
