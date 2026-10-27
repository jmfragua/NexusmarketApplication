import type { ProductResponse, ProductVariantResponse } from '@/api/types'
import { DEFAULT_CURRENCY, parsePrice } from '@/lib/money'

/**
 * The backend models no name, price or image on a product: a variant is only a map of attributes
 * (RD-CAT-05). This frontend stores its commercial data under these reserved keys; every other
 * key (Color, Talla, Capacidad...) is a choosable option shown to the buyer.
 */
export const RESERVED_KEYS = ['name', 'price', 'currency', 'description', 'image', 'brand'] as const
export type ReservedKey = (typeof RESERVED_KEYS)[number]

const reserved = new Set<string>(RESERVED_KEYS)

export function isReservedKey(key: string): boolean {
  return reserved.has(key.trim().toLowerCase())
}

export interface VariantView {
  id: string
  name: string | null
  price: number | null
  currency: string
  description: string | null
  image: string | null
  brand: string | null
  options: Array<[string, string]>
}

function pick(attributes: Record<string, string>, key: ReservedKey): string | null {
  for (const [k, v] of Object.entries(attributes)) {
    if (k.trim().toLowerCase() === key && v.trim() !== '') return v.trim()
  }
  return null
}

// A path served by the frontend itself, such as /productos/morral.jpg: one leading slash (two would
// be a protocol-relative URL to another host), plain file-name characters and no ".." segment.
const LOCAL_IMAGE = /^\/(?!\/)[A-Za-z0-9._~/-]+$/

/**
 * The image of a variant, or null when it is not one we are willing to load: an http(s) URL or a
 * local path under the site root. Anything else (javascript:, data:, "//host", "../") is dropped.
 */
export function safeImageSource(value: string | null): string | null {
  const url = value?.trim()
  if (!url) return null
  if (url.startsWith('/')) return LOCAL_IMAGE.test(url) && !url.split('/').includes('..') ? url : null
  try {
    const parsed = new URL(url)
    return parsed.protocol === 'https:' || parsed.protocol === 'http:' ? parsed.toString() : null
  } catch {
    return null
  }
}

export function readVariant(variant: ProductVariantResponse): VariantView {
  const { attributes } = variant
  return {
    id: variant.id,
    name: pick(attributes, 'name'),
    price: parsePrice(pick(attributes, 'price') ?? undefined),
    currency: (pick(attributes, 'currency') ?? DEFAULT_CURRENCY).toUpperCase(),
    description: pick(attributes, 'description'),
    image: safeImageSource(pick(attributes, 'image')),
    brand: pick(attributes, 'brand'),
    options: Object.entries(attributes)
      .filter(([key, value]) => !isReservedKey(key) && value.trim() !== '')
      .map(([key, value]) => [key.trim(), value.trim()] as [string, string])
      .sort(([a], [b]) => a.localeCompare(b, 'es')),
  }
}

export interface ProductView {
  id: string
  title: string
  brand: string | null
  description: string | null
  image: string | null
  /** Lowest priced variant, which is what a card advertises ("desde"). */
  priceFrom: number | null
  currency: string
  hasPriceRange: boolean
  variants: VariantView[]
}

export function fallbackTitle(productId: string): string {
  return `Producto ${productId.replace(/-/g, '').slice(0, 6).toUpperCase()}`
}

export function readProduct(product: ProductResponse): ProductView {
  const variants = product.variants.map(readVariant)
  const first = variants[0]
  const priced = variants.filter((v): v is VariantView & { price: number } => v.price !== null)
  const cheapest = priced.reduce<(VariantView & { price: number }) | null>(
    (best, v) => (best === null || v.price < best.price ? v : best),
    null,
  )
  const prices = new Set(priced.map((v) => v.price))
  return {
    id: product.id,
    title: variants.find((v) => v.name)?.name ?? fallbackTitle(product.id),
    brand: variants.find((v) => v.brand)?.brand ?? null,
    description: variants.find((v) => v.description)?.description ?? null,
    image: cheapest?.image ?? first?.image ?? variants.find((v) => v.image)?.image ?? null,
    priceFrom: cheapest?.price ?? null,
    currency: cheapest?.currency ?? first?.currency ?? DEFAULT_CURRENCY,
    hasPriceRange: prices.size > 1,
    variants,
  }
}

/** Short human label for a variant inside a product, e.g. "Azul · M". */
export function variantLabel(variant: VariantView): string {
  if (variant.options.length > 0) return variant.options.map(([, v]) => v).join(' · ')
  return variant.name ?? 'Única'
}

function sameText(a: string, b: string): boolean {
  const norm = (t: string) => t.normalize('NFD').replace(/\p{Diacritic}/gu, '').replace(/\s+/g, ' ').trim().toLowerCase()
  return norm(a) === norm(b)
}

/**
 * What tells this variant apart, printed under the product name: its options ("M", "Arena"), or
 * its own name. Empty when that would only repeat the product name ("Mouse inalámbrico Pulso"
 * under "Mouse inalámbrico Pulso").
 */
export function variantDetail(variant: VariantView, productTitle: string): string {
  const label = variantLabel(variant)
  return sameText(label, productTitle) ? '' : label
}

export interface VariantDraft {
  name: string
  price: string
  currency: string
  description?: string
  image?: string
  brand?: string
  options: Array<{ key: string; value: string }>
}

export function buildAttributes(draft: VariantDraft): Record<string, string> {
  const attributes: Record<string, string> = {}
  const set = (key: string, value: string | undefined) => {
    const trimmed = value?.trim() ?? ''
    if (trimmed !== '') attributes[key] = trimmed
  }
  set('name', draft.name)
  const price = parsePrice(draft.price)
  if (price !== null) attributes.price = String(price)
  set('currency', (draft.currency || DEFAULT_CURRENCY).toUpperCase())
  set('description', draft.description)
  set('image', draft.image)
  set('brand', draft.brand)
  for (const { key, value } of draft.options) {
    const k = key.trim()
    if (k !== '' && !isReservedKey(k)) set(k, value)
  }
  return attributes
}
