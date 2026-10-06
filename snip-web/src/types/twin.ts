export interface TwinDetailDto {
  id: string
  name: string
  scopeType: string
  scopeId: string
  status: string
  latestVersion: number
  createdAt: string | null
  synchronizedAt: string | null
  synthetic: boolean
  freshness: string
}
