import { SimulationEvidence } from '../optimization/SimulationEvidence'
import type { PlanningEvaluationDto } from '../../types/planning'
import { evaluationViewLabel } from './planningCopy'

export function EvaluationEvidencePanel({ evaluation }: { evaluation: PlanningEvaluationDto }) {
  return (
    <section className="panel" aria-labelledby="eval-heading">
      <header className="panel-header">
        <h2 id="eval-heading">Evaluation evidence</h2>
        <p className="muted">
          {evaluation.historical ? 'Historical evaluation against pinned Digital Twin versions.' : 'Current evaluation of these intents.'}{' '}
          Status: {evaluation.status === 'PARTIAL' ? 'Partially evaluated' : evaluation.status}.
        </p>
      </header>
      {evaluation.status === 'PARTIAL' ? (
        <p role="status">
          Partially evaluated. Failed cells:{' '}
          {evaluation.items
            .filter((item) => item.outcome === 'FAILED')
            .map((item) => item.cellId)
            .join(', ') || 'none'}
          .
        </p>
      ) : null}
      <p className="muted">{evaluationViewLabel(evaluation.status === 'SUCCEEDED' ? 'EVALUATED' : evaluation.status)}</p>
      {evaluation.items.map((item) => (
        <article key={item.id} className="panel">
          <h3>
            {item.alternativeName} · {item.cellId}
          </h3>
          <p className="muted">
            {item.parameterId} {item.pinnedBaselineTxPower ?? '—'} → {item.intendedValue} dBm · twin v
            {item.twinVersion ?? '—'} · {item.synthetic ? 'SYNTHETIC' : ''} {item.confidence ?? ''} · model{' '}
            {item.modelId} {item.modelVersion}
          </p>
          {item.outcome === 'FAILED' ? (
            <p>{item.failureCode}: {item.failureMessage}</p>
          ) : item.simulationRunId ? (
            <SimulationEvidence
              candidates={[
                {
                  candidateValue: String(item.intendedValue),
                  baselineCandidate: false,
                  validationOutcome: null,
                  validationReason: null,
                  simulationRunId: item.simulationRunId,
                  simulationConfidence: item.confidence,
                  benefitScore: null,
                  riskLevel: null,
                  proposalScore: null,
                  rankOrder: 1,
                },
              ]}
            />
          ) : null}
        </article>
      ))}
    </section>
  )
}
