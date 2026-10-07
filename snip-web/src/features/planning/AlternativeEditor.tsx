export function AlternativeEditor({
  name,
  ordinal,
  onRename,
  onRemove,
  canRemove,
}: {
  name: string
  ordinal: number
  onRename: (name: string) => void
  onRemove: () => void
  canRemove: boolean
}) {
  return (
    <div className="cluster">
      <label>
        Alternative {ordinal} name
        <input value={name} onChange={(event) => onRename(event.target.value)} />
      </label>
      {canRemove ? (
        <button type="button" className="btn btn-quiet" onClick={onRemove}>
          Remove alternative
        </button>
      ) : null}
    </div>
  )
}
