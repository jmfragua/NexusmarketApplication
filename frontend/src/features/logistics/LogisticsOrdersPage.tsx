import { useState } from 'react'
import type { OrderResponse, OrderStatus } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { OrderStatusTabs } from '@/features/orders/OrderStatusTabs'
import { OrderTable } from '@/features/orders/OrderTable'
import { shortId } from '@/lib/id'
import { orderStatusLabel } from '@/lib/labels'
import { OrderWorkbench } from './OrderWorkbench'
import { useOrdersByStatus } from './queries'

const statuses: OrderStatus[] = ['PAID', 'DISPATCHED', 'DELIVERED', 'PENDING_PAYMENT']

const empty: Record<OrderStatus, string> = {
  CART: '',
  PENDING_PAYMENT: 'ningún pedido espera pago.',
  PAID: 'no hay pedidos pagados esperando alistamiento. Buen trabajo.',
  DISPATCHED: 'no hay pedidos en camino.',
  DELIVERED: 'aún no se ha entregado ningún pedido.',
}

export function LogisticsOrdersPage() {
  const [status, setStatus] = useState<OrderStatus>('PAID')
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState<OrderResponse | null>(null)
  const { data, isPending, error, refetch } = useOrdersByStatus(status, page)

  return (
    <>
      <PageHeader eyebrow="Operación logística" title="pedidos" description="alista, empaca, despacha y confirma entregas. Empieza por los pagados." />
      <OrderStatusTabs
        statuses={statuses}
        value={status}
        onChange={(s) => {
          setStatus(s)
          setPage(0)
        }}
      />
      {isPending ? (
        <LoadingRows label="cargando pedidos" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState title={`sin pedidos ${orderStatusLabel[status].toLowerCase()}`} description={empty[status]} />
      ) : (
        <>
          <OrderTable
            orders={data.content}
            caption={`Pedidos en estado ${orderStatusLabel[status]}`}
            renderAction={(o) => (
              <Button size="sm" variant={status === 'PAID' || status === 'DISPATCHED' ? 'primary' : 'outline'} onClick={() => setOpen(o)}>
                Gestionar
              </Button>
            )}
          />
          <Pagination page={data} onChange={setPage} />
        </>
      )}

      <Dialog wide open={open !== null} onOpenChange={(v) => !v && setOpen(null)} title={open ? `pedido #${shortId(open.id)}` : 'pedido'}>
        {open && <OrderWorkbench key={open.id} order={open} onDone={() => setOpen(null)} />}
      </Dialog>
    </>
  )
}
