import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { snipApi } from '../api/snipApi'
import { ErrorState } from '../components/ErrorState'
import { LoadingState } from '../components/LoadingState'
import { StatusBadge } from '../components/StatusBadge'
import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import { formatNumber, formatTimestamp } from '../utils/format'

export function AssuranceCasePage() {
  const { caseId = '' } = useParams()
  const [item, setItem] = useState<AssuranceCaseDto | null>(null)
  const [assessment, setAssessment] = useState<DecisionAssessmentDto | null>(null)
  const [error, setError] = useState<unknown>(null)

  function load() {
    setError(null)
    Promise.all([snipApi.getAssuranceCase(caseId), snipApi.getAssuranceAssessment(caseId)])
      .then(([nextCase, nextAssessment]) => {
        setItem(nextCase)
        setAssessment(nextAssessment)
      })
      .catch(setError)
  }

  useEffect(() => {
    load()
  }, [caseId])

  if (error) {
    return <ErrorState error={error} onRetry={load} />
  }
  if (!item || !assessment) {
    return <LoadingState label="Loading assurance case…" />
  }

  const cellLink =
    item.affectedEntityType === 'CELL'
      ? `/network/cells/${encodeURIComponent(item.affectedEntityId)}`
      : null

  return (
    <div className="page">
      <p className="crumb">
        <Link to="/assurance">Assurance</Link> / {item.id}
      </p>
      <header className="page-header">
        <div>
          <h1>{item.caseType}</h1>
          <p className="muted">
            {item.affectedEntityType} {item.affectedEntityId}
            {cellLink ? (
              <>
                {' '}
                · <Link to={cellLink}>Open cell</Link>
              </>
            ) : null}
          </p>
        </div>
        <div className="badge-row">
          <StatusBadge status={item.severity} kind="severity" />
          <StatusBadge status={item.status} />
        </div>
      </header>
      {item.synthetic ? <p className="banner-demo">This case is marked synthetic / demo.</p> : null}

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

      <section className="panel">
        <h2>Evidence</h2>
        {item.evidence.length === 0 ? (
          <p className="muted">No evidence rows.</p>
        ) : (
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Type</th>
                <th scope="col">Metric</th>
                <th scope="col">Value</th>
                <th scope="col">Observed</th>
                <th scope="col">Source</th>
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
                  <td>{formatTimestamp(evidence.observedAt)}</td>
                  <td>
                    {evidence.source ?? '—'}
                    {evidence.synthetic ? ' · synthetic' : ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      <section className="panel">
        <h2>Assessment</h2>
        <p>{assessment.summary}</p>
        <p className="muted">
          Urgency {assessment.urgency}
          {assessment.humanReviewRequired ? ' · human review required' : ''}
        </p>
        <h3>Likely contributors</h3>
        <List items={assessment.likelyContributors} />
        <h3>Recommended checks</h3>
        <p className="muted">These are investigation prompts, not automatic network actions.</p>
        <List items={assessment.recommendedChecks} />
        <h3>Citations</h3>
        {assessment.citations.length === 0 ? (
          <p className="muted">No citations.</p>
        ) : (
          <ul>
            {assessment.citations.map((citation, index) => (
              <li key={`${citation.sourceId}-${index}`}>
                {citation.sourceId}
                {citation.snippet ? <div className="muted">{citation.snippet}</div> : null}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

function List({ items }: { items: string[] }) {
  if (items.length === 0) {
    return <p className="muted">None returned.</p>
  }
  return (
    <ul>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  )
}
