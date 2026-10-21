import type { CartItemResponse, OrderLineRequest, ProductResponse } from '@/api/types'
import { readProduct, variantLabel, type ProductView, type VariantView } from '@/features/catalog/variant'
import { roundMoney } from '@/lib/money'

export type LineProblem = 'unavailable' | 'variant-missing' | 'no-price'

export interface PricedLine {
  item: CartItemResponse
  product: ProductView | null
  variant: VariantView | null
  title: string
  variantText: string
  unitPrice: number | null
  lineAmount: number | null
  currency: string | null
  problem: LineProblem | null
}

export interface CartTotals {
  lines: PricedLine[]
  total: number
  currency: string | null
  itemCount: number
  /** Why the cart cannot be confirmed right now, or null when it can. */
  blocker: string | null
}

/**
 * Prices each cart line from the `price` attribute of its variant. `products` holds what the
 * public catalogue answered per product id; `null` means the product is no longer visible.
 */
export function priceCart(
  items: CartItemResponse[],
  products: Map<string, ProductResponse | null>,
): CartTotals {
  const lines = items.map<PricedLine>((item) => {
    const raw = products.get(item.productId)
    if (!raw) {
      return {
        item,
        product: null,
        variant: null,
        title: 'Producto no disponible',
        variantText: '',
        unitPrice: null,
        lineAmount: null,
        currency: null,
        problem: 'unavailable',
      }
    }
    const product = readProduct(raw)
    const variant = product.variants.find((v) => v.id === item.productVariantId) ?? null
    if (!variant) {
      return {
        item,
        product,
        variant: null,
        title: product.title,
        variantText: 'variante retirada',
        unitPrice: null,
        lineAmount: null,
        currency: null,
        problem: 'variant-missing',
      }
    }
    const unitPrice = variant.price
    return {
      item,
      product,
      variant,
      title: variant.name ?? product.title,
      variantText: variantLabel(variant),
      unitPrice,
      lineAmount: unitPrice === null ? null : roundMoney(unitPrice * item.quantity),
      currency: variant.currency,
      problem: unitPrice === null ? 'no-price' : null,
    }
  })

  const currencies = new Set(lines.flatMap((l) => (l.currency ? [l.currency] : [])))
  const total = roundMoney(lines.reduce((sum, l) => sum + (l.lineAmount ?? 0), 0))
  const itemCount = items.reduce((sum, i) => sum + i.quantity, 0)

  let blocker: string | null = null
  if (items.length === 0) blocker = 'tu carrito está vacío'
  else if (lines.some((l) => l.problem === 'unavailable' || l.problem === 'variant-missing'))
    blocker = 'quita los productos que ya no están disponibles para continuar'
  else if (lines.some((l) => l.problem === 'no-price'))
    blocker = 'hay productos sin precio publicado; quítalos para continuar'
  else if (currencies.size > 1) blocker = 'no se pueden pagar juntos productos en monedas distintas'

  return {
    lines,
    total,
    currency: currencies.size === 1 ? ([...currencies][0] ?? null) : null,
    itemCount,
    blocker,
  }
}

/** One priced order line per cart line, matching product, variant and quantity exactly. */
export function toOrderLines(totals: CartTotals, newId: () => string): OrderLineRequest[] {
  if (totals.blocker) throw new Error(totals.blocker)
  return totals.lines.map((line) => ({
    orderLineId: newId(),
    productId: line.item.productId,
    productVariantId: line.item.productVariantId,
    quantity: line.item.quantity,
    lineAmount: line.lineAmount ?? 0,
    currency: line.currency ?? '',
  }))
}
