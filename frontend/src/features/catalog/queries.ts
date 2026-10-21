import { useQuery } from '@tanstack/react-query'
import { ApiError } from '@/api/client'
import { catalogApi } from '@/api/endpoints'
import type { ProductResponse } from '@/api/types'
import { isSampleId, SAMPLE_PRODUCTS } from './samples'

export const catalogKeys = {
  all: ['catalog'] as const,
  product: (id: string) => ['catalog', 'product', id] as const,
}

export type CatalogSource = 'live' | 'sample-offline' | 'sample-empty'

export interface CatalogResult {
  products: ProductResponse[]
  source: CatalogSource
}

// The page size is the backend maximum: search and filters run on the client over the whole
// public catalogue, which the API exposes without any query parameters of its own.
const CATALOG_SIZE = 200

async function fetchCatalog(signal: AbortSignal): Promise<CatalogResult> {
  try {
    const page = await catalogApi.list({ page: 0, size: CATALOG_SIZE }, signal)
    if (page.content.length === 0) return { products: SAMPLE_PRODUCTS, source: 'sample-empty' }
    return { products: page.content, source: 'live' }
  } catch (error) {
    if (error instanceof ApiError && error.isNetwork) return { products: SAMPLE_PRODUCTS, source: 'sample-offline' }
    throw error
  }
}

export function useCatalog() {
  return useQuery({ queryKey: catalogKeys.all, queryFn: ({ signal }) => fetchCatalog(signal), staleTime: 30_000 })
}

export function useProduct(productId: string) {
  return useQuery({
    queryKey: catalogKeys.product(productId),
    queryFn: async ({ signal }) => {
      if (isSampleId(productId)) {
        const found = SAMPLE_PRODUCTS.find((p) => p.id === productId)
        if (!found) throw new ApiError(404, 'EntityNotFoundException', 'este producto de ejemplo no existe')
        return found
      }
      return catalogApi.get(productId, signal)
    },
    retry: (count, error) => !(error instanceof ApiError && [404, 409].includes(error.status)) && count < 2,
  })
}
