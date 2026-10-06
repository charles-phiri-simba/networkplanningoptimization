import { ApiError } from '../api/client'
import { translateOperatorError } from '../features/operator/operatorMessages'

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const apiError = error instanceof ApiError ? error : null
  const translated = translateOperatorError(error)
  const message =
    translated?.message ??
    apiError?.message ??
    (error instanceof Error ? error.message : 'Unexpected error')
  const status = apiError?.status
  const heading =
    translated?.heading ??
    (status === 0
      ? 'Backend unavailable'
      : status === 404
        ? 'Not found'
        : status === 400
          ? 'Validation error'
          : status === 403
            ? 'Permission required'
            : 'Request failed')

  return (
    <div className="state-panel state-error" role="alert">
      <p className="state-title">{heading}</p>
      <p>{message}</p>
      {apiError?.failureCode || apiError?.correlationId ? (
        <details>
          <summary>Technical details</summary>
          {apiError.failureCode ? (
            <p className="muted diagnostic">failureCode {apiError.failureCode}</p>
          ) : null}
          {apiError.correlationId ? (
            <p className="muted diagnostic">Correlation ID {apiError.correlationId}</p>
          ) : null}
        </details>
      ) : null}
      {onRetry ? (
        <button type="button" className="btn" onClick={onRetry}>
          Retry
        </button>
      ) : null}
    </div>
  )
}
