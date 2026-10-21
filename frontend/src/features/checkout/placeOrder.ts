import { ApiError } from '@/api/client'
import { buyerApi } from '@/api/endpoints'
import type { BuyerResponse, OrderResponse } from '@/api/types'
import { toOrderLines, type CartTotals } from '@/features/cart/totals'
import { newId } from '@/lib/id'

export type CheckoutStep = 'address' | 'order' | 'payment'

export class CheckoutError extends Error {
  readonly step: CheckoutStep
  override readonly cause: unknown
  /** Set when the order exists but the payment failed: it can be paid later from its page. */
  readonly order: OrderResponse | null

  constructor(step: CheckoutStep, cause: unknown, order: OrderResponse | null = null) {
    super(cause instanceof Error ? cause.message : 'checkout failed')
    this.name = 'CheckoutError'
    this.step = step
    this.cause = cause
    this.order = order
  }
}

interface PlaceOrderInput {
  address: string
  totals: CartTotals
  onStep?: (step: CheckoutStep) => void
}

/**
 * Carrito → pendiente de pago → pagado. The delivery address must already be registered for the
 * buyer, so it is registered first; the backend ignores an address it already knows.
 */
export async function placeAndPay({ address, totals, onStep }: PlaceOrderInput): Promise<{ order: OrderResponse; buyer: BuyerResponse }> {
  const lines = toOrderLines(totals, newId)

  onStep?.('address')
  let buyer: BuyerResponse
  try {
    buyer = await buyerApi.addAddress(address)
  } catch (error) {
    throw new CheckoutError('address', error)
  }

  onStep?.('order')
  let order: OrderResponse
  try {
    order = await buyerApi.confirmCart({ orderId: newId(), deliveryAddress: address, lines })
  } catch (error) {
    throw new CheckoutError('order', error)
  }

  onStep?.('payment')
  try {
    return { order: await buyerApi.pay(order.id), buyer }
  } catch (error) {
    throw new CheckoutError('payment', error instanceof ApiError ? error : new Error(String(error)), order)
  }
}
