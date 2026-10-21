import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { ApiError } from '@/api/client'
import { buyerApi } from '@/api/endpoints'
import type { OrderResponse } from '@/api/types'
import { describeError } from '@/api/errors'
import { newId } from '@/lib/id'

export const ordersKey = ['buyer', 'orders'] as const
const orderKey = (id: string) => [...ordersKey, 'detail', id] as const
export const invoicesKey = ['buyer', 'invoices'] as const

export function useMyOrders(page: number) {
  return useQuery({
    queryKey: [...ordersKey, 'list', page],
    queryFn: () => buyerApi.orders({ page, size: 10 }),
    placeholderData: keepPreviousData,
  })
}

export function useMyOrder(orderId: string) {
  return useQuery({
    queryKey: orderKey(orderId),
    queryFn: () => buyerApi.order(orderId),
    retry: (count, error) => !(error instanceof ApiError && [403, 404].includes(error.status)) && count < 2,
  })
}

/** 404 here only means "not issued yet", which is a normal state of a fresh order, not an error. */
export function useInvoiceOf(orderId: string, enabled: boolean) {
  return useQuery({
    queryKey: [...invoicesKey, 'of', orderId],
    queryFn: async () => {
      try {
        return await buyerApi.invoiceOf(orderId)
      } catch (error) {
        if (error instanceof ApiError && error.status === 404) return null
        throw error
      }
    },
    enabled,
  })
}

export function useMyInvoices(page: number) {
  return useQuery({
    queryKey: [...invoicesKey, 'list', page],
    queryFn: () => buyerApi.invoices({ page, size: 10 }),
    placeholderData: keepPreviousData,
  })
}

export function useReturnsOf(orderId: string, enabled: boolean) {
  return useQuery({
    queryKey: [...ordersKey, 'returns', orderId],
    queryFn: () => buyerApi.returnsOf(orderId, { size: 200 }),
    enabled,
  })
}

function useStoreOrder() {
  const queryClient = useQueryClient()
  return (order: OrderResponse) => {
    queryClient.setQueryData(orderKey(order.id), order)
    void queryClient.invalidateQueries({ queryKey: [...ordersKey, 'list'] })
  }
}

export function usePayOrder() {
  const store = useStoreOrder()
  return useMutation({
    mutationFn: buyerApi.pay,
    onSuccess: (order) => {
      store(order)
      toast.success('pago confirmado; tu pedido entra en preparación')
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function useRequestReturn(orderId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (vars: { orderLineId: string; quantity: number }) =>
      buyerApi.requestReturn({ returnId: newId(), orderId, ...vars }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: [...ordersKey, 'returns', orderId] })
      toast.success('registramos tu solicitud de devolución')
    },
    onError: (error) => toast.error(describeError(error)),
  })
}
