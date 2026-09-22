export const DEFAULT_SNIP_API_TARGET = 'http://127.0.0.1:8080'

export function resolveSnipApiTarget(value: string | undefined): string {
  const trimmed = value?.trim()
  return trimmed ? trimmed : DEFAULT_SNIP_API_TARGET
}
