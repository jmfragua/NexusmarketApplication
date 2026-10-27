import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { describeError } from '@/api/errors'
import { adminApi } from '@/api/endpoints'
import type { InvoiceResponse, RegisterStaffRequest, Role, WarehouseResponse } from '@/api/types'
import { newId } from '@/lib/id'
import { readJson, writeJson } from '@/lib/storage'

export const adminKeys = {
  sellers: ['admin', 'sellers'] as const,
  refunds: ['admin', 'refunds'] as const,
}

// The admin API can issue invoices and register warehouses but cannot list them back, so what
// this browser created is remembered locally to offer it again (e.g. the invoice of a refund).
const INVOICES_KEY = 'nexusmarket.admin.invoices'
const WAREHOUSES_KEY = 'nexusmarket.admin.warehouses'

const isInvoiceList = (v: unknown): v is InvoiceResponse[] =>
  Array.isArray(v) && v.every((x) => typeof x === 'object' && x !== null && typeof (x as InvoiceResponse).id === 'string' && typeof (x as InvoiceResponse).orderId === 'string')
const isWarehouseList = (v: unknown): v is WarehouseResponse[] =>
  Array.isArray(v) && v.every((x) => typeof x === 'object' && x !== null && typeof (x as WarehouseResponse).id === 'string')

export const readIssuedInvoices = () => readJson(INVOICES_KEY, isInvoiceList) ?? []
export const readRegisteredWarehouses = () => readJson(WAREHOUSES_KEY, isWarehouseList) ?? []

export function useSellers(page: number) {
  return useQuery({
    queryKey: [...adminKeys.sellers, page],
    queryFn: () => adminApi.sellers({ page, size: 10 }),
    placeholderData: keepPreviousData,
  })
}

export function useRefunds(page: number) {
  return useQuery({
    queryKey: [...adminKeys.refunds, page],
    queryFn: () => adminApi.refunds({ page, size: 10 }),
    placeholderData: keepPreviousData,
  })
}

function useAdminMutation<V, R>(fn: (vars: V) => Promise<R>, success: string | ((r: R) => string), invalidate: readonly string[] = []) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (result) => {
      if (invalidate.length) void queryClient.invalidateQueries({ queryKey: invalidate })
      toast.success(typeof success === 'string' ? success : success(result))
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function useRegisterSeller() {
  return useAdminMutation(
    (v: { fullName: string; email: string; identityDocument: string; password: string }) =>
      adminApi.registerSeller({ ...v, sellerId: newId(), firstWarehouseId: newId() }),
    (s) => `incorporamos a ${s.fullName} con su primera bodega`,
    adminKeys.sellers,
  )
}

/** Incorporates a logistics operator or a supervisor; the form shows its own errors, so no toast. */
export function useRegisterStaff() {
  return useMutation({
    mutationFn: (v: Omit<RegisterStaffRequest, 'userId'>) => adminApi.registerStaff({ ...v, userId: newId() }),
    onSuccess: (user) => toast.success(`creamos la cuenta de ${user.fullName}`),
  })
}

export const useBlockUser = () => useAdminMutation(adminApi.block, (u) => `${u.fullName} quedó bloqueado`, adminKeys.sellers)
export const useActivateUser = () => useAdminMutation(adminApi.activate, (u) => `${u.fullName} está activo de nuevo`, adminKeys.sellers)
export const useChangeRole = () =>
  useAdminMutation((v: { userId: string; role: Role }) => adminApi.changeRole(v.userId, { role: v.role }), 'cambiamos el rol')

export function useRegisterWarehouse() {
  return useAdminMutation(
    async () => {
      const warehouse = await adminApi.registerWarehouse({ warehouseId: newId() })
      writeJson(WAREHOUSES_KEY, [warehouse, ...readRegisteredWarehouses()].slice(0, 50))
      return warehouse
    },
    'registramos la bodega del marketplace',
  )
}

export function useIssueInvoice() {
  return useAdminMutation(
    async (orderId: string) => {
      const invoice = await adminApi.issueInvoice({ invoiceId: newId(), orderId })
      writeJson(INVOICES_KEY, [invoice, ...readIssuedInvoices().filter((i) => i.orderId !== orderId)].slice(0, 100))
      return invoice
    },
    'factura emitida',
  )
}

export function useIssueRefund() {
  return useAdminMutation(
    (v: { productReturnId: string; invoiceId: string; amount: number; currency: string }) => adminApi.issueRefund({ ...v, refundId: newId() }),
    'reembolso emitido',
    adminKeys.refunds,
  )
}
