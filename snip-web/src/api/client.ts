export class ApiError extends Error {
  readonly status: number
  readonly correlationId: string | undefined
  readonly failureCode: string | undefined
  readonly body: unknown

  constructor(
    status: number,
    message: string,
    correlationId: string | undefined,
    body: unknown,
    failureCode?: string,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.correlationId = correlationId
    this.failureCode = failureCode
    this.body = body
  }
}

function newCorrelationId(): string {
  return crypto.randomUUID()
}

function messageFromBody(body: unknown, fallback: string): string {
  if (body && typeof body === 'object' && 'error' in body) {
    const error = (body as { error?: unknown }).error
    if (typeof error === 'string' && error.length > 0) {
      return error
    }
  }
  return fallback
}

function failureCodeFromBody(body: unknown): string | undefined {
  if (body && typeof body === 'object' && 'failureCode' in body) {
    const code = (body as { failureCode?: unknown }).failureCode
    if (typeof code === 'string' && code.length > 0) {
      return code
    }
  }
  return undefined
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const outgoingId = headers.get('X-Correlation-Id') ?? newCorrelationId()
  headers.set('X-Correlation-Id', outgoingId)

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'SNIP backend is unavailable', outgoingId, null)
  }

  const correlationId = response.headers.get('X-Correlation-Id') ?? outgoingId
  const text = await response.text()
  let parsed: unknown = null
  if (text.length > 0) {
    try {
      parsed = JSON.parse(text) as unknown
    } catch {
      parsed = text
    }
  }

  if (!response.ok) {
    const fallback =
      response.status === 404
        ? 'Resource not found'
        : response.status === 400
          ? 'Request was rejected'
          : `Request failed (${response.status})`
    throw new ApiError(
      response.status,
      messageFromBody(parsed, fallback),
      correlationId,
      parsed,
      failureCodeFromBody(parsed),
    )
  }

  return parsed as T
}

export function apiGet<T>(path: string, headers?: HeadersInit): Promise<T> {
  return apiRequest<T>(path, { method: 'GET', headers })
}

export function apiPost<T>(path: string, body?: unknown, headers?: HeadersInit): Promise<T> {
  return apiRequest<T>(path, {
    method: 'POST',
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}
