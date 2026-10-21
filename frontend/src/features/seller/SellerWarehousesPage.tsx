import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { WarehouseList } from '@/features/inventory/WarehouseList'
import { useMyWarehouses } from './queries'

export function SellerWarehousesPage() {
  const { data, isPending, error, refetch } = useMyWarehouses()
  return (
    <>
      <PageHeader eyebrow="Vendedor" title="mis bodegas" description="la primera bodega se crea con tu cuenta; las demás las registra el equipo de NexusMarket." />
      {isPending ? (
        <LoadingRows rows={2} label="cargando tus bodegas" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState title="no tienes bodegas" description="pide al administrador que registre una bodega a tu nombre." />
      ) : (
        <WarehouseList warehouses={data.content} />
      )}
    </>
  )
}
