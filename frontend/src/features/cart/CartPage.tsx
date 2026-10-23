import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Badge } from '@/components/ui/Badge'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { PageHeader } from '@/components/ui/PageHeader'
import { QuantityStepper } from '@/components/ui/QuantityStepper'
import { EmptyState, ErrorState, LoadingRows, Notice } from '@/components/ui/States'
import { ProductCover } from '@/features/catalog/ProductCover'
import { formatMoney } from '@/lib/money'
import { useCart, useClearCart, usePricedCart, useRemoveCartItem, useUpdateCartItem } from './queries'
import type { PricedLine } from './totals'

const problemText: Record<NonNullable<PricedLine['problem']>, string> = {
  unavailable: 'ya no está disponible',
  'variant-missing': 'esta opción fue retirada',
  'no-price': 'sin precio publicado',
}

function CartLine({ line }: { line: PricedLine }) {
  const update = useUpdateCartItem()
  const remove = useRemoveCartItem()
  const busy = update.isPending || remove.isPending
  const productHref = `/productos/${encodeURIComponent(line.item.productId)}`
  return (
    <li className="grid grid-cols-[4.5rem_1fr] gap-4 py-5 sm:grid-cols-[6rem_1fr_auto] sm:items-center">
      <Link to={productHref} className="block aspect-square overflow-hidden rounded-2xl bg-paper ring-1 ring-line" tabIndex={-1} aria-hidden="true">
        <ProductCover productId={line.item.productId} title={line.title} image={line.variant?.image ?? line.product?.image ?? null} />
      </Link>
      <div className="min-w-0">
        <h3 className="text-lg leading-snug font-bold">
          {line.problem === 'unavailable' ? line.title : <Link to={productHref} className="hover:text-cobalt">{line.title}</Link>}
        </h3>
        {line.variantText && <p className="text-sm text-ink-soft">{line.variantText}</p>}
        {line.problem ? (
          <Badge tone="coral" className="mt-2">
            {problemText[line.problem]}
          </Badge>
        ) : (
          line.unitPrice !== null && (
            <p className="mt-1 text-sm text-ink-soft">{formatMoney(line.unitPrice, line.currency ?? undefined)} c/u</p>
          )
        )}
      </div>
      <div className="col-span-2 flex items-center justify-between gap-3 sm:col-span-1 sm:flex-col sm:items-end">
        <div className="flex items-center gap-2">
          <QuantityStepper
            size="sm"
            label={`Cantidad de ${line.title}`}
            value={line.item.quantity}
            disabled={busy || line.problem === 'unavailable' || line.problem === 'variant-missing'}
            onChange={(quantity) => update.mutate({ cartItemId: line.item.id, quantity })}
          />
          <Button variant="ghost" size="sm" onClick={() => remove.mutate(line.item.id)} disabled={busy} aria-label={`Quitar ${line.title} del carrito`}>
            Quitar
          </Button>
        </div>
        <p className="font-display text-xl font-bold tabular-nums">
          {line.lineAmount !== null ? formatMoney(line.lineAmount, line.currency ?? undefined) : '—'}
        </p>
      </div>
    </li>
  )
}

export function CartPage() {
  const cart = useCart()
  const { totals, loading, error } = usePricedCart(cart.data)
  const clear = useClearCart()
  const [confirmClear, setConfirmClear] = useState(false)

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <PageHeader title="tu carrito" description="revisa cantidades antes de pagar; los precios se toman del catálogo en este momento." />

      {cart.isPending ? (
        <LoadingRows rows={3} label="cargando tu carrito" />
      ) : cart.error ? (
        <ErrorState error={cart.error} onRetry={() => void cart.refetch()} />
      ) : cart.data.items.length === 0 ? (
        <EmptyState
          title="tu carrito está vacío"
          description="cuando agregues productos los verás aquí, con el total listo para pagar."
          action={<ButtonLink to="/">Explorar el catálogo</ButtonLink>}
        />
      ) : (
        <div className="grid gap-8 lg:grid-cols-[1fr_22rem]">
          <section aria-labelledby="carrito-productos" className="rounded-[var(--radius-card)] bg-card px-5 ring-1 ring-line">
            <h2 id="carrito-productos" className="sr-only">
              Productos en el carrito
            </h2>
            {error && (
              <div className="pt-5">
                <ErrorState error={error} title="no pudimos consultar algunos precios" />
              </div>
            )}
            <ul className="divide-y divide-line">
              {totals.lines.map((line) => (
                <CartLine key={line.item.id} line={line} />
              ))}
            </ul>
          </section>

          <aside className="h-fit rounded-[var(--radius-card)] bg-ink p-6 text-white lg:sticky lg:top-24" aria-label="Resumen">
            <h2 className="text-2xl font-bold text-white">resumen</h2>
            <dl className="mt-5 grid gap-2 text-white/80">
              <div className="flex justify-between">
                <dt>Productos</dt>
                <dd className="tabular-nums">{totals.itemCount}</dd>
              </div>
              <div className="flex justify-between">
                <dt>Envío</dt>
                <dd>se coordina al despachar</dd>
              </div>
            </dl>
            <div className="mt-5 flex items-end justify-between border-t border-white/15 pt-5">
              <span className="text-white/80">Total</span>
              <span className="font-display text-3xl font-extrabold tabular-nums" aria-live="polite">
                {loading ? '…' : formatMoney(totals.total, totals.currency ?? undefined)}
              </span>
            </div>
            {totals.blocker && !loading && (
              <div className="mt-4">
                <Notice tone="coral" title="aún no puedes pagar">
                  {totals.blocker}
                </Notice>
              </div>
            )}
            {totals.blocker || loading ? (
              <Button variant="accent" size="lg" className="mt-6 w-full" disabled>
                Ir a pagar
              </Button>
            ) : (
              <ButtonLink to="/pagar" variant="accent" size="lg" className="mt-6 w-full">
                Ir a pagar
              </ButtonLink>
            )}
            <button type="button" className="mt-3 w-full rounded-full py-2 text-sm font-semibold text-white/75 hover:text-white" onClick={() => setConfirmClear(true)}>
              Vaciar carrito
            </button>
          </aside>
        </div>
      )}

      <Dialog open={confirmClear} onOpenChange={setConfirmClear} title="¿vaciar el carrito?" description="quitaremos todos los productos. Puedes volver a agregarlos desde el catálogo.">
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => setConfirmClear(false)}>
            Conservar
          </Button>
          <Button variant="danger" loading={clear.isPending} onClick={() => clear.mutate(undefined, { onSuccess: () => setConfirmClear(false) })}>
            Vaciar carrito
          </Button>
        </div>
      </Dialog>
    </div>
  )
}
