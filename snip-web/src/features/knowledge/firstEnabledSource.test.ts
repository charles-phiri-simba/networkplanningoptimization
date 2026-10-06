import { describe, expect, it } from 'vitest'
import { firstEnabledSource } from './firstEnabledSource'

describe('firstEnabledSource', () => {
  it('selects the first enabled source, matching proposal generation', () => {
    expect(
      firstEnabledSource([
        {
          sourceSystem: 'DISABLED',
          sourceScope: 'DEFAULT',
          connectorId: 'none',
          enabled: false,
        },
        {
          sourceSystem: 'ERICSSON_ENM_SIMULATOR',
          sourceScope: 'DEFAULT',
          connectorId: 'ERICSSON_ENM_SIMULATOR_INT_INVENTORY_READER',
          enabled: true,
        },
      ])?.sourceSystem,
    ).toBe('ERICSSON_ENM_SIMULATOR')
    expect(firstEnabledSource([])).toBeNull()
  })
})
