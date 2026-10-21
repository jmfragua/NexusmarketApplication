import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { Toaster } from 'sonner'
import { configureApiClient } from '@/api/client'
import { SessionProvider } from '@/features/auth/SessionProvider'
import { LocationProbe } from './LocationProbe'

type Handler = (init: RequestInit, url: URL) => { status: number; body?: unknown }
export type FakeRoutes = Record<string, Handler>

/**
 * A fetch double keyed by "METHOD /path" (without /api/v1). Every call is recorded so a test can
 * assert what the UI actually sent.
 */
export function fakeBackend(routes: FakeRoutes) {
  const calls: Array<{ key: string; body: unknown; auth: string | undefined }> = []
  const fetchImpl = async (input: RequestInfo | URL, init: RequestInit = {}) => {
    const url = new URL(String(input))
    const key = `${init.method ?? 'GET'} ${url.pathname.replace('/api/v1', '')}`
    const headers = (init.headers ?? {}) as Record<string, string>
    calls.push({ key, body: init.body ? JSON.parse(String(init.body)) : undefined, auth: headers.Authorization })
    const handler = routes[key]
    if (!handler) return new Response(JSON.stringify({ status: 404, rule: 'EntityNotFoundException', message: `no route ${key}` }), { status: 404 })
    const { status, body } = handler(init, url)
    return new Response(body === undefined ? null : JSON.stringify(body), { status })
  }
  configureApiClient({ fetchImpl: fetchImpl as typeof fetch, baseUrl: 'http://api.test' })
  return { calls }
}

export function renderApp(element: ReactElement, { path = '/', route = '/' }: { path?: string; route?: string } = {}) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[route]}>
        <SessionProvider>
          <Routes>
            <Route path={path} element={element} />
            <Route path="*" element={null} />
          </Routes>
          <LocationProbe />
          <Toaster />
        </SessionProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}
