import type { ProductResponse } from '@/api/types'
import { readProduct } from './variant'

export const HERO_SLOTS = 3

export interface HeroTile {
  /** Identifier the cover is drawn from. */
  id: string
  /** The product the tile shows, or null for a decorative cover. */
  product: ProductResponse | null
}

/**
 * The first products of the catalogue that have a real name. When there are fewer than three the
 * rest are decorative covers: no name and no price, because a price alone would mean nothing.
 */
export function heroTiles(products: ProductResponse[]): HeroTile[] {
  const named = products.filter((p) => readProduct(p).variants.some((v) => v.name !== null)).slice(0, HERO_SLOTS)
  const tiles: HeroTile[] = named.map((product) => ({ id: product.id, product }))
  for (let i = tiles.length; i < HERO_SLOTS; i++) tiles.push({ id: `nexusmarket-decor-${i + 1}`, product: null })
  return tiles
}
