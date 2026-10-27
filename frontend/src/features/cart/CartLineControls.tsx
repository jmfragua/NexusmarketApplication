import { Button } from '@/components/ui/Button'
import { QuantityStepper } from '@/components/ui/QuantityStepper'
import { cn } from '@/lib/cn'
import { useRemoveCartItem, useUpdateCartItem } from './queries'
import type { PricedLine } from './totals'

/**
 * Quantity and removal of a cart line, through the same cart endpoints wherever the line is shown:
 * the cart page uses the "Quitar" button, the side panel a compact "x".
 */
export function CartLineControls({ line, compact = false }: { line: PricedLine; compact?: boolean }) {
  const update = useUpdateCartItem()
  const remove = useRemoveCartItem()
  const busy = update.isPending || remove.isPending
  const locked = line.problem === 'unavailable' || line.problem === 'variant-missing'
  return (
    <div className={cn('flex items-center', compact ? 'gap-1' : 'gap-2')}>
      <QuantityStepper
        size="sm"
        label={`Cantidad de ${line.title}`}
        value={line.item.quantity}
        disabled={busy || locked}
        onChange={(quantity) => update.mutate({ cartItemId: line.item.id, quantity })}
      />
      {compact ? (
        <button
          type="button"
          onClick={() => remove.mutate(line.item.id)}
          disabled={busy}
          aria-label={`Quitar ${line.title} del carrito`}
          className="grid size-9 place-items-center rounded-full text-ink-soft hover:bg-coral-wash hover:text-coral-deep disabled:opacity-50"
        >
          <svg viewBox="0 0 20 20" className="size-4" aria-hidden="true">
            <path d="m5.5 5.5 9 9m0-9-9 9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        </button>
      ) : (
        <Button variant="ghost" size="sm" onClick={() => remove.mutate(line.item.id)} disabled={busy} aria-label={`Quitar ${line.title} del carrito`}>
          Quitar
        </Button>
      )}
    </div>
  )
}
