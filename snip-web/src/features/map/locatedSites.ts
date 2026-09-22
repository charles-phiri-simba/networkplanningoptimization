import type { SiteDto } from '../../types/network'

export function locatedSites(sites: SiteDto[]): SiteDto[] {
  return sites.filter((site) => site.latitude !== null && site.longitude !== null)
}

export function mapCenter(sites: SiteDto[]): [number, number] | null {
  const located = locatedSites(sites)
  if (located.length === 0) {
    return null
  }
  return [
    located.reduce((sum, site) => sum + (site.latitude ?? 0), 0) / located.length,
    located.reduce((sum, site) => sum + (site.longitude ?? 0), 0) / located.length,
  ]
}
