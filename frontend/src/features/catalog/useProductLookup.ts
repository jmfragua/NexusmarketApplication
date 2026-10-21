import { useQueries, type UseQueryResult } from '@tanstack/react-query'
import { useMemo } from 'react'
import { ApiError } from '@/api/client'
import { catalogApi } from '@/api/endpoints'
import type { ProductResponse } from '@/api/types'
import { catalogKeys } from './queries'

/** 404: the product no longer exists; 409: it exists but is no longer visible in the catalogue. */
export function isGone(error: unknown): boolean {
  return error instanceof ApiError && (error.status === 404 || error.status === 409)
}

// Module level on purpose: TanStack Query only memoises `combine` while its reference is stable.
function combine(results: Array<UseQueryResult<ProductResponse>>) {
  return {
    data: results.map((r) => r.data ?? null),
    loading: results.some((r) => r.isPending),
    error: results.find((r) => r.error && !isGone(r.error))?.error ?? null,
  }
}

/**
 * Resolves products through the public catalogue, which is where the name and price attributes
 * live. A product that is gone resolves to `null` instead of failing the whole lookup.
 */
export function useProductLookup(ids: string[]) {
  const unique = useMemo(() => [...new Set(ids)].sort(), [ids])
  const lookup = useQueries({
    queries: unique.map((id) => ({
      queryKey: catalogKeys.product(id),
      queryFn: ({ signal }: { signal: AbortSignal }) => catalogApi.get(id, signal),
      retry: (count: number, error: unknown) => !isGone(error) && count < 2,
      staleTime: 30_000,
    })),
    combine,
  })

  const products = useMemo(() => {
    const map = new Map<string, ProductResponse | null>()
    unique.forEach((id, i) => map.set(id, lookup.data[i] ?? null))
    return map
  }, [unique, lookup.data])

  return { products, loading: lookup.loading, error: lookup.error }
}
