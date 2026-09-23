import { useEffect, useState } from 'react'
import { snipApi } from '../../api/snipApi'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import type { CandidateEvidenceDto } from '../../types/proposal'
import type { SimulationDetailDto } from '../../types/simulation'
import { formatNumber } from '../../utils/format'

const DISPLAY_METRICS = new Set(['txPower', 'BLER_DL', 'PRB_UTILIZATION_DL', 'THROUGHPUT_DL'])

export function SimulationEvidence({ candidates }: { candidates: CandidateEvidenceDto[] }) {
  const ranked = candidates.find((candidate) => candidate.rankOrder === 1 && candidate.simulationRunId)
  const fallback = candidates.find((candidate) => candidate.simulationRunId)
  const simulationId = ranked?.simulationRunId ?? fallback?.simulationRunId
  const [simulation, setSimulation] = useState<SimulationDetailDto | null>(null)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    if (!simulationId) {
      setSimulation(null)
      setError(null)
      return
    }
    setSimulation(null)
    setError(null)
    snipApi.getSimulation(simulationId).then(setSimulation).catch(setError)
  }, [simulationId])

  if (!simulationId) {
    return (
      <section className="panel" aria-labelledby="sim-heading">
        <h2 id="sim-heading">Synthetic simulation evidence</h2>
        <p className="muted">No simulationRunId is available on this proposal&apos;s candidates.</p>
      </section>
    )
  }

  return (
    <section className="panel" aria-labelledby="sim-heading">
      <header className="panel-header">
        <h2 id="sim-heading">Synthetic simulation evidence</h2>
        <p className="muted">Retrieved from GET /api/v1/simulations/{simulationId}. Not a live RF forecast.</p>
      </header>
      <p className="banner-synthetic" role="note">
        <strong>SYNTHETIC SIMULATION</strong> · LOW CONFIDENCE · NOT VENDOR-CALIBRATED RF · NO REAL
        NETWORK CHANGE
      </p>
      {error ? <ErrorState error={error} /> : null}
      {!error && !simulation ? <LoadingState label="Loading simulation result…" /> : null}
      {simulation ? <SimulationDetail simulation={simulation} /> : null}
    </section>
  )
}

function SimulationDetail({ simulation }: { simulation: SimulationDetailDto }) {
  const metrics = simulation.metrics.filter((metric) => DISPLAY_METRICS.has(metric.metric))
  return (
    <>
      <dl className="kv">
        <div>
          <dt>Model</dt>
          <dd>
            {simulation.modelId ?? '—'} {simulation.modelVersion ? `v${simulation.modelVersion}` : ''}
          </dd>
        </div>
        <div>
          <dt>Confidence</dt>
          <dd>{simulation.confidence ?? '—'}</dd>
        </div>
        <div>
          <dt>Synthetic</dt>
          <dd>{simulation.synthetic ? 'Yes' : 'No'}</dd>
        </div>
        <div>
          <dt>Status</dt>
          <dd>{simulation.status}</dd>
        </div>
      </dl>
      {metrics.length > 0 ? (
        <table className="data-table">
          <thead>
            <tr>
              <th scope="col">Metric</th>
              <th scope="col">Baseline</th>
              <th scope="col">Candidate</th>
              <th scope="col">Delta</th>
              <th scope="col">Unit</th>
            </tr>
          </thead>
          <tbody>
            {metrics.map((metric) => (
              <tr key={metric.metric}>
                <td>{metric.metric}</td>
                <td>{formatNumber(metric.baselineValue)}</td>
                <td>{formatNumber(metric.candidateValue)}</td>
                <td>{formatNumber(metric.delta)}</td>
                <td>{metric.unit ?? '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p className="muted">No supported metric rows were returned.</p>
      )}
      {simulation.assumptions.length > 0 ? (
        <div>
          <h3>Assumptions</h3>
          <ul>
            {simulation.assumptions.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      ) : null}
      {simulation.limitations.length > 0 ? (
        <div>
          <h3>Limitations</h3>
          <ul>
            {simulation.limitations.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      ) : null}
    </>
  )
}
