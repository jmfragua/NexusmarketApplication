import { QueryClient } from '@tanstack/react-query'
import { ApiError } from '@/api/client'

export function createQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        // A 4xx is an answer, not a glitch: retrying it only delays the message the user needs.
        retry: (count, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && count < 2,
        refetchOnWindowFocus: false,
        staleTime: 10_000,
      },
      mutations: { retry: false },
    },
  })
}
