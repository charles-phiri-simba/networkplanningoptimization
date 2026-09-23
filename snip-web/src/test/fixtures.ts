import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import type { ExecutionDetailDto } from '../types/execution'
import type { CellContextDto, CellDto, GnbDto, SiteDto } from '../types/network'
import type { ChangePlanDetailDto } from '../types/plan'
import type { ChangeProposalDetailDto } from '../types/proposal'
import type { RecommendationResponse } from '../types/recommendation'
import type { SimulationDetailDto } from '../types/simulation'

export const siteFixture: SiteDto = {
  siteId: 'SITE-001',
  name: 'Midband Demo Site',
  latitude: -26.2041,
  longitude: 28.0473,
  status: 'ACTIVE',
}

export const gnbFixture: GnbDto = {
  gnbId: 'GNB-001',
  name: 'Demo gNB Midband',
  siteId: 'SITE-001',
  vendor: 'DemoVendor',
  model: 'SNIP-RAN-1',
  status: 'ACTIVE',
}

export const cellFixture: CellDto = {
  cellId: 'CELL-001',
  name: 'n78-1 high-BLER demo',
  gnbId: 'GNB-001',
  siteId: 'SITE-001',
  technology: 'NR',
  band: 'n78',
  arfcn: 630000,
  pci: 12,
  bandwidthMhz: 40,
  duplexMode: 'TDD',
  status: 'ACTIVE',
}

export const contextFixture: CellContextDto = {
  cell: cellFixture,
  gnb: gnbFixture,
  site: siteFixture,
  radioConfiguration: [
    { parameterName: 'txPower', parameterValue: '46', unit: 'dBm', effectiveFrom: '2026-01-01T00:00:00Z' },
  ],
  kpis: [
    {
      metric: 'BLER_DL',
      value: 0.12,
      unit: 'ratio',
      observedAt: '2026-01-01T00:00:00Z',
      eventTime: '2026-01-01T00:00:00Z',
      ingestedAt: '2026-01-01T00:00:00Z',
      eventId: 'e1',
      source: 'DEMO_SEED',
      synthetic: true,
    },
  ],
  neighbours: [{ targetCellId: 'CELL-002', relationType: 'INTRA_FREQ', status: 'ACTIVE' }],
  telemetry: [
    {
      metric: 'BLER_DL',
      current: {
        metric: 'BLER_DL',
        value: 0.12,
        unit: 'ratio',
        observedAt: '2026-01-01T00:00:00Z',
        eventTime: '2026-01-01T00:00:00Z',
        ingestedAt: '2026-01-01T00:00:00Z',
        eventId: 'e1',
        source: 'DEMO_SEED',
        synthetic: true,
      },
      history: [],
      trend: 'DEGRADING',
    },
  ],
  provenance: { source: 'DEMO_SEED', synthetic: true },
}

export const caseFixture: AssuranceCaseDto = {
  id: '11111111-1111-4111-8111-111111111111',
  caseType: 'DEGRADING_RADIO_QUALITY',
  affectedEntityType: 'CELL',
  affectedEntityId: 'CELL-001',
  severity: 'CRITICAL',
  confidence: 'HIGH',
  status: 'OPEN',
  detectedAt: '2026-01-01T00:00:00Z',
  firstObservedAt: '2026-01-01T00:00:00Z',
  lastObservedAt: '2026-01-01T00:00:00Z',
  ruleId: 'RULE_DEGRADING_RADIO_QUALITY_BLER_DL_V1',
  synthetic: true,
  evidence: [
    {
      id: '22222222-2222-4222-8222-222222222222',
      evidenceType: 'KPI',
      metric: 'BLER_DL',
      value: 0.12,
      unit: 'ratio',
      trend: 'DEGRADING',
      observedAt: '2026-01-01T00:00:00Z',
      source: 'DEMO_SEED',
      synthetic: true,
      description: 'High BLER',
    },
  ],
}

export const assessmentFixture: DecisionAssessmentDto = {
  assuranceCaseId: caseFixture.id,
  summary: 'Downlink BLER is above the critical threshold.',
  likelyContributors: ['High txPower', 'Interference'],
  recommendedChecks: ['Review neighbours', 'Inspect PRB utilisation'],
  missingEvidence: ['PRB utilisation time series is incomplete'],
  urgency: 'HIGH',
  humanReviewRequired: true,
  severity: 'CRITICAL',
  confidence: 'HIGH',
  caseType: 'DEGRADING_RADIO_QUALITY',
  status: 'OPEN',
  operationalEvidence: caseFixture.evidence,
  citations: [{ sourceId: 'doc-1', locator: 'p1', snippet: 'BLER guidance', chunkId: 'c1', score: 0.9 }],
  retrievalEmpty: false,
  retrievalMode: 'lexical',
  retrievalLatencyMs: 1,
  generationLatencyMs: 1,
  totalLatencyMs: 2,
}

