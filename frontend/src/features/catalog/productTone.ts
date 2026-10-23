import type { ProductType } from '@/api/types'

/**
 * The one colour rule of the catalogue: physical products are coral, digital products are lime.
 * The price sticker and the type chip both read it, so the two always tell the same thing.
 */
export type ProductTone = 'coral' | 'lime'

export function productTone(type: ProductType): ProductTone {
  return type === 'DIGITAL' ? 'lime' : 'coral'
}

/** Soft fill and an outline in the full tone of the sticker. */
export const typeChipRing: Record<ProductTone, string> = {
  coral: 'ring-1 ring-inset ring-coral',
  lime: 'ring-1 ring-inset ring-lime-deep',
}
