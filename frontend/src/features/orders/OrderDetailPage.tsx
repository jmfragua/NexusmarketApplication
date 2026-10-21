import { useMemo, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ApiError } from '@/api/client'
import type { OrderLineResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { QuantityStepper } from '@/components/ui/QuantityStepper'
import { EmptyState, ErrorState, LoadingRows, Notice, Skeleton } from '@/components/ui/States'
import { ProductCover } from '@/features/catalog/ProductCover'
import { useProductLookup } from '@/features/catalog/useProductLookup'
import { fallbackTitle, readProduct, variantLabel } from '@/features/catalog/variant'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { OrderStatusBadge } from './OrderStatusBadge'
import { useInvoiceOf, useMyOrder, usePayOrder, useRequestReturn, useReturnsOf } from './queries'
import { StatusTimeline } from './StatusTimeline'

interface LineView {
  line: OrderLineResponse
  title: string
  variantText: string
  image: string | null
}

function ReturnDialog({ orderId, target, onClose }: { orderId: string; target: LineView | null; onClose: () => void }) {
  const [quantity, setQuantity] = useState(1)
  const request = useRequestReturn(orderId)
  return (
    <Dialog
      open={target !== null}
      onOpenChange={(open) => !open && onClose()}
      title="solicitar devolución"
      description={target ? `${target.title}${target.variantText ? ` · ${target.variantText}` : ''}` : undefined}
    >
      {target && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            request.mutate({ orderLineId: target.line.id, quantity }, { onSuccess: onClose })
          }}
          className="grid gap-5"
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <span className="font-semibold">¿cuántas unidades devuelves?</span>
            <QuantityStepper value={quantity} onChange={setQuantity} max={target.line.quantity} label="Unidades a devolver" />
          </div>
          <p className="text-sm text-ink-soft">
            compraste {target.line.quantity}. El equipo de NexusMarket revisará la solicitud y, si procede, emitirá el reembolso.
          </p>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={onClose}>
              Cancelar
            </Button>
            <Button type="submit" loading={request.isPending}>
              Enviar solicitud
            </Button>
          </div>
        </form>
      )}
    </Dialog>
  )
}

