import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { describeError } from '@/api/errors'
import { logisticsApi } from '@/api/endpoints'
import type { OrderStatus } from '@/api/types'
import { newId } from '@/lib/id'
import { readJson, writeJson } from '@/lib/storage'

export const logisticsKeys = {
  orders: (status: OrderStatus) => ['logistics', 'orders', status] as const,
  allOrders: ['logistics', 'orders'] as const,
  warehouses: ['logistics', 'warehouses'] as const,
  stock: (warehouseId: string) => ['logistics', 'stock', warehouseId] as const,
  allStock: ['logistics', 'stock'] as const,
}

/**
 * The API exposes no shipment lookup, so the shipment id is derived from the order: one shipment
 * per order, and dispatching or delivering always finds the shipment that was packed.
 */
export const shipmentIdOf = (orderId: string) => `shp-${orderId}`

const PACKED_KEY = 'nexusmarket.logistics.packed'
const isStringArray = (v: unknown): v is string[] => Array.isArray(v) && v.every((x) => typeof x === 'string')
export const readPacked = (): string[] => readJson(PACKED_KEY, isStringArray) ?? []
const rememberPacked = (orderId: string) => writeJson(PACKED_KEY, [...new Set([...readPacked(), orderId])].slice(-200))

export function useOrdersByStatus(status: OrderStatus, page: number) {
  return useQuery({
    queryKey: [...logisticsKeys.orders(status), page],
    queryFn: () => logisticsApi.ordersByStatus(status, { page, size: 10 }),
    placeholderData: keepPreviousData,
  })
}

export function useWarehouses() {
  return useQuery({ queryKey: logisticsKeys.warehouses, queryFn: () => logisticsApi.warehouses({ size: 200 }) })
}

export function useWarehouseStock(warehouseId: string | null) {
  return useQuery({
    queryKey: logisticsKeys.stock(warehouseId ?? ''),
    queryFn: () => logisticsApi.stockOf(warehouseId ?? '', { size: 200 }),
    enabled: warehouseId !== null && warehouseId !== '',
  })
}

function useOpsMutation<V, R>(fn: (vars: V) => Promise<R>, success: string, after?: (vars: V) => void) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (_result, vars) => {
      after?.(vars)
      void queryClient.invalidateQueries({ queryKey: logisticsKeys.allOrders })
      void queryClient.invalidateQueries({ queryKey: logisticsKeys.allStock })
      toast.success(success)
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function usePack() {
  return useOpsMutation(
    (vars: { orderId: string; warehouseId: string; deliveryAddress: string }) =>
      logisticsApi.pack({ shipmentId: shipmentIdOf(vars.orderId), ...vars }),
    'pedido empacado; ya se puede despachar',
    (vars) => rememberPacked(vars.orderId),
  )
}

export function useDispatch() {
  return useOpsMutation(
    (orderId: string) => logisticsApi.dispatch({ shipmentId: shipmentIdOf(orderId), orderId }),
    'pedido despachado',
  )
}

export function useDeliver() {
  return useOpsMutation(
    (orderId: string) => logisticsApi.deliver({ shipmentId: shipmentIdOf(orderId), orderId }),
    'entrega confirmada',
  )
}

type LineMovement = { orderId: string; orderLineId: string; inventoryItemId: string }

export const useReserve = () => useOpsMutation((v: LineMovement) => logisticsApi.reserve({ ...v, movementId: newId() }), 'reservamos las existencias de la línea')
export const useRelease = () => useOpsMutation((v: LineMovement) => logisticsApi.release({ ...v, movementId: newId() }), 'liberamos la reserva')
export const useIssueForSale = () => useOpsMutation((v: LineMovement) => logisticsApi.issueForSale({ ...v, movementId: newId() }), 'registramos la salida por venta')

export function useReceive() {
  return useOpsMutation(
    (vars: { inventoryItemId: string; quantity: number }) => logisticsApi.receive({ ...vars, movementId: newId() }),
    'registramos el ingreso',
  )
}

export function useRestockReturn() {
  return useOpsMutation(
    (vars: { returnId: string; inventoryItemId: string }) => logisticsApi.restockReturn(vars.returnId, vars.inventoryItemId, newId()),
    'reintegramos la devolución al inventario',
  )
}
