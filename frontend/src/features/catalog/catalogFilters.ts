import type { ProductResponse, ProductType } from '@/api/types'
import { parsePrice } from '@/lib/money'
import { readProduct } from './variant'

/**
 * Search, filters and order of the catalogue. They all live in the URL (so a filtered view can be
 * shared and survives a reload) and run on the client over the whole public catalogue, which the
 * API returns without query parameters of its own.
 */

/** How many cards the catalogue shows at first, and how many more each "ver más" adds. */
export const PAGE_SIZE = 12

export type TypeFilter = 'todos' | 'fisicos' | 'digitales'
export type SortOrder = 'destacados' | 'precio-asc' | 'precio-desc'

export interface CatalogFilters {
  query: string
  type: TypeFilter
  /** The brand a shopper picked from a card ("tienda: Kilo"), or null. */
  store: string | null
  minPrice: number | null
  maxPrice: number | null
  sort: SortOrder
}

export const SORT_OPTIONS: Array<{ value: SortOrder; label: string }> = [
  { value: 'destacados', label: 'Destacados' },
  { value: 'precio-asc', label: 'Precio: menor a mayor' },
  { value: 'precio-desc', label: 'Precio: mayor a menor' },
]

/** The URL parameter of every filter. */
export const PARAM = { query: 'q', type: 'tipo', store: 'tienda', min: 'min', max: 'max', sort: 'orden' } as const

const typeOf: Record<TypeFilter, ProductType | null> = { todos: null, fisicos: 'PHYSICAL', digitales: 'DIGITAL' }

function readPrice(raw: string | null): number | null {
  if (raw === null || raw.trim() === '') return null
  const value = parsePrice(raw)
  return value !== null && value >= 0 ? value : null
}

export function readFilters(params: URLSearchParams): CatalogFilters {
  const type = params.get(PARAM.type)
  const sort = params.get(PARAM.sort)
  const store = params.get(PARAM.store)?.trim()
  return {
    query: params.get(PARAM.query) ?? '',
    type: type === 'fisicos' || type === 'digitales' ? type : 'todos',
    store: store ? store : null,
    minPrice: readPrice(params.get(PARAM.min)),
    maxPrice: readPrice(params.get(PARAM.max)),
    sort: sort === 'precio-asc' || sort === 'precio-desc' ? sort : 'destacados',
  }
}

/** True when anything narrows or reorders the catalogue, so "limpiar filtros" has work to do. */
export function hasActiveFilters(filters: CatalogFilters): boolean {
  return (
    filters.query.trim() !== '' ||
    filters.type !== 'todos' ||
    filters.store !== null ||
    filters.minPrice !== null ||
    filters.maxPrice !== null ||
    filters.sort !== 'destacados'
  )
}

export function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

function matchesQuery(product: ProductResponse, query: string): boolean {
  if (query.trim() === '') return true
  const view = readProduct(product)
  const haystack = normalize(
    [view.title, view.brand, view.description, ...view.variants.flatMap((v) => v.options.map(([, value]) => value))]
      .filter(Boolean)
      .join(' '),
  )
  return normalize(query)
    .split(/\s+/)
    .filter(Boolean)
    .every((word) => haystack.includes(word))
}

/** A product is in the price range when at least one of its variants is. */
function matchesPrice(product: ProductResponse, min: number | null, max: number | null): boolean {
  if (min === null && max === null) return true
  // A range given backwards (min above max) is read the right way round.
  const low = min !== null && max !== null ? Math.min(min, max) : min
  const high = min !== null && max !== null ? Math.max(min, max) : max
  return readProduct(product).variants.some(
    (v) => v.price !== null && (low === null || v.price >= low) && (high === null || v.price <= high),
  )
}

export function applyFilters(products: ProductResponse[], filters: CatalogFilters): ProductResponse[] {
  const type = typeOf[filters.type]
  const store = filters.store === null ? null : normalize(filters.store)
  const found = products.filter(
    (p) =>
      (type === null || p.productType === type) &&
      (store === null || normalize(readProduct(p).brand ?? '') === store) &&
      matchesQuery(p, filters.query) &&
      matchesPrice(p, filters.minPrice, filters.maxPrice),
  )
  if (filters.sort === 'destacados') return found
  const direction = filters.sort === 'precio-asc' ? 1 : -1
  // Products without a price yet always go last, whatever the direction. The sort is stable, so
  // products with the same price keep the order of the catalogue.
  return [...found].sort((a, b) => {
    const pa = readProduct(a).priceFrom
    const pb = readProduct(b).priceFrom
    if (pa === null || pb === null) return pa === pb ? 0 : pa === null ? 1 : -1
    return (pa - pb) * direction
  })
}

/** The brands present in the catalogue, once each and in alphabetical order, for the store chips. */
export function storesOf(products: ProductResponse[]): string[] {
  const seen = new Map<string, string>()
  for (const product of products) {
    const brand = readProduct(product).brand?.trim()
    if (brand && !seen.has(normalize(brand))) seen.set(normalize(brand), brand)
  }
  return [...seen.values()].sort((a, b) => a.localeCompare(b, 'es'))
}

export function sameStore(a: string, b: string | null): boolean {
  return b !== null && normalize(a) === normalize(b)
}
