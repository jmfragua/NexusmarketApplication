import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { describeError } from '@/api/errors'
import { sellerApi } from '@/api/endpoints'
import type { ProductResponse, ProductType } from '@/api/types'
import { catalogKeys } from '@/features/catalog/queries'
import { newId } from '@/lib/id'

export const sellerKeys = {
  products: ['seller', 'products'] as const,
  warehouses: ['seller', 'warehouses'] as const,
  stock: (productId: string) => ['seller', 'stock', productId] as const,
}

// The API lists a seller's own catalogue but has no "get one of my products" endpoint; the whole
// catalogue (up to the 200 page maximum) is read once and the detail page picks from it.
const ALL = { page: 0, size: 200 }

export function useMyProducts() {
  return useQuery({ queryKey: sellerKeys.products, queryFn: () => sellerApi.products(ALL), placeholderData: keepPreviousData })
}

export function useMyProduct(productId: string) {
  const query = useMyProducts()
  return { ...query, product: query.data?.content.find((p) => p.id === productId) ?? null }
}

export function useMyWarehouses() {
  return useQuery({ queryKey: sellerKeys.warehouses, queryFn: () => sellerApi.warehouses(ALL) })
}

export function useStockOf(productId: string) {
  return useQuery({ queryKey: sellerKeys.stock(productId), queryFn: () => sellerApi.stockOf(productId, ALL) })
}

/** Writes the product the backend returned into the seller list and drops the public copy. */
function useStoreProduct() {
  const queryClient = useQueryClient()
  return (product: ProductResponse) => {
    queryClient.setQueryData(sellerKeys.products, (prev: { content: ProductResponse[] } | undefined) =>
      prev
        ? { ...prev, content: prev.content.some((p) => p.id === product.id) ? prev.content.map((p) => (p.id === product.id ? product : p)) : [product, ...prev.content] }
        : prev,
    )
    void queryClient.invalidateQueries({ queryKey: sellerKeys.products })
    void queryClient.invalidateQueries({ queryKey: catalogKeys.all })
    queryClient.removeQueries({ queryKey: catalogKeys.product(product.id) })
  }
}

function useProductMutation<V>(fn: (vars: V) => Promise<ProductResponse>, success: string) {
  const store = useStoreProduct()
  return useMutation({
    mutationFn: fn,
    onSuccess: (product) => {
      store(product)
      toast.success(success)
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function useRegisterProduct() {
  const store = useStoreProduct()
  return useMutation({
    mutationFn: async (vars: { productType: ProductType; attributes: Record<string, string> }) => {
      const productId = newId()
      await sellerApi.registerProduct({ productId, productType: vars.productType })
      // A product without variants is never visible (RD-CAT-05), so the first one goes right away.
      return sellerApi.addVariant(productId, { variantId: newId(), attributes: vars.attributes })
    },
    onSuccess: (product) => {
      store(product)
      toast.success('registramos el producto y ya está publicado')
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export const usePublish = () => useProductMutation(sellerApi.publish, 'el producto volvió al catálogo')
export const useSuspend = () => useProductMutation(sellerApi.suspend, 'suspendimos el producto; ya no aparece en el catálogo')
export const useDiscontinue = () => useProductMutation(sellerApi.discontinue, 'descontinuamos el producto de forma definitiva')

export function useAddVariant(productId: string) {
  return useProductMutation(
    (attributes: Record<string, string>) => sellerApi.addVariant(productId, { variantId: newId(), attributes }),
    'agregamos la variante',
  )
}

export function useRemoveVariant(productId: string) {
  return useProductMutation((variantId: string) => sellerApi.removeVariant(productId, variantId), 'retiramos la variante')
}

function useStockMutation<V, R>(productId: string, fn: (vars: V) => Promise<R>, success: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: sellerKeys.stock(productId) })
      toast.success(success)
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function useReceiveStock(productId: string) {
  return useStockMutation(
    productId,
    (vars: { inventoryItemId: string; quantity: number }) => sellerApi.receive({ ...vars, movementId: newId() }),
    'registramos el ingreso',
  )
}

export function useAdjustStock(productId: string) {
  return useStockMutation(
    productId,
    (vars: { inventoryItemId: string; quantity: number }) => sellerApi.adjust({ ...vars, movementId: newId() }),
    'ajustamos las existencias',
  )
}

export function useMarkDamaged(productId: string) {
  return useStockMutation(productId, (inventoryItemId: string) => sellerApi.markDamaged(inventoryItemId), 'marcamos las existencias como dañadas')
}
