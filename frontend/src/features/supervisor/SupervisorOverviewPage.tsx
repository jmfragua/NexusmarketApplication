import { useState } from 'react'
import type { OrderStatus } from '@/api/types'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows, Skeleton } from '@/components/ui/States'
import { OrderStatusTabs } from '@/features/orders/OrderStatusTabs'
import { OrderTable } from '@/features/orders/OrderTable'
import { cn } from '@/lib/cn'
import { orderStatusLabel } from '@/lib/labels'
import { formatMoney } from '@/lib/money'
import { useCompletedOrders, useDamagedStock, useInvoicedTotal, useIssuedInvoices, useOrdersByStatus, useSupervisorRefunds } from './queries'

function Kpi({ label, value, loading, tone = 'card', hint }: { label: string; value: string; loading: boolean; tone?: 'card' | 'lime' | 'ink'; hint?: string }) {
  return (
    <div className={cn('grid content-between gap-3 rounded-[var(--radius-card)] p-5', tone === 'lime' ? 'bg-lime' : tone === 'ink' ? 'bg-ink text-white' : 'bg-card ring-1 ring-line')}>
      <p className={cn('text-sm font-semibold', tone === 'ink' ? 'text-white/75' : 'text-ink-soft')}>{label}</p>
      {loading ? <Skeleton className="h-9 w-24" /> : <p className={cn('font-display text-[clamp(1.6rem,3vw,2.2rem)] leading-none font-extrabold tabular-nums', tone === 'ink' && 'text-white')}>{value}</p>}
      {hint && <p className={cn('text-xs', tone === 'ink' ? 'text-white/60' : 'text-ink-soft')}>{hint}</p>}
    </div>
  )
}

const statuses: OrderStatus[] = ['PENDING_PAYMENT', 'PAID', 'DISPATCHED', 'DELIVERED']

const refundsHint = (n: number) => (n === 1 ? '1 reembolso emitido' : `${n} reembolsos emitidos`)

export function SupervisorOverviewPage() {
  const total = useInvoicedTotal()
  const completed = useCompletedOrders(0, 1)
  const invoices = useIssuedInvoices(0, 1)
  const damaged = useDamagedStock(0, 1)
  const refunds = useSupervisorRefunds(0, 1)
  const [status, setStatus] = useState<OrderStatus>('PAID')
  const [page, setPage] = useState(0)
  const orders = useOrdersByStatus(status, page)

  const totalText = total.data
    ? Object.entries(total.data)
        .map(([currency, amount]) => formatMoney(amount, currency))
        .join(' · ')
    : 'sin facturación'

  return (
    <>
      <PageHeader eyebrow="Supervisión · solo lectura" title="resumen operativo" description="lo que pasa en el marketplace, sin poder modificar nada." />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
        <div className="sm:col-span-2">
          <Kpi tone="ink" label="Total facturado" value={total.error ? 'no disponible' : totalText} loading={total.isPending} hint="suma de todas las facturas emitidas" />
        </div>
        <Kpi tone="lime" label="Pedidos entregados" value={String(completed.data?.totalElements ?? '—')} loading={completed.isPending} />
        <Kpi label="Facturas emitidas" value={String(invoices.data?.totalElements ?? '—')} loading={invoices.isPending} />
        <Kpi label="Existencias dañadas" value={String(damaged.data?.totalElements ?? '—')} loading={damaged.isPending} hint={refundsHint(refunds.data?.totalElements ?? 0)} />
      </div>

      <section className="mt-10" aria-labelledby="pedidos">
        <h2 id="pedidos" className="mb-4 text-2xl font-bold">
          pedidos por estado
        </h2>
        <OrderStatusTabs
          statuses={statuses}
          value={status}
          onChange={(s) => {
            setStatus(s)
            setPage(0)
          }}
        />
        {orders.isPending ? (
          <LoadingRows label="cargando pedidos" />
        ) : orders.error ? (
          <ErrorState error={orders.error} onRetry={() => void orders.refetch()} />
        ) : orders.data.content.length === 0 ? (
          <EmptyState title={`no hay pedidos en estado ${orderStatusLabel[status].toLowerCase()}`} />
        ) : (
          <>
            <OrderTable orders={orders.data.content} caption={`Pedidos ${orderStatusLabel[status]}`} />
            <Pagination page={orders.data} onChange={setPage} />
          </>
        )}
      </section>
    </>
  )
}