export const recommendationFixture: RecommendationResponse = {
  recommendation: 'Investigate downlink BLER on CELL-001 before changing configuration.',
  citations: [{ sourceId: 'doc-1', locator: 'p1', snippet: 'Investigate BLER', chunkId: 'c1', score: 0.8 }],
  contextUsed: { id: 'CELL-001', kpis: { BLER_DL: 0.12 } },
  retrievalEmpty: false,
  retrievalMode: 'lexical',
  retrievalLatencyMs: 4,
  generationLatencyMs: 8,
  totalLatencyMs: 12,
  retrievalHitCount: 1,
  contextEvidence: {
    cellId: 'CELL-001',
    gnbId: 'GNB-001',
    siteId: 'SITE-001',
    source: 'DEMO_SEED',
    synthetic: true,
  },
  contextResolutionLatencyMs: 1,
  contextCellId: 'CELL-001',
  contextFound: true,
  kpiObservationCount: 1,
  neighbourCount: 0,
  historyObservationCount: 1,
  lastEventTime: '2026-01-01T00:00:00Z',
}

export const proposalId = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'
export const planId = 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb'
export const executionId = 'cccccccc-cccc-4ccc-8ccc-cccccccccccc'
export const simulationId = 'dddddddd-dddd-4ddd-8ddd-dddddddddddd'

export const recommendedProposalFixture: ChangeProposalDetailDto = {
  proposal: {
    id: proposalId,
    proposalType: 'RADIO_TX_POWER_OPTIMIZATION',
    status: 'RECOMMENDED',
    targetEntityType: 'CELL',
    targetEntityId: 'CELL-001',
    parameterName: 'txPower',
    currentValue: '46',
    proposedValue: '42',
    unit: 'dBm',
    networkKnowledgeConfidence: 'HIGH',
    assuranceConfidence: 'HIGH',
    simulationConfidence: 'LOW',
    riskLevel: 'LOW',
    benefitSummary: 'deterministic benefit from PRB/BLER/throughput deltas',
    proposalScore: 1.2,
    failureCode: null,
    failureReason: null,
    createdAt: '2026-01-01T00:00:00Z',
    evaluatedAt: '2026-01-01T00:00:01Z',
    expiresAt: '2026-01-02T00:00:00Z',
    invalidationReason: null,
    version: 3,
  },
  candidates: [
    {
      candidateValue: '42',
      baselineCandidate: false,
      validationOutcome: 'VALID',
      validationReason: null,
      simulationRunId: simulationId,
      simulationConfidence: 'LOW',
      benefitScore: 1.1,
      riskLevel: 'LOW',
      proposalScore: 1.2,
      rankOrder: 1,
    },
    {
      candidateValue: '46',
      baselineCandidate: true,
      validationOutcome: 'SKIPPED_BASELINE',
      validationReason: 'BASELINE',
      simulationRunId: null,
      simulationConfidence: null,
      benefitScore: null,
      riskLevel: null,
      proposalScore: null,
      rankOrder: 2,
    },
  ],
}

export const approvedProposalFixture: ChangeProposalDetailDto = {
  proposal: { ...recommendedProposalFixture.proposal, status: 'APPROVED' },
  candidates: recommendedProposalFixture.candidates,
}

export const simulationFixture: SimulationDetailDto = {
  id: simulationId,
  scenarioId: 'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeeee',
  twinId: 'ffffffff-ffff-4fff-8fff-ffffffffffff',
  baselineTwinVersion: 1,
  modelId: 'snip.synthetic.cell-parameter.v1',
  modelVersion: '1.0',
  modelType: 'RULE_BASED',
  status: 'SUCCEEDED',
  startedAt: '2026-01-01T00:00:00Z',
  completedAt: '2026-01-01T00:00:01Z',
  synthetic: true,
  confidence: 'LOW',
  assumptions: ['Deterministic synthetic model'],
  limitations: ['Not vendor-calibrated RF'],
  metrics: [
    { metric: 'txPower', baselineValue: 46, candidateValue: 42, delta: -4, unit: 'dBm' },
    { metric: 'BLER_DL', baselineValue: 0.12, candidateValue: 0.108, delta: -0.012, unit: 'ratio' },
    { metric: 'PRB_UTILIZATION_DL', baselineValue: 0.8, candidateValue: 0.768, delta: -0.032, unit: 'ratio' },
  ],
  provenance: {
    source: 'SNIP_OPERATIONAL_STATE',
    sourceCellId: 'CELL-001',
    sourceContextVersion: '1',
    sourceTelemetryTimestamp: '2026-01-01T00:00:00Z',
    capturedAt: '2026-01-01T00:00:00Z',
    synthetic: true,
  },
  actionId: null,
}

