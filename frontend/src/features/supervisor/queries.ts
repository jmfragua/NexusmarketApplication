import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { supervisorApi } from '@/api/endpoints'
import type { OrderStatus } from '@/api/types'

const key = (...parts: Array<string | number>) => ['supervisor', ...parts] as const
const opts = { placeholderData: keepPreviousData }

export const useInvoicedTotal = () => useQuery({ queryKey: key('invoiced-total'), queryFn: supervisorApi.invoicedTotal })
export const useCompletedOrders = (page: number, size = 10) =>
  useQuery({ queryKey: key('completed', page, size), queryFn: () => supervisorApi.completedOrders({ page, size }), ...opts })
export const useIssuedInvoices = (page: number, size = 10) =>
  useQuery({ queryKey: key('invoices', page, size), queryFn: () => supervisorApi.invoices({ page, size }), ...opts })
export const useDamagedStock = (page: number, size = 10) =>
  useQuery({ queryKey: key('damaged', page, size), queryFn: () => supervisorApi.damagedStock({ page, size }), ...opts })
export const useMovements = (page: number, size = 15) =>
  useQuery({ queryKey: key('movements', page, size), queryFn: () => supervisorApi.movements({ page, size }), ...opts })
export const useSupervisorRefunds = (page: number, size = 10) =>
  useQuery({ queryKey: key('refunds', page, size), queryFn: () => supervisorApi.refunds({ page, size }), ...opts })
export const useOrdersByStatus = (status: OrderStatus, page: number) =>
  useQuery({ queryKey: key('orders', status, page), queryFn: () => supervisorApi.ordersByStatus(status, { page, size: 10 }), ...opts })
export const useWarehouseStock = (warehouseId: string) =>
  useQuery({
    queryKey: key('stock', warehouseId),
    queryFn: () => supervisorApi.stockOfWarehouse(warehouseId, { size: 200 }),
    enabled: warehouseId !== '',
  })
