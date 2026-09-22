import { apiGet, apiPost } from './client'
import type { AssuranceCaseDto, DecisionAssessmentDto } from '../types/assurance'
import type { CellContextDto, CellDto, GnbDto, SiteDto } from '../types/network'
import type { RecommendationRequest, RecommendationResponse } from '../types/recommendation'

export const snipApi = {
  listSites: () => apiGet<SiteDto[]>('/api/v1/sites'),
  getSite: (siteId: string) => apiGet<SiteDto>(`/api/v1/sites/${encodeURIComponent(siteId)}`),
  listGnbs: () => apiGet<GnbDto[]>('/api/v1/gnbs'),
  listCells: () => apiGet<CellDto[]>('/api/v1/cells'),
  getCellContext: (cellId: string) =>
    apiGet<CellContextDto>(`/api/v1/cells/${encodeURIComponent(cellId)}/context`),
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