export const readyForReviewPlanFixture: ChangePlanDetailDto = {
  plan: {
    id: planId,
    proposalId,
    status: 'READY_FOR_REVIEW',
    targetEntityType: 'CELL',
    targetEntityId: 'CELL-001',
    parameterName: 'txPower',
    expectedCurrentValue: '46',
    desiredValue: '42',
    impactLevel: 'LOW',
    createdAt: '2026-01-01T00:00:00Z',
    expiresAt: '2026-01-02T00:00:00Z',
  },
  fingerprint: 'fp-1',
  authorizedFingerprint: null,
  knowledgeConfidenceAtCreation: 'HIGH',
  riskLevel: 'LOW',
  reviewedBy: null,
  reviewedAt: null,
  authorizedBy: null,
  authorizedAt: null,
  cancelledBy: null,
  cancelledAt: null,
  invalidationReason: null,
  invalidatedAt: null,
  operations: [
    {
      sequenceNumber: 1,
      operationType: 'SET_PARAMETER',
      targetEntityType: 'CELL',
      targetEntityId: 'CELL-001',
      parameterName: 'txPower',
      expectedCurrentValue: '46',
      desiredValue: '42',
    },
  ],
  rollbackOperations: [
    {
      sequenceNumber: 1,
      operationType: 'SET_PARAMETER',
      targetEntityType: 'CELL',
      targetEntityId: 'CELL-001',
      parameterName: 'txPower',
      expectedCurrentValue: '42',
      desiredValue: '46',
    },
  ],
  preconditions: [
    {
      preconditionType: 'EXPECTED_PARAMETER_VALUE',
      expectedCondition: '46',
      observedValue: '46',
      result: 'PASS',
      reasonCode: null,
      checkedAt: '2026-01-01T00:00:00Z',
    },
  ],
  readinessAssessments: [],
}

export const reviewedPlanFixture: ChangePlanDetailDto = {
  ...readyForReviewPlanFixture,
  reviewedBy: 'demo-network-engineer',
  reviewedAt: '2026-01-01T00:05:00Z',
}

export const authorizedPlanFixture: ChangePlanDetailDto = {
  ...reviewedPlanFixture,
  plan: { ...reviewedPlanFixture.plan, status: 'AUTHORIZED' },
  authorizedBy: 'demo-network-engineer',
  authorizedAt: '2026-01-01T00:06:00Z',
  authorizedFingerprint: 'fp-1',
}

export const readyPlanFixture: ChangePlanDetailDto = {
  ...authorizedPlanFixture,
  plan: { ...authorizedPlanFixture.plan, status: 'READY_FOR_EXECUTION' },
  readinessAssessments: [
    {
      assessedAt: '2026-01-01T00:07:00Z',
      result: 'READY',
      assessedFingerprint: 'fp-1',
      reasonCodes: null,
    },
  ],
}

export const reviewableExecutionFixture: ExecutionDetailDto = {
  executionId,
  planId,
  planVersion: 1,
  planFingerprint: 'fp-1',
  executionTargetId: 'snip-simulator',
  executionTargetType: 'SIMULATOR',
  executionTargetEnvironment: 'SIMULATOR',
  adapterProfileId: 'simulator-execution-v1',
  capabilityProfileVersion: '1',
  cellId: 'CELL-001',
  parameterName: 'txPower',
  executionFingerprint: 'ex-fp-1',
  authorizedExecutionFingerprint: null,
  status: 'READY_FOR_REVIEW',
  requestedBy: 'api-user',
  requestedAt: '2026-01-01T00:08:00Z',
  reviewedBy: null,
  reviewedAt: null,
  authorizedBy: null,
  authorizedAt: null,
  executionWindowOpensAt: null,
  executionWindowClosesAt: null,
  startedAt: null,
  completedAt: null,
  failureCode: null,
  failureDetailSafe: null,
  verificationStatus: null,
  recoveryStatus: 'NOT_REQUIRED',
  rollbackStatus: 'NOT_REQUESTED',
  operations: [
    {
      sequenceNumber: 1,
      operationType: 'SET_PARAMETER',
      targetEntityType: 'CELL',
      targetEntityId: 'CELL-001',
      parameterName: 'txPower',
      expectedCurrentValue: '46',
      desiredValue: '42',
    },
  ],
}

export const authorizedExecutionFixture: ExecutionDetailDto = {
  ...reviewableExecutionFixture,
  status: 'AUTHORIZED',
  reviewedBy: 'demo-network-engineer',
  reviewedAt: '2026-01-01T00:09:00Z',
  authorizedBy: 'demo-network-engineer',
  authorizedAt: '2026-01-01T00:10:00Z',
  authorizedExecutionFingerprint: 'ex-fp-1',
  executionWindowOpensAt: '2026-01-01T00:10:00Z',
  executionWindowClosesAt: '2026-01-01T01:10:00Z',
}

export const verifiedExecutionFixture: ExecutionDetailDto = {
  ...authorizedExecutionFixture,
  status: 'VERIFIED',
  verificationStatus: 'VERIFIED',
  startedAt: '2026-01-01T00:11:00Z',
  completedAt: '2026-01-01T00:11:01Z',
}
