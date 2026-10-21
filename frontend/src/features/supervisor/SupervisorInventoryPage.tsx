import { useState, type FormEvent } from 'react'
import { Badge, type Tone } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { StockTable } from '@/features/inventory/StockTable'
import type { MovementType } from '@/api/types'
import { shortId } from '@/lib/id'
import { movementLabel } from '@/lib/labels'
import { useDamagedStock, useMovements, useWarehouseStock } from './queries'

const movementTone: Record<MovementType, Tone> = { INBOUND: 'mint', RESERVATION: 'cobalt', SALE_OUTBOUND: 'lime', ADJUSTMENT: 'amber', RETURN: 'coral' }

export function SupervisorInventoryPage() {
  const [movementPage, setMovementPage] = useState(0)
  const [damagedPage, setDamagedPage] = useState(0)
  const movements = useMovements(movementPage)
  const damaged = useDamagedStock(damagedPage)
  const [draft, setDraft] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const stock = useWarehouseStock(warehouseId)

  const lookup = (e: FormEvent) => {
    e.preventDefault()
    setWarehouseId(draft.trim())
  }

  return (
    <>
      <PageHeader eyebrow="Supervisión · solo lectura" title="inventario" description="trazabilidad de movimientos, existencias dañadas y existencias por bodega." />

      <section aria-labelledby="movimientos">
        <h2 id="movimientos" className="mb-3 text-2xl font-bold">
          movimientos
        </h2>
        {movements.isPending ? (
          <LoadingRows label="cargando movimientos" />
        ) : movements.error ? (
          <ErrorState error={movements.error} onRetry={() => void movements.refetch()} />
        ) : movements.data.content.length === 0 ? (
          <EmptyState title="aún no hay movimientos de inventario" />
        ) : (
          <>
            <div className="relative overflow-x-auto rounded-[var(--radius-card)] bg-card ring-1 ring-line">
              <table className="w-full min-w-[30rem] text-left text-[0.95rem]">
                <caption className="sr-only">Movimientos de inventario</caption>
                <thead className="border-b border-line text-sm text-ink-soft">
                  <tr>
                    <th scope="col" className="px-4 py-3 font-semibold">Movimiento</th>
                    <th scope="col" className="px-4 py-3 font-semibold">Tipo</th>
                    <th scope="col" className="px-4 py-3 font-semibold">Registro</th>
                    <th scope="col" className="px-4 py-3 text-right font-semibold">Cantidad</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {movements.data.content.map((m) => (
                    <tr key={m.id}>
                      <td className="px-4 py-3 font-mono text-sm">#{shortId(m.id)}</td>
                      <td className="px-4 py-3">
                        <Badge tone={movementTone[m.movementType]}>{movementLabel[m.movementType]}</Badge>
                      </td>
                      <td className="px-4 py-3 font-mono text-sm" title={m.inventoryItemId}>
                        #{shortId(m.inventoryItemId)}
                      </td>
                      <td className="px-4 py-3 text-right font-semibold tabular-nums">{m.quantity}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={movements.data} onChange={setMovementPage} />
          </>
        )}
      </section>

      <section aria-labelledby="danadas" className="mt-10">
        <h2 id="danadas" className="mb-3 text-2xl font-bold">
          existencias dañadas
        </h2>
        {damaged.isPending ? (
          <LoadingRows rows={2} label="cargando existencias dañadas" />
        ) : damaged.error ? (
          <ErrorState error={damaged.error} onRetry={() => void damaged.refetch()} />
        ) : damaged.data.content.length === 0 ? (
          <EmptyState title="no hay existencias dañadas" description="ningún registro está excluido de las reservas." />
        ) : (
          <>
            <StockTable caption="Existencias dañadas" reference="product" items={damaged.data.content} />
            <Pagination page={damaged.data} onChange={setDamagedPage} />
          </>
        )}
      </section>

      <section aria-labelledby="porbodega" className="mt-10">
        <h2 id="porbodega" className="mb-3 text-2xl font-bold">
          existencias por bodega
        </h2>
        <form onSubmit={lookup} className="mb-4 grid gap-3 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:grid-cols-[1fr_auto] sm:items-end">
          <Field label="Identificador de la bodega" hint="no hay una cifra global: el inventario se consulta bodega por bodega.">
            <Input value={draft} onChange={(e) => setDraft(e.target.value)} autoComplete="off" />
          </Field>
          <Button type="submit" variant="outline">
            Consultar
          </Button>
        </form>
        {warehouseId !== '' &&
          (stock.isPending ? (
            <LoadingRows rows={2} label="cargando existencias de la bodega" />
          ) : stock.error ? (
            <ErrorState error={stock.error} onRetry={() => void stock.refetch()} />
          ) : stock.data.content.length === 0 ? (
            <EmptyState title="esta bodega no tiene existencias registradas" />
          ) : (
            <StockTable caption={`Existencias de la bodega ${warehouseId}`} reference="product" items={stock.data.content} />
          ))}
      </section>
    </>
  )
}
