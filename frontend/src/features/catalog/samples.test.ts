import { describe, expect, it } from 'vitest'
import { fallbackTitle, readProduct } from './variant'
import { isSampleId, SAMPLE_PRODUCTS } from './samples'

describe('demo products', () => {
  it('has enough products, of both types, with unique ids', () => {
    expect(SAMPLE_PRODUCTS.length).toBeGreaterThanOrEqual(6)
    expect(new Set(SAMPLE_PRODUCTS.map((p) => p.id)).size).toBe(SAMPLE_PRODUCTS.length)
    expect(new Set(SAMPLE_PRODUCTS.map((p) => p.productType))).toEqual(new Set(['PHYSICAL', 'DIGITAL']))
    expect(SAMPLE_PRODUCTS.every((p) => isSampleId(p.id))).toBe(true)
  })

  it.each(SAMPLE_PRODUCTS.map((p) => [p.id, p] as const))('%s has a real name, a brand, a price and a description', (_id, product) => {
    const view = readProduct(product)
    expect(view.title).not.toBe(fallbackTitle(product.id))
    expect(view.title).not.toMatch(/^Producto /)
    expect(view.brand).toBeTruthy()
    expect(view.description).toBeTruthy()
    expect(view.priceFrom).not.toBeNull()
    expect(view.priceFrom ?? 0).toBeGreaterThan(0)
    expect(view.currency).toBe('COP')
  })

  it('gives every variant of every demo product a name and a price', () => {
    for (const product of SAMPLE_PRODUCTS) {
      for (const variant of readProduct(product).variants) {
        expect([product.id, variant.name].every(Boolean)).toBe(true)
        expect([product.id, variant.price !== null].every(Boolean)).toBe(true)
      }
    }
  })

  it('is visible in the catalogue and published', () => {
    expect(SAMPLE_PRODUCTS.every((p) => p.visibleInCatalog && p.status === 'PUBLISHED')).toBe(true)
  })
})
