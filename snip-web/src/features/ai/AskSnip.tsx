import { useEffect, useState } from 'react'
import { ApiError } from '../../api/client'
import { snipApi } from '../../api/snipApi'
import type { RecommendationResponse } from '../../types/recommendation'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'

const PRESETS = [
  'Why is this cell unhealthy?',
  'Explain the recent KPI behaviour.',
  'What evidence is available for this cell?',
  'What configuration should an engineer investigate?',
]

export function AskSnip({ cellId }: { cellId: string }) {
  const [question, setQuestion] = useState(PRESETS[0])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>(null)
  const [response, setResponse] = useState<RecommendationResponse | null>(null)

  useEffect(() => {
    setResponse(null)
    setError(null)
  }, [cellId])

  async function submit() {
    setBusy(true)
    setError(null)
    try {
      const result = await snipApi.recommend({ question, cellId })
      setResponse(result)
    } catch (err) {
      setError(err instanceof ApiError || err instanceof Error ? err : new Error('Unexpected error'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="panel" aria-labelledby="ask-snip-heading">
      <header className="panel-header">
        <h2 id="ask-snip-heading">Ask SNIP</h2>
        <p className="muted">
          Decision support only. Review evidence. This explanation does not execute network changes
          and is not an authorised optimisation recommendation.
        </p>
      </header>
      <div className="preset-row" role="group" aria-label="Preset questions">
        {PRESETS.map((preset) => (
          <button
            key={preset}
            type="button"
            className={preset === question ? 'chip chip-active' : 'chip'}
            onClick={() => setQuestion(preset)}
          >
            {preset}
          </button>
        ))}
      </div>
      <label className="field">
        <span>Question</span>
        <textarea
          value={question}
          onChange={(event) => setQuestion(event.target.value)}
          rows={3}
        />
      </label>
      <button type="button" className="btn btn-primary" onClick={() => void submit()} disabled={busy || !question.trim()}>
        Explain this cell
      </button>
      {busy ? <LoadingState label="Requesting explanation…" /> : null}
      {error ? <ErrorState error={error} onRetry={() => void submit()} /> : null}
      {response ? <RecommendationView response={response} /> : null}
    </section>
  )
}

function RecommendationView({ response }: { response: RecommendationResponse }) {
  return (
    <div className="recommendation">
      <p className="eyebrow">AI explanation</p>
      <p>{response.recommendation}</p>
      {response.contextEvidence ? (
        <p className="muted">
          Context {response.contextEvidence.cellId} · {response.contextEvidence.source}
          {response.contextEvidence.synthetic ? ' · synthetic/demo' : ''}
          {response.retrievalEmpty ? ' · retrieval empty' : ''}
          {response.retrievalMode ? ` · ${response.retrievalMode}` : ''}
        </p>
      ) : null}
      {response.citations?.length ? (
        <div>
          <h3>Citations</h3>
          <ul>
            {response.citations.map((citation, index) => (
              <li key={`${citation.sourceId}-${citation.chunkId ?? index}`}>
                <strong>{citation.sourceId}</strong>
                {citation.locator ? ` · ${citation.locator}` : ''}
                {citation.snippet ? <div className="muted">{citation.snippet}</div> : null}
              </li>
            ))}
          </ul>
        </div>
      ) : (
        <p className="muted">No citations were returned.</p>
      )}
    </div>
  )
}
