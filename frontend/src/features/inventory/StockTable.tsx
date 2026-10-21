import type { ReactNode } from 'react'
import type { InventoryItemResponse } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { shortId } from '@/lib/id'
import { conditionLabel } from '@/lib/labels'

interface StockTableProps {
  items: InventoryItemResponse[]
  caption: string
  /** Which reference column to show: the warehouse (seller view) or the product (warehouse view). */
  reference: 'warehouse' | 'product'
  productName?: (productId: string) => string
  renderActions?: (item: InventoryItemResponse) => ReactNode
}

export function StockTable({ items, caption, reference, productName, renderActions }: StockTableProps) {
  return (
    <div className="relative overflow-x-auto rounded-[var(--radius-card)] bg-card ring-1 ring-line">
      <table className="w-full min-w-[36rem] text-left text-[0.95rem]">
        <caption className="sr-only">{caption}</caption>
        <thead className="border-b border-line text-sm text-ink-soft">
          <tr>
            <th scope="col" className="px-4 py-3 font-semibold">Registro</th>
            <th scope="col" className="px-4 py-3 font-semibold">{reference === 'warehouse' ? 'Bodega' : 'Producto'}</th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">Disponible</th>
            <th scope="col" className="px-4 py-3 text-right font-semibold">Reservado</th>
            <th scope="col" className="px-4 py-3 font-semibold">Estado</th>
            {renderActions && (
              <th scope="col" className="px-4 py-3 text-right font-semibold">
                <span className="sr-only">Acciones</span>
              </th>
            )}
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {items.map((item) => (
            <tr key={item.id}>
              <td className="px-4 py-3 font-mono text-sm font-semibold" title={item.id}>
                #{shortId(item.id)}
              </td>
              <td className="px-4 py-3" title={reference === 'warehouse' ? item.warehouseId : item.productId}>
                {reference === 'warehouse'
                  ? `bodega #${shortId(item.warehouseId)}`
                  : (productName?.(item.productId) ?? `producto #${shortId(item.productId)}`)}
              </td>
              <td className="px-4 py-3 text-right font-semibold tabular-nums">{item.availableQuantity}</td>
              <td className="px-4 py-3 text-right tabular-nums text-ink-soft">{item.reservedQuantity}</td>
              <td className="px-4 py-3">
                <Badge tone={item.condition === 'DAMAGED' ? 'coral' : 'mint'}>{conditionLabel[item.condition]}</Badge>
              </td>
              {renderActions && <td className="px-4 py-3 text-right">{renderActions(item)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
