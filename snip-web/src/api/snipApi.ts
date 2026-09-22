import { apiGet, apiPost } from './client'
import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import type { CellContextDto, CellDto, GnbDto, KpiObservationDto, KpiSeriesDto, SiteDto } from '../types/network'
import type { RecommendationRequest, RecommendationResponse } from '../types/recommendation'

export const snipApi = {
  listSites: () => apiGet<SiteDto[]>('/api/v1/sites'),
  getSite: (siteId: string) => apiGet<SiteDto>(`/api/v1/sites/${encodeURIComponent(siteId)}`),
  listGnbs: () => apiGet<GnbDto[]>('/api/v1/gnbs'),
  listCells: () => apiGet<CellDto[]>('/api/v1/cells'),
  getCell: (cellId: string) => apiGet<CellDto>(`/api/v1/cells/${encodeURIComponent(cellId)}`),
  getCellContext: (cellId: string) =>
    apiGet<CellContextDto>(`/api/v1/cells/${encodeURIComponent(cellId)}/context`),
  getCellKpis: (cellId: string) =>
    apiGet<KpiObservationDto[]>(`/api/v1/cells/${encodeURIComponent(cellId)}/kpis`),
  getCellTelemetry: (cellId: string) =>
    apiGet<KpiSeriesDto[]>(`/api/v1/cells/${encodeURIComponent(cellId)}/telemetry`),
  getCellTelemetryMetric: (cellId: string, metric: string) =>
    apiGet<KpiSeriesDto>(
      `/api/v1/cells/${encodeURIComponent(cellId)}/telemetry/${encodeURIComponent(metric)}`,
    ),
  listAssuranceCases: () => apiGet<AssuranceCaseDto[]>('/api/v1/assurance/cases'),
  getAssuranceForCell: (cellId: string) =>
    apiGet<AssuranceCaseDto[]>(`/api/v1/cells/${encodeURIComponent(cellId)}/assurance`),
  getAssuranceCase: (caseId: string) =>
    apiGet<AssuranceCaseDto>(`/api/v1/assurance/cases/${encodeURIComponent(caseId)}`),
  getAssuranceAssessment: (caseId: string) =>
    apiGet<DecisionAssessmentDto>(`/api/v1/assurance/cases/${encodeURIComponent(caseId)}/assessment`),
  recommend: (request: RecommendationRequest) =>
    apiPost<RecommendationResponse>('/api/v1/recommendations', request),
}