export function OrderDetailPage() {
  const { orderId = '' } = useParams()
  const location = useLocation()
  const fresh = (location.state as { fresh?: boolean } | null)?.fresh === true
  const { data: order, isPending, error, refetch } = useMyOrder(orderId)
  const pay = usePayOrder()
  const delivered = order?.status === 'DELIVERED'
  const invoiceExpected = order !== undefined && order.status !== 'PENDING_PAYMENT' && order.status !== 'CART'
  const invoice = useInvoiceOf(orderId, invoiceExpected)
  const returns = useReturnsOf(orderId, delivered)
  const [returning, setReturning] = useState<LineView | null>(null)

  const productIds = useMemo(() => order?.lines.map((l) => l.productId) ?? [], [order])
  const lookup = useProductLookup(productIds)

  const lines = useMemo<LineView[]>(
    () =>
      (order?.lines ?? []).map((line) => {
        const raw = lookup.products.get(line.productId)
        const product = raw ? readProduct(raw) : null
        const variant = product?.variants.find((v) => v.id === line.productVariantId) ?? null
        return {
          line,
          title: variant?.name ?? product?.title ?? fallbackTitle(line.productId),
          variantText: variant ? variantLabel(variant) : '',
          image: variant?.image ?? product?.image ?? null,
        }
      }),
    [order, lookup.products],
  )

  if (isPending) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <Skeleton className="mb-6 h-12 w-72" />
        <LoadingRows rows={3} label="cargando el pedido" />
      </div>
    )
  }
  if (error || !order) {
    const missing = error instanceof ApiError && (error.status === 404 || error.status === 403)
    return (
      <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
        {missing ? (
          <EmptyState title="no encontramos este pedido" description="revisa el enlace o vuelve a la lista de tus pedidos." action={<ButtonLink to="/pedidos">Ver mis pedidos</ButtonLink>} />
        ) : (
          <ErrorState error={error} onRetry={() => void refetch()} />
        )}
      </div>
    )
  }

  const returnedUnits = (orderLineId: string) =>
    (returns.data?.content ?? []).filter((r) => r.orderLineId === orderLineId).reduce((s, r) => s + r.quantity, 0)

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <Link to="/pedidos" className="text-sm font-semibold text-ink-soft hover:text-ink">
        <span aria-hidden="true">←</span> Mis pedidos
      </Link>

      <header className="mt-4 mb-8 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-[clamp(2rem,5vw,3rem)] font-extrabold">pedido #{shortId(order.id)}</h1>
          <div className="mt-2 flex flex-wrap items-center gap-2">
            <OrderStatusBadge status={order.status} />
            <CopyId id={order.id} label="número de pedido" />
          </div>
        </div>
        <p className="font-display text-3xl font-extrabold tabular-nums">{formatMoney(order.totalAmount, order.currency)}</p>
      </header>

      {fresh && order.status === 'PAID' && (
        <div className="mb-6 flex items-center gap-4 rounded-[var(--radius-card)] bg-lime p-5 motion-safe:animate-rise">
          <span className="sticker shrink-0 text-lg" data-tone="cobalt">
            ¡pagado!
          </span>
          <p className="font-semibold">gracias por tu compra. Te avisamos aquí mismo cuando el pedido salga de la bodega.</p>
        </div>
      )}

      <section className="rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:p-6" aria-labelledby="estado">
        <h2 id="estado" className="sr-only">
          Estado
        </h2>
        <StatusTimeline status={order.status} />
        {order.status === 'PENDING_PAYMENT' && (
          <div className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-amber-wash p-4">
            <p className="font-semibold text-amber-ink">este pedido espera la confirmación del pago.</p>
            <Button loading={pay.isPending} onClick={() => pay.mutate(order.id)}>
              Confirmar pago
            </Button>
          </div>
        )}
      </section>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_20rem]">
        <section aria-labelledby="productos">
          <h2 id="productos" className="mb-3 text-2xl font-bold">
            productos
          </h2>
          <ul className="divide-y divide-line rounded-[var(--radius-card)] bg-card px-5 ring-1 ring-line">
            {lines.map((view) => {
              const already = returnedUnits(view.line.id)
              return (
                <li key={view.line.id} className="flex flex-wrap items-center gap-4 py-4">
                  <div className="size-16 shrink-0 overflow-hidden rounded-xl bg-paper ring-1 ring-line" aria-hidden="true">
                    <ProductCover productId={view.line.productId} title={view.title} image={view.image} />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">{view.title}</p>
                    <p className="text-sm text-ink-soft">
                      {view.line.quantity} {view.line.quantity === 1 ? 'unidad' : 'unidades'}
                      {view.variantText && ` · ${view.variantText}`}
                    </p>
                    {already > 0 && (
                      <div className="mt-1 flex flex-wrap items-center gap-1.5 text-sm font-medium text-cobalt">
                        <span>devolución solicitada: {already}</span>
                        {(returns.data?.content ?? [])
                          .filter((r) => r.orderLineId === view.line.id)
                          .map((r) => (
                            <CopyId key={r.id} id={r.id} label="identificador de la devolución" />
                          ))}
                      </div>
                    )}
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="font-semibold tabular-nums">{formatMoney(view.line.lineAmount, view.line.currency)}</span>
                    {delivered && (
                      <Button variant="outline" size="sm" onClick={() => setReturning(view)}>
                        Devolver
                      </Button>
                    )}
                  </div>
                </li>
              )
            })}
          </ul>
          {!delivered && order.status !== 'PENDING_PAYMENT' && (
            <p className="mt-3 text-sm text-ink-soft">podrás solicitar devoluciones cuando el pedido esté entregado.</p>
          )}
        </section>

        <aside className="grid content-start gap-4">
          <div className="rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line">
            <h2 className="text-lg font-bold">entrega</h2>
            <p className="mt-1 text-ink-soft">{order.deliveryAddress}</p>
          </div>
          <div className="rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line">
            <h2 className="text-lg font-bold">factura</h2>
            {!invoiceExpected ? (
              <p className="mt-1 text-ink-soft">se emite después de confirmar el pago.</p>
            ) : invoice.isPending ? (
              <Skeleton className="mt-2 h-10" />
            ) : invoice.error ? (
              <p className="mt-1 text-coral-deep">no pudimos consultar la factura.</p>
            ) : invoice.data ? (
              <div className="mt-2 grid gap-1">
                <p className="flex justify-between">
                  <span className="text-ink-soft">Factura</span> <CopyId id={invoice.data.id} label="número de factura" />
                </p>
                <p className="flex justify-between">
                  <span className="text-ink-soft">Total facturado</span>
                  <strong className="tabular-nums">{formatMoney(invoice.data.totalAmount, invoice.data.currency)}</strong>
                </p>
              </div>
            ) : (
              <Notice title="aún no se ha emitido">el equipo de NexusMarket la genera después del pago; vuelve a revisar más tarde.</Notice>
            )}
          </div>
        </aside>
      </div>

      <ReturnDialog key={returning?.line.id ?? "ninguna"} orderId={order.id} target={returning} onClose={() => setReturning(null)} />
    </div>
  )
}
