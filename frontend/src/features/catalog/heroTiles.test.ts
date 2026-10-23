import { describe, expect, it } from 'vitest'
import type { ProductResponse } from '@/api/types'
import { HERO_SLOTS, heroTiles } from './heroTiles'

const product = (id: string, attributes: Record<string, string>): ProductResponse => ({
  id,
  sellerId: 's',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: [{ id: `${id}-v`, attributes }],
})

describe('heroTiles', () => {
  it('takes the first three products that have a real name', () => {
    const tiles = heroTiles([
      product('a', { name: 'Morral', price: '1000' }),
      product('b', { name: 'Termo' }),
      product('c', { name: 'Guía' }),
      product('d', { name: 'Cuarto' }),
    ])
    expect(tiles.map((t) => t.product?.id)).toEqual(['a', 'b', 'c'])
  })

  it('skips a product without a name instead of showing a price or a placeholder title', () => {
    const tiles = heroTiles([product('anon', { color: 'azul' }), product('a', { name: 'Morral' })])
    expect(tiles.map((t) => t.product?.id ?? null)).toEqual(['a', null, null])
  })

  it('fills the missing slots with decorative covers that carry no product', () => {
    const tiles = heroTiles([product('a', { name: 'Morral' })])
    expect(tiles).toHaveLength(HERO_SLOTS)
    expect(tiles.filter((t) => t.product === null)).toHaveLength(2)
    expect(new Set(tiles.map((t) => t.id)).size).toBe(HERO_SLOTS)
  })

  it('is all decoration while there is nothing to show', () => {
    expect(heroTiles([]).every((t) => t.product === null)).toBe(true)
  })
})
