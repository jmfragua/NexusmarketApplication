import { useMemo, useState, type FormEvent } from 'react'
import type { InventoryItemResponse } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingRows, Notice } from '@/components/ui/States'
import { useProductLookup } from '@/features/catalog/useProductLookup'
import { fallbackTitle, readProduct } from '@/features/catalog/variant'
import { QuantityDialog } from '@/features/inventory/QuantityDialog'
import { StockTable } from '@/features/inventory/StockTable'
import { WarehouseList } from '@/features/inventory/WarehouseList'
import { shortId } from '@/lib/id'
import { useReceive, useRestockReturn, useWarehouses, useWarehouseStock } from './queries'

function RestockReturnForm({ items }: { items: InventoryItemResponse[] }) {
  const restock = useRestockReturn()
  const [returnId, setReturnId] = useState('')
  const [itemId, setItemId] = useState(items[0]?.id ?? '')
  const [error, setError] = useState<string | null>(null)
  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (returnId.trim() === '') {
      setError('pega el identificador de la devolución')
      return
    }
    setError(null)
    restock.mutate({ returnId: returnId.trim(), inventoryItemId: itemId || (items[0]?.id ?? '') }, { onSuccess: () => setReturnId('') })
  }
  return (
    <form onSubmit={submit} noValidate className="grid gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:grid-cols-[1fr_1fr_auto] sm:items-end">
      <Field label="Devolución" error={error ?? undefined}>
        <Input value={returnId} onChange={(e) => setReturnId(e.target.value)} placeholder="Identificador de la devolución" />
      </Field>
      <Field label="Registro donde reintegrar">
        <Select value={itemId} onChange={(e) => setItemId(e.target.value)}>
          {items.map((i) => (
            <option key={i.id} value={i.id}>
              #{shortId(i.id)} · {i.availableQuantity} disponibles
            </option>
          ))}
        </Select>
      </Field>
      <Button type="submit" loading={restock.isPending}>
        Reintegrar
      </Button>
    </form>
  )
}

export function LogisticsWarehousesPage() {
  const warehouses = useWarehouses()
  const [selected, setSelected] = useState<string | null>(null)
  const stock = useWarehouseStock(selected)
  const receive = useReceive()
  const [receiving, setReceiving] = useState<InventoryItemResponse | null>(null)
  const ids = useMemo(() => stock.data?.content.map((i) => i.productId) ?? [], [stock.data])
  const lookup = useProductLookup(ids)
  const productName = (id: string) => {
    const raw = lookup.products.get(id)
    return raw ? readProduct(raw).title : fallbackTitle(id)
  }

  return (
    <>
      <PageHeader eyebrow="Operación logística" title="bodegas" description="todas las bodegas del marketplace y de los vendedores. Elige una para ver sus existencias." />
      {warehouses.isPending ? (
        <LoadingRows rows={2} label="cargando bodegas" />
      ) : warehouses.error ? (
        <ErrorState error={warehouses.error} onRetry={() => void warehouses.refetch()} />
      ) : warehouses.data.content.length === 0 ? (
        <EmptyState title="no hay bodegas registradas" description="el administrador registra las bodegas del marketplace; las de vendedores se crean con cada vendedor." />
      ) : (
        <WarehouseList
          warehouses={warehouses.data.content}
          renderAction={(w) => (
            <Button size="sm" variant={selected === w.id ? 'primary' : 'outline'} aria-pressed={selected === w.id} onClick={() => setSelected(w.id)}>
              Ver existencias
            </Button>
          )}
        />
      )}

      {selected && (
        <section className="mt-10 grid grid-cols-1 gap-5" aria-labelledby="stock-title">
          <h2 id="stock-title" className="text-2xl font-bold">
            existencias de la bodega #{shortId(selected)}
          </h2>
          {stock.isPending ? (
            <LoadingRows rows={2} label="cargando existencias" />
          ) : stock.error ? (
            <ErrorState error={stock.error} onRetry={() => void stock.refetch()} />
          ) : stock.data.content.length === 0 ? (
            <EmptyState title="esta bodega no tiene existencias registradas" description="el backend aún no ofrece una operación para crear el registro de un producto en una bodega." />
          ) : (
            <>
              <StockTable
                caption="Existencias de la bodega"
                reference="product"
                productName={productName}
                items={stock.data.content}
                renderActions={(item) => (
                  <Button size="sm" variant="outline" onClick={() => setReceiving(item)}>
                    Ingreso
                  </Button>
                )}
              />
              <div>
                <h3 className="mb-2 text-lg font-bold">reintegrar una devolución</h3>
                <RestockReturnForm items={stock.data.content} />
              </div>
            </>
          )}
          <Notice tone="cobalt" title="sin cifra global">el inventario es distribuido: no existe un total que sume todas las bodegas.</Notice>
        </section>
      )}

      <QuantityDialog
        key={receiving?.id ?? 'none'}
        open={receiving !== null}
        onOpenChange={(v) => !v && setReceiving(null)}
        title="registrar ingreso"
        description={receiving ? `${productName(receiving.productId)} · hoy hay ${receiving.availableQuantity} disponibles` : ''}
        label="Unidades que ingresan"
        submitLabel="Registrar ingreso"
        pending={receive.isPending}
        onSubmit={(quantity) => receiving && receive.mutate({ inventoryItemId: receiving.id, quantity }, { onSuccess: () => setReceiving(null) })}
      />
    </>
  )
}
