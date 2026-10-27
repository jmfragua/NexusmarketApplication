import { describe, expect, it } from 'vitest'
import type { CartItemResponse, ProductResponse } from '@/api/types'
import { priceCart, toOrderLines } from './totals'

const product = (id: string, variants: Array<[string, Record<string, string>]>): ProductResponse => ({
  id,
  sellerId: 's-1',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: variants.map(([vid, attributes]) => ({ id: vid, attributes })),
})

const item = (id: string, productId: string, productVariantId: string, quantity: number): CartItemResponse => ({
  id,
  productId,
  productVariantId,
  quantity,
})

const morral = product('p-morral', [
  ['v-arena', { name: 'Morral', price: '189000', currency: 'COP', Color: 'Arena' }],
  ['v-carbon', { name: 'Morral', price: '199.000', currency: 'cop', Color: 'Carbón' }],
])
const termo = product('p-termo', [['v-coral', { name: 'Termo', price: '68500.50' }]])

describe('priceCart', () => {
  it('prices each line from the variant price attribute and adds them up', () => {
    const totals = priceCart(
      [item('c1', 'p-morral', 'v-carbon', 2), item('c2', 'p-termo', 'v-coral', 3)],
      new Map([
        ['p-morral', morral],
        ['p-termo', termo],
      ]),
    )

    expect(totals.lines.map((l) => l.lineAmount)).toEqual([398000, 205501.5])
    expect(totals.total).toBe(603501.5)
    expect(totals.currency).toBe('COP')
    expect(totals.itemCount).toBe(5)
    expect(totals.blocker).toBeNull()
    expect(totals.lines[0]?.variantText).toBe('Carbón')
  })

  it('blocks checkout when a product left the catalogue', () => {
    const totals = priceCart([item('c1', 'p-gone', 'v-1', 1)], new Map([['p-gone', null]]))
    expect(totals.lines[0]?.problem).toBe('unavailable')
    expect(totals.blocker).toMatch(/ya no están disponibles/)
  })

  it('blocks checkout when a variant has no price or was removed', () => {
    const noPrice = product('p-x', [['v-1', { name: 'Sin precio' }]])
    expect(priceCart([item('c1', 'p-x', 'v-1', 1)], new Map([['p-x', noPrice]])).blocker).toMatch(/sin precio/)
    expect(priceCart([item('c1', 'p-x', 'v-zz', 1)], new Map([['p-x', noPrice]])).lines[0]?.problem).toBe('variant-missing')
  })

  it('refuses to add different currencies', () => {
    const usd = product('p-usd', [['v-1', { name: 'Libro', price: '10', currency: 'USD' }]])
    const totals = priceCart(
      [item('c1', 'p-usd', 'v-1', 1), item('c2', 'p-termo', 'v-coral', 1)],
      new Map([
        ['p-usd', usd],
        ['p-termo', termo],
      ]),
    )
    expect(totals.currency).toBeNull()
    expect(totals.blocker).toMatch(/monedas distintas/)
  })

  it('reports an empty cart', () => {
    expect(priceCart([], new Map()).blocker).toBe('tu carrito está vacío')
  })
})

describe('toOrderLines', () => {
  it('builds exactly one priced order line per cart line', () => {
    const totals = priceCart([item('c1', 'p-morral', 'v-arena', 2)], new Map([['p-morral', morral]]))
    let n = 0
    const lines = toOrderLines(totals, () => `line-${++n}`)
    expect(lines).toEqual([
      { orderLineId: 'line-1', productId: 'p-morral', productVariantId: 'v-arena', quantity: 2, lineAmount: 378000, currency: 'COP' },
    ])
  })

  it('never builds lines for a cart that cannot be confirmed', () => {
    const totals = priceCart([item('c1', 'p-gone', 'v-1', 1)], new Map([['p-gone', null]]))
    expect(() => toOrderLines(totals, () => 'x')).toThrow(/ya no están disponibles/)
  })
})

describe('cart line text', () => {
  it('does not repeat the product name as the variant text', () => {
    const mouse = product('p', [['v', { name: 'Mouse inalámbrico Pulso', price: '59900' }]])
    const shirt = product('s', [['m', { name: 'Camiseta', price: '49900', Talla: 'M' }]])
    const { lines } = priceCart([item('1', 'p', 'v', 1), item('2', 's', 'm', 1)], new Map([['p', mouse], ['s', shirt]]))
    expect(lines.map((l) => [l.title, l.variantText])).toEqual([
      ['Mouse inalámbrico Pulso', ''],
      ['Camiseta', 'M'],
    ])
  })
})
