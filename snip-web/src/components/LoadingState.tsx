export function LoadingState({ label }: { label: string }) {
  return (
    <div className="state-panel" role="status" aria-live="polite">
      <p>{label}</p>
    </div>
  )
}
