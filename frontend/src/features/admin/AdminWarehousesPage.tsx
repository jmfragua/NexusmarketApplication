import { useState } from 'react'
import { Button } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, Notice } from '@/components/ui/States'
import { WarehouseList } from '@/features/inventory/WarehouseList'
import { readRegisteredWarehouses, useRegisterWarehouse } from './queries'

export function AdminWarehousesPage() {
  const register = useRegisterWarehouse()
  const [created, setCreated] = useState(readRegisteredWarehouses)
  return (
    <>
      <PageHeader
        eyebrow="Administración"
        title="bodegas del marketplace"
        description="las bodegas propias de la plataforma. Las de cada vendedor se crean al incorporarlo."
        actions={
          <Button loading={register.isPending} onClick={() => register.mutate(undefined, { onSuccess: () => setCreated(readRegisteredWarehouses()) })}>
            Registrar bodega
          </Button>
        }
      />
      <div className="grid gap-6">
        {created.length === 0 ? (
          <EmptyState title="aún no registras bodegas aquí" description="registra una y aparecerá en esta lista; logística la verá de inmediato." />
        ) : (
          <WarehouseList warehouses={created} />
        )}
        <Notice tone="cobalt" title="¿y las demás bodegas?">el administrador no tiene una consulta de bodegas; el operador logístico ve todas en su panel.</Notice>
      </div>
    </>
  )
}
