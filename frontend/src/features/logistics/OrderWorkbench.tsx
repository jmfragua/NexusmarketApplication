import { useMemo, useState } from 'react'
import type { InventoryItemResponse, OrderLineResponse, OrderResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Button } from '@/components/ui/Button'
import { Field, Select } from '@/components/ui/Field'
import { Notice } from '@/components/ui/States'
import { useProductLookup } from '@/features/catalog/useProductLookup'
import { fallbackTitle, readProduct, variantLabel } from '@/features/catalog/variant'
import { StatusTimeline } from '@/features/orders/StatusTimeline'
import { shortId } from '@/lib/id'
import { conditionLabel, warehouseTypeLabel } from '@/lib/labels'
import { formatMoney } from '@/lib/money'
import { readPacked, useDeliver, useDispatch, useIssueForSale, usePack, useRelease, useReserve, useWarehouses, useWarehouseStock } from './queries'

function LineStock({ order, line, title, stock }: { order: OrderResponse; line: OrderLineResponse; title: string; stock: InventoryItemResponse[] }) {
  const matches = stock.filter((s) => s.productId === line.productId)
  const [itemId, setItemId] = useState(matches[0]?.id ?? '')
  const reserve = useReserve()
  const release = useRelease()
  const outbound = useIssueForSale()
  const selected = matches.find((m) => m.id === itemId) ?? matches[0]
  const vars = selected ? { orderId: order.id, orderLineId: line.id, inventoryItemId: selected.id } : null

  return (
    <li className="grid gap-3 py-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <p className="font-semibold">
          {title} <span className="font-normal text-ink-soft">× {line.quantity}</span>
        </p>
        <span className="text-sm text-ink-soft tabular-nums">{formatMoney(line.lineAmount, line.currency)}</span>
      </div>
      {matches.length === 0 ? (
        <p className="text-sm text-ink-soft">esta bodega no tiene un registro de existencias para este producto.</p>
      ) : (
        <div className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-end">
          <Field label="Registro de existencias">
            <Select value={selected?.id ?? ''} onChange={(e) => setItemId(e.target.value)}>
              {matches.map((m) => (
                <option key={m.id} value={m.id}>
                  #{shortId(m.id)} · {m.availableQuantity} disponibles · {m.reservedQuantity} reservadas · {conditionLabel[m.condition]}
                </option>
              ))}
            </Select>
          </Field>
          <div className="flex flex-wrap gap-1.5">
            <Button size="sm" variant="outline" disabled={!vars} loading={reserve.isPending} onClick={() => vars && reserve.mutate(vars)}>
              Reservar
            </Button>
            <Button size="sm" variant="outline" disabled={!vars} loading={release.isPending} onClick={() => vars && release.mutate(vars)}>
              Liberar
            </Button>
            <Button size="sm" variant="outline" disabled={!vars} loading={outbound.isPending} onClick={() => vars && outbound.mutate(vars)}>
              Salida por venta
            </Button>
          </div>
        </div>
      )}
    </li>
  )
}

export function OrderWorkbench({ order, onDone }: { order: OrderResponse; onDone: () => void }) {
  const warehouses = useWarehouses()
  const [warehouseId, setWarehouseId] = useState<string>('')
  const chosen = warehouseId || warehouses.data?.content[0]?.id || ''
  const stock = useWarehouseStock(chosen || null)
  const pack = usePack()
  const dispatch = useDispatch()
  const deliver = useDeliver()
  const [packed, setPacked] = useState(() => readPacked().includes(order.id))

  const ids = useMemo(() => order.lines.map((l) => l.productId), [order])
  const lookup = useProductLookup(ids)
  const titleOf = (line: OrderLineResponse) => {
    const raw = lookup.products.get(line.productId)
    if (!raw) return fallbackTitle(line.productId)
    const view = readProduct(raw)
    const variant = view.variants.find((v) => v.id === line.productVariantId)
    return variant ? `${variant.name ?? view.title} · ${variantLabel(variant)}` : view.title
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-center gap-2 text-sm text-ink-soft">
        <CopyId id={order.id} label="número de pedido" />
        <span>entrega en {order.deliveryAddress}</span>
      </div>
      <StatusTimeline status={order.status} audience="staff" />

      <Field label="Bodega que atiende el pedido" hint="se usa para consultar existencias y para empacar.">
        <Select value={chosen} onChange={(e) => setWarehouseId(e.target.value)} disabled={warehouses.isPending}>
          {(warehouses.data?.content ?? []).map((w) => (
            <option key={w.id} value={w.id}>
              bodega #{shortId(w.id)} · {warehouseTypeLabel[w.warehouseType].toLowerCase()}
            </option>
          ))}
        </Select>
      </Field>

      <section aria-labelledby="lineas">
        <h3 id="lineas" className="text-lg font-bold">
          existencias por línea
        </h3>
        <p className="text-sm text-ink-soft">reservar, liberar y dar salida toman la cantidad de la línea; no se escribe a mano.</p>
        <ul className="divide-y divide-line">
          {order.lines.map((line) => (
            <LineStock key={`${line.id}-${chosen}-${stock.dataUpdatedAt}`} order={order} line={line} title={titleOf(line)} stock={stock.data?.content ?? []} />
          ))}
        </ul>
      </section>

      <section aria-labelledby="envio" className="grid gap-3 rounded-2xl bg-paper p-4">
        <h3 id="envio" className="text-lg font-bold">
          envío
        </h3>
        {order.status === 'PENDING_PAYMENT' && <Notice title="esperando el pago">no se puede empacar un pedido que aún no está pagado.</Notice>}
        {order.status === 'PAID' && (
          <>
            <p className="text-sm text-ink-soft">
              {packed ? 'ya está empacado; el siguiente paso es despacharlo.' : 'primero se empaca en la bodega elegida y luego se despacha.'}
            </p>
            <div className="flex flex-wrap gap-2">
              <Button
                variant={packed ? 'outline' : 'primary'}
                disabled={!chosen}
                loading={pack.isPending}
                onClick={() => pack.mutate({ orderId: order.id, warehouseId: chosen, deliveryAddress: order.deliveryAddress }, { onSuccess: () => setPacked(true) })}
              >
                {packed ? 'Empacar de nuevo' : 'Empacar'}
              </Button>
              <Button variant={packed ? 'primary' : 'outline'} loading={dispatch.isPending} onClick={() => dispatch.mutate(order.id, { onSuccess: onDone })}>
                Despachar
              </Button>
            </div>
          </>
        )}
        {order.status === 'DISPATCHED' && (
          <Button className="justify-self-start" loading={deliver.isPending} onClick={() => deliver.mutate(order.id, { onSuccess: onDone })}>
            Confirmar entrega
          </Button>
        )}
        {order.status === 'DELIVERED' && <p className="font-semibold text-mint">entregado; el ciclo del pedido terminó.</p>}
      </section>
    </div>
  )
}
