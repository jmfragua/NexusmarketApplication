import type { ReactNode } from 'react'
import type { WarehouseResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Badge } from '@/components/ui/Badge'
import { shortId } from '@/lib/id'
import { warehouseTypeLabel } from '@/lib/labels'

export function WarehouseList({ warehouses, renderAction }: { warehouses: WarehouseResponse[]; renderAction?: (w: WarehouseResponse) => ReactNode }) {
  return (
    <ul className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
      {warehouses.map((w) => (
        <li key={w.id} className="grid gap-3 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="font-display text-xl font-bold">bodega #{shortId(w.id)}</p>
              <Badge tone={w.warehouseType === 'MARKETPLACE' ? 'cobalt' : 'lime'} className="mt-1">
                {warehouseTypeLabel[w.warehouseType]}
              </Badge>
            </div>
            <svg viewBox="0 0 32 32" className="size-9 shrink-0 text-ink/80" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
              <path d="M4 13 16 6l12 7v13H4z" />
              <path d="M10 26v-8h12v8M10 22h12" />
            </svg>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <CopyId id={w.id} label="identificador de la bodega" />
            {renderAction?.(w)}
          </div>
        </li>
      ))}
    </ul>
  )
}
