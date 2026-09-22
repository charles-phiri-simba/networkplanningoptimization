import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import type { CellContextDto, CellDto, GnbDto, SiteDto } from '../types/network'
import type { RecommendationResponse } from '../types/recommendation'

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
  neighbours: [],
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
  missingEvidence: [],
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
