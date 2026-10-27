import { describe, expect, it } from 'vitest'
import type { ProductResponse } from '@/api/types'
import { applyFilters, hasActiveFilters, readFilters, type CatalogFilters } from './catalogFilters'
import { SAMPLE_PRODUCTS } from './samples'
import { readProduct } from './variant'

const product = (id: string, type: ProductResponse['productType'], brand: string, prices: string[], name = id): ProductResponse => ({
  id,
  sellerId: 's',
  productType: type,
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: prices.map((price, i) => ({ id: `${id}-v${i}`, attributes: { name, brand, price, currency: 'COP' } })),
})

const morral = product('morral', 'PHYSICAL', 'Páramo', ['189000', '199000'], 'Morral urbano')
const mouse = product('mouse', 'PHYSICAL', 'Kilo', ['59900'], 'Mouse Pulso')
const curso = product('curso', 'DIGITAL', 'Estudio Faro', ['89000'], 'Curso de fotografía')
const sinPrecio: ProductResponse = { ...product('nuevo', 'PHYSICAL', 'Kilo', []), variants: [{ id: 'n', attributes: { name: 'Sin precio', brand: 'Kilo' } }] }
const all = [morral, mouse, curso, sinPrecio]

const none: CatalogFilters = { query: '', type: 'todos', store: null, minPrice: null, maxPrice: null, sort: 'destacados' }
const ids = (products: ProductResponse[]) => products.map((p) => p.id)

describe('reading the filters from the URL', () => {
  it('reads every filter and ignores values it does not know', () => {
    expect(readFilters(new URLSearchParams('q=termo&tipo=digitales&tienda=Kilo&min=10.000&max=90000&orden=precio-desc'))).toEqual({
      query: 'termo',
      type: 'digitales',
      store: 'Kilo',
      minPrice: 10000,
      maxPrice: 90000,
      sort: 'precio-desc',
    })
    expect(readFilters(new URLSearchParams('tipo=otro&orden=raro&min=abc&max=-5&tienda=%20'))).toEqual(none)
  })

  it('knows when there is something to clear', () => {
    expect(hasActiveFilters(none)).toBe(false)
    expect(hasActiveFilters({ ...none, sort: 'precio-asc' })).toBe(true)
    expect(hasActiveFilters({ ...none, store: 'Kilo' })).toBe(true)
    expect(hasActiveFilters({ ...none, minPrice: 0 })).toBe(true)
  })
})

describe('applying the filters', () => {
  it('keeps the catalogue order when nothing is chosen', () => {
    expect(ids(applyFilters(all, none))).toEqual(['morral', 'mouse', 'curso', 'nuevo'])
  })

  it('orders by price both ways, by the lowest price of each product, with unpriced products last', () => {
    expect(ids(applyFilters(all, { ...none, sort: 'precio-asc' }))).toEqual(['mouse', 'curso', 'morral', 'nuevo'])
    expect(ids(applyFilters(all, { ...none, sort: 'precio-desc' }))).toEqual(['morral', 'curso', 'mouse', 'nuevo'])
  })

  it('keeps a product in a price range when any of its variants is in it', () => {
    expect(ids(applyFilters(all, { ...none, minPrice: 195000 }))).toEqual(['morral'])
    expect(ids(applyFilters(all, { ...none, maxPrice: 60000 }))).toEqual(['mouse'])
    expect(ids(applyFilters(all, { ...none, minPrice: 60000, maxPrice: 100000 }))).toEqual(['curso'])
  })

  it('reads a range typed backwards the right way round', () => {
    expect(ids(applyFilters(all, { ...none, minPrice: 100000, maxPrice: 60000 }))).toEqual(['curso'])
  })

  it('filters by store ignoring case and accents', () => {
    expect(ids(applyFilters(all, { ...none, store: 'paramo' }))).toEqual(['morral'])
    expect(ids(applyFilters(all, { ...none, store: 'KILO' }))).toEqual(['mouse', 'nuevo'])
  })

  it('combines search, type, store, price and order', () => {
    const filters: CatalogFilters = { query: 'mouse', type: 'fisicos', store: 'Kilo', minPrice: 50000, maxPrice: 70000, sort: 'precio-desc' }
    expect(ids(applyFilters(all, filters))).toEqual(['mouse'])
    expect(applyFilters(all, { ...filters, type: 'digitales' })).toEqual([])
  })
})

describe('the demo products with filters', () => {
  it('can be filtered by type, store and price, and ordered by price', () => {
    const physical = applyFilters(SAMPLE_PRODUCTS, { ...none, type: 'fisicos' })
    expect(physical.length).toBeGreaterThan(0)
    expect(physical.every((p) => p.productType === 'PHYSICAL')).toBe(true)

    expect(applyFilters(SAMPLE_PRODUCTS, { ...none, store: 'Kilo' }).map((p) => readProduct(p).title)).toEqual(['Audífonos inalámbricos Onda'])

    const cheap = applyFilters(SAMPLE_PRODUCTS, { ...none, maxPrice: 50000 })
    expect(cheap.length).toBeGreaterThan(0)
    expect(cheap.every((p) => readProduct(p).variants.some((v) => (v.price ?? Infinity) <= 50000))).toBe(true)

    const prices = applyFilters(SAMPLE_PRODUCTS, { ...none, sort: 'precio-asc' }).map((p) => readProduct(p).priceFrom ?? 0)
    expect(prices).toEqual([...prices].sort((a, b) => a - b))
  })
})
