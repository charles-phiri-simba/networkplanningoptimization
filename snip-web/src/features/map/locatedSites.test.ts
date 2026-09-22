import { describe, expect, it } from 'vitest'
import type { SiteDto } from '../../types/network'
import { locatedSites, mapCenter } from './locatedSites'

const located: SiteDto = {
  siteId: 'SITE-001',
  name: 'Midband Demo Site',
  latitude: -26.2041,
  longitude: 28.0473,
  status: 'ACTIVE',
}

const unlocated: SiteDto = {
  siteId: 'SITE-SIM-001',
  name: 'Sim Site',
  latitude: null,
  longitude: null,
  status: 'ACTIVE',
}

describe('locatedSites', () => {
  it('uses real site coordinates and omits sites without coordinates', () => {
    expect(locatedSites([located, unlocated])).toEqual([located])
    expect(mapCenter([located, unlocated])).toEqual([-26.2041, 28.0473])
  })

  it('does not invent coordinates for unlocated sites or cells', () => {
    expect(locatedSites([unlocated])).toEqual([])
    expect(mapCenter([unlocated])).toBeNull()
  })
})
