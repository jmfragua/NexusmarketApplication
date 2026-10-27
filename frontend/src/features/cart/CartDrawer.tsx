import * as RadixDialog from '@radix-ui/react-dialog'
import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Button, ButtonLink } from '@/components/ui/Button'
import { ProductCover } from '@/features/catalog/ProductCover'
import { formatMoney } from '@/lib/money'
import { CartLineControls } from './CartLineControls'
import { useCart, usePricedCart } from './queries'

/**
 * The cart at a glance, in a side panel: its lines, the total and the way to pay. It reads the same
 * cart and the same catalogue prices as the cart page. Radix traps the focus inside, closes on Esc
 * and gives the focus back to `trigger`, the button that opens it.
 */
export function CartDrawer({ open, onOpenChange, trigger }: { open: boolean; onOpenChange: (open: boolean) => void; trigger: ReactNode }) {
  const { data: cart, isPending } = useCart()
  const { totals, loading } = usePricedCart(cart)
  const close = () => onOpenChange(false)
  const empty = !isPending && (cart?.items.length ?? 0) === 0
  // With no line priced (every product gone or without price) a total of 0 would be misleading.
  const priced = totals.lines.some((line) => line.lineAmount !== null)

  return (
    <RadixDialog.Root open={open} onOpenChange={onOpenChange}>
      <RadixDialog.Trigger asChild>{trigger}</RadixDialog.Trigger>
      <RadixDialog.Portal>
        <RadixDialog.Overlay className="fixed inset-0 z-40 bg-ink/35" />
        <RadixDialog.Content className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col bg-paper shadow-[-24px_0_60px_-20px_rgba(11,18,51,0.45)] outline-none motion-safe:data-[state=open]:animate-drawer">
          <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
            <div>
              <RadixDialog.Title className="font-display text-2xl font-bold">tu carrito</RadixDialog.Title>
              <RadixDialog.Description className="text-sm text-ink-soft">
                {empty ? 'todavía no tienes productos.' : `${totals.itemCount} ${totals.itemCount === 1 ? 'artículo' : 'artículos'} en tu carrito`}
              </RadixDialog.Description>
            </div>
            <RadixDialog.Close className="grid size-10 shrink-0 place-items-center rounded-full text-ink-soft hover:bg-ink/6 hover:text-ink" aria-label="Cerrar el carrito">
              <svg viewBox="0 0 20 20" className="size-5" aria-hidden="true">
                <path d="m5 5 10 10M15 5 5 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </RadixDialog.Close>
          </div>

          <div className="flex-1 overflow-y-auto px-5 py-4">
            {isPending || loading ? (
              <output className="block text-ink-soft">cargando tu carrito…</output>
            ) : empty ? (
              <p className="text-ink-soft">cuando agregues productos los verás aquí.</p>
            ) : (
              <ul className="grid gap-3" aria-label="Productos en el carrito">
                {totals.lines.map((line) => (
                  <li key={line.item.id} className="flex gap-3 rounded-2xl bg-card p-2.5 ring-1 ring-line">
                    <div className="aspect-[4/3] w-20 shrink-0 overflow-hidden rounded-xl bg-cobalt-wash">
                      <ProductCover productId={line.item.productId} title={line.title} image={line.variant?.image ?? line.product?.image ?? null} />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-semibold">
                        {line.problem === 'unavailable' ? (
                          line.title
                        ) : (
                          <Link to={`/productos/${encodeURIComponent(line.item.productId)}`} onClick={close} className="rounded-sm hover:text-cobalt hover:underline">
                            {line.title}
                          </Link>
                        )}
                      </p>
                      {line.variantText && <p className="truncate text-sm text-ink-soft">{line.variantText}</p>}
                      <p className="mt-0.5 flex justify-between gap-2 text-sm">
                        <span className="text-ink-soft">{line.unitPrice === null ? 'sin precio' : `${formatMoney(line.unitPrice, line.currency ?? undefined)} c/u`}</span>
                        <span className="font-semibold tabular-nums">{line.lineAmount === null ? 'no disponible' : formatMoney(line.lineAmount, line.currency ?? undefined)}</span>
                      </p>
                      <div className="mt-1.5">
                        <CartLineControls line={line} compact />
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="border-t border-line bg-card px-5 py-4">
            {!empty && (
              <div className="mb-4 flex items-end justify-between">
                <span className="text-ink-soft">Total</span>
                <span className="font-display text-2xl font-extrabold tabular-nums">
                  {loading ? '…' : priced ? formatMoney(totals.total, totals.currency ?? undefined) : '—'}
                </span>
              </div>
            )}
            {!empty && totals.blocker && !loading && <p className="mb-3 text-sm text-coral-deep">{totals.blocker}</p>}
            <div className="grid gap-2">
              {!empty &&
                (totals.blocker || loading ? (
                  <Button variant="accent" disabled>
                    Ir a pagar
                  </Button>
                ) : (
                  <ButtonLink to="/pagar" variant="accent" onClick={close}>
                    Ir a pagar
                  </ButtonLink>
                ))}
              <Button variant="outline" onClick={close}>
                Seguir comprando
              </Button>
              {!empty && (
                <Link to="/carrito" onClick={close} className="justify-self-center rounded-sm py-1 text-sm font-semibold text-cobalt underline-offset-4 hover:underline">
                  Ver el carrito completo
                </Link>
              )}
            </div>
          </div>
        </RadixDialog.Content>
      </RadixDialog.Portal>
    </RadixDialog.Root>
  )
}
