import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ButtonLink } from '@/components/ui/Button'
import { BoxArt } from '@/components/ui/EmptyArt'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { StorePage } from '@/components/ui/StorePage'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { OrderStatusBadge } from './OrderStatusBadge'
import { useMyOrders } from './queries'

export function OrdersPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, error, refetch } = useMyOrders(page)

  return (
    <StorePage>
      <PageHeader title="mis pedidos" description="sigue cada compra desde el pago hasta la entrega." />
      {isPending ? (
        <LoadingRows label="cargando tus pedidos" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState art={<BoxArt />} title="aún no tienes pedidos" description="cuando pagues tu primer carrito, aparecerá aquí." action={<ButtonLink to="/">Explorar el catálogo</ButtonLink>} />
      ) : (
        <>
          <ul className="grid gap-3">
            {data.content.map((order) => {
              const units = order.lines.reduce((s, l) => s + l.quantity, 0)
              return (
                <li key={order.id}>
                  <Link
                    to={`/pedidos/${encodeURIComponent(order.id)}`}
                    className="flex flex-wrap items-center justify-between gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line transition-shadow hover:ring-2 hover:ring-ink"
                  >
                    <div>
                      <p className="font-display text-lg font-bold">pedido #{shortId(order.id)}</p>
                      <p className="text-sm text-ink-soft">
                        {units} {units === 1 ? 'producto' : 'productos'} · {order.deliveryAddress}
                      </p>
                    </div>
                    <div className="flex items-center gap-4">
                      <OrderStatusBadge status={order.status} />
                      <span className="font-display text-xl font-bold tabular-nums">{formatMoney(order.totalAmount, order.currency)}</span>
                    </div>
                  </Link>
                </li>
              )
            })}
          </ul>
          <Pagination page={data} onChange={setPage} />
        </>
      )}
    </StorePage>
  )
}
