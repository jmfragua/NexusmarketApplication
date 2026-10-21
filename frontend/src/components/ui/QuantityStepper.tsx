import { cn } from '@/lib/cn'

interface QuantityStepperProps {
  value: number
  onChange: (next: number) => void
  min?: number
  max?: number
  disabled?: boolean
  label: string
  size?: 'sm' | 'md'
}

export function QuantityStepper({ value, onChange, min = 1, max = 99, disabled, label, size = 'md' }: QuantityStepperProps) {
  const box = size === 'sm' ? 'h-9' : 'h-11'
  const btn = cn(
    'grid shrink-0 place-items-center rounded-full text-lg font-bold transition-colors hover:bg-ink/8 disabled:opacity-40',
    size === 'sm' ? 'size-8' : 'size-10',
  )
  return (
    <fieldset className={cn('inline-flex w-max shrink-0 items-center rounded-full bg-card p-0.5 ring-1 ring-inset ring-line-strong', box)} aria-label={label}>
      <button type="button" className={btn} disabled={disabled || value <= min} onClick={() => onChange(value - 1)} aria-label="Restar uno">
        −
      </button>
      <input
        type="number"
        inputMode="numeric"
        min={min}
        max={max}
        value={value}
        disabled={disabled}
        aria-label={label}
        onChange={(e) => {
          const n = Number(e.target.value)
          if (Number.isInteger(n)) onChange(Math.min(max, Math.max(min, n)))
        }}
        className="w-10 [appearance:textfield] bg-transparent text-center font-semibold tabular-nums outline-none [&::-webkit-inner-spin-button]:appearance-none"
      />
      <button type="button" className={btn} disabled={disabled || value >= max} onClick={() => onChange(value + 1)} aria-label="Sumar uno">
        +
      </button>
    </fieldset>
  )
}
