import type { ReactNode } from 'react'
import type { OrderResponse } from '@/api/types'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { OrderStatusBadge } from './OrderStatusBadge'

export function OrderTable({ orders, caption, renderAction }: { orders: OrderResponse[]; caption: string; renderAction?: (o: OrderResponse) => ReactNode }) {
  return (
    <div className="relative overflow-x-auto rounded-[var(--radius-card)] bg-card ring-1 ring-line">
      <table className="w-full min-w-[40rem] text-left text-[0.95rem]">
        <caption className="sr-only">{caption}</caption>
        <thead className="border-b border-line text-sm text-ink-soft">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Pedido</th>
            <th scope="col" className="px-4 py-3 font-semibold">Estado</th>
            <th scope="col" className="px-4 py-3 font-semibold">Entrega</th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">Unidades</th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">Total</th>
            {renderAction && (
              <th scope="col" className="px-4 py-3">
                <span className="sr-only">Acciones</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {orders.map((o) => (
            <tr key={o.id}>
              <td className="px-4 py-3 font-mono text-sm font-semibold" title={o.id}>
                #{shortId(o.id)}
              </td>
              <td className="px-4 py-3">
                <OrderStatusBadge status={o.status} />
              </td>
              <td className="max-w-[16rem] truncate px-4 py-3 text-ink-soft" title={o.deliveryAddress}>
                {o.deliveryAddress}
              </td>
              <td className="px-4 py-3 text-right tabular-nums">{o.lines.reduce((s, l) => s + l.quantity, 0)}</td>
              <td className="px-4 py-3 text-right font-semibold tabular-nums">{formatMoney(o.totalAmount, o.currency)}</td>
              {renderAction && <td className="px-4 py-3 text-right">{renderAction(o)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
