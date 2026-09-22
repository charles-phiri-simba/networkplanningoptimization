export function EmptyState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="state-panel">
      <p className="state-title">{title}</p>
      {detail ? <p className="muted">{detail}</p> : null}
    </div>
  )
}
