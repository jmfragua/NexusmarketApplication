import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo } from 'react'
import { toast } from 'sonner'
import { buyerApi } from '@/api/endpoints'
import type { CartResponse } from '@/api/types'
import { describeError } from '@/api/errors'
import { useSession } from '@/features/auth/session'
import { useProductLookup } from '@/features/catalog/useProductLookup'
import { newId } from '@/lib/id'
import { priceCart } from './totals'

export const cartKey = ['buyer', 'cart'] as const

export function useCart() {
  const { session } = useSession()
  return useQuery({ queryKey: cartKey, queryFn: buyerApi.cart, enabled: session?.role === 'BUYER' })
}

export function usePricedCart(cart: CartResponse | undefined) {
  const ids = useMemo(() => cart?.items.map((i) => i.productId) ?? [], [cart])
  const { products, loading, error } = useProductLookup(ids)
  const totals = useMemo(() => priceCart(cart?.items ?? [], products), [cart, products])
  return { totals, loading, error }
}

function useCartMutation<V>(fn: (vars: V) => Promise<CartResponse>, success?: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: fn,
    onSuccess: (cart) => {
      queryClient.setQueryData(cartKey, cart)
      if (success) toast.success(success)
    },
    onError: (error) => toast.error(describeError(error)),
  })
}

export function useAddToCart() {
  return useCartMutation((vars: { productId: string; productVariantId: string; quantity: number }) =>
    buyerApi.addCartItem({ cartItemId: newId(), ...vars }),
  )
}

export function useUpdateCartItem() {
  return useCartMutation((vars: { cartItemId: string; quantity: number }) =>
    buyerApi.updateCartItem(vars.cartItemId, vars.quantity),
  )
}

export function useRemoveCartItem() {
  return useCartMutation((cartItemId: string) => buyerApi.removeCartItem(cartItemId), 'quitamos el producto del carrito')
}

export function useClearCart() {
  return useCartMutation(() => buyerApi.clearCart(), 'vaciamos tu carrito')
}
