import { validateTxPower } from './planningGuards'

export function IntentEditor({
  cellId,
  value,
  onChange,
}: {
  cellId: string
  value: number
  onChange: (value: number) => void
}) {
  const error = validateTxPower(value)
  return (
    <label>
      {cellId} intended txPower (dBm)
      <input
        type="number"
        min={20}
        max={50}
        step={1}
        value={Number.isFinite(value) ? value : ''}
        onChange={(event) => onChange(Number(event.target.value))}
      />
      {error ? <span className="muted">{error}</span> : null}
    </label>
  )
}
