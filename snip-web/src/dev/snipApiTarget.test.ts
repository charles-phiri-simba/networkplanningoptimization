import { describe, expect, it } from 'vitest'
import { DEFAULT_SNIP_API_TARGET, resolveSnipApiTarget } from './snipApiTarget'

describe('resolveSnipApiTarget', () => {
  it('uses an explicit non-empty target', () => {
    expect(resolveSnipApiTarget('http://127.0.0.1:8081')).toBe('http://127.0.0.1:8081')
  })

  it('preserves the local default when unset or blank', () => {
    expect(resolveSnipApiTarget(undefined)).toBe(DEFAULT_SNIP_API_TARGET)
    expect(resolveSnipApiTarget('')).toBe(DEFAULT_SNIP_API_TARGET)
    expect(resolveSnipApiTarget('   ')).toBe(DEFAULT_SNIP_API_TARGET)
    expect(DEFAULT_SNIP_API_TARGET).toBe('http://127.0.0.1:8080')
  })
})
