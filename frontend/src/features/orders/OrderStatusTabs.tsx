import type { OrderStatus } from '@/api/types'
import { cn } from '@/lib/cn'
import { orderStatusLabel } from '@/lib/labels'

export function OrderStatusTabs({ statuses, value, onChange }: { statuses: OrderStatus[]; value: OrderStatus; onChange: (s: OrderStatus) => void }) {
  return (
    <fieldset className="mb-5 flex flex-wrap gap-1.5">
      <legend className="sr-only">Estado del pedido</legend>
      {statuses.map((s) => (
        <button
          key={s}
          type="button"
          aria-pressed={value === s}
          onClick={() => onChange(s)}
          className={cn('h-9 rounded-full px-4 text-sm font-semibold ring-1 ring-inset transition-colors', value === s ? 'bg-ink text-white ring-ink' : 'bg-card ring-line-strong hover:ring-ink')}
        >
          {orderStatusLabel[s]}
        </button>
      ))}
    </fieldset>
  )
}
