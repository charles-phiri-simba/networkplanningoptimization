import { describe, expect, it, vi, afterEach } from 'vitest'
import { ApiError, apiGet } from './client'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('api client', () => {
  it('returns JSON on success and captures correlation id on error', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ siteId: 'SITE-001' }), {
          status: 200,
          headers: { 'Content-Type': 'application/json', 'X-Correlation-Id': 'corr-ok' },
        }),
      ),
    )
    await expect(apiGet<{ siteId: string }>('/api/v1/sites/SITE-001')).resolves.toEqual({
      siteId: 'SITE-001',
    })
  })

  it('maps HTTP errors and backend-unavailable', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: 'site not found', id: 'SITE-X' }), {
          status: 404,
          headers: { 'X-Correlation-Id': 'corr-404' },
        }),
      ),
    )
    await expect(apiGet('/api/v1/sites/SITE-X')).rejects.toMatchObject({
      name: 'ApiError',
      status: 404,
      message: 'site not found',
      correlationId: 'corr-404',
    } satisfies Partial<ApiError>)

    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ error: 'execution disabled', failureCode: 'CHANGE_EXECUTION_DISABLED' }), {
          status: 403,
          headers: { 'X-Correlation-Id': 'corr-403' },
        }),
      ),
    )
    await expect(apiGet('/api/v1/change-execution/executions')).rejects.toMatchObject({
      status: 403,
      failureCode: 'CHANGE_EXECUTION_DISABLED',
    })

    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(apiGet('/api/v1/sites')).rejects.toMatchObject({
      status: 0,
      message: 'SNIP backend is unavailable',
    })
  })
})
