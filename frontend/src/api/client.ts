import type { ErrorBody, PageParams } from './types'

export class ApiError extends Error {
  readonly status: number
  readonly rule: string

  constructor(status: number, rule: string, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.rule = rule
  }

  /** The request never reached the backend: it is down, or CORS rejected the origin. */
  get isNetwork(): boolean {
    return this.status === 0
  }
}

type Query = Record<string, string | number | boolean | undefined>

interface RequestOptions {
  body?: unknown
  query?: Query
  signal?: AbortSignal
}

interface ClientConfig {
  baseUrl: string
  getToken: () => string | null
  onUnauthorized: () => void
  fetchImpl: typeof fetch
}

function readBaseUrl(): string {
  const raw: unknown = import.meta.env.VITE_API_URL
  const base = typeof raw === 'string' && raw.trim() !== '' ? raw.trim() : 'http://localhost:8080'
  return base.replace(/\/+$/, '')
}

// Held synchronously, not wired from an effect: React runs the effects of children (the first
// queries) before those of the provider, so a token set in an effect misses the first requests.
let authToken: string | null = null

export function setAuthToken(token: string | null): void {
  authToken = token
}

const config: ClientConfig = {
  baseUrl: readBaseUrl(),
  getToken: () => authToken,
  onUnauthorized: () => {},
  fetchImpl: (...args) => fetch(...args),
}

export function configureApiClient(next: Partial<ClientConfig>): void {
  Object.assign(config, next)
}

export function apiUrl(path: string, query?: Query): string {
  const url = new URL(`${config.baseUrl}/api/v1${path}`)
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) url.searchParams.set(key, String(value))
  }
  return url.toString()
}

function isErrorBody(value: unknown): value is ErrorBody {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Record<string, unknown>
  return typeof candidate.message === 'string' && typeof candidate.rule === 'string'
}

const SESSION_REJECTED = 'SessionRejected'

async function parseError(response: Response): Promise<ApiError> {
  const fallback = defaultMessage(response.status)
  try {
    const body: unknown = await response.json()
    if (isErrorBody(body)) {
      return new ApiError(response.status, body.rule, body.message || fallback)
    }
  } catch {
    // The body was empty or not JSON; the status alone has to explain it.
  }
  // Every domain rejection carries {status, rule, message}. A bare 403 comes from the security
  // filter chain instead: the backend answers a missing, invalid or expired token with 403, not 401.
  if (response.status === 403) {
    return new ApiError(403, SESSION_REJECTED, 'tu sesión ya no es válida, vuelve a ingresar')
  }
  return new ApiError(response.status, 'HttpError', fallback)
}

function isRejectedSession(error: ApiError): boolean {
  return error.status === 401 || error.rule === SESSION_REJECTED
}

function defaultMessage(status: number): string {
  switch (status) {
    case 400:
      return 'la solicitud no es válida'
    case 401:
      return 'tu sesión expiró, vuelve a ingresar'
    case 403:
      return 'no tienes permiso para hacer esto'
    case 404:
      return 'no encontramos lo que buscabas'
    case 409:
      return 'el estado actual no permite esta operación'
    default:
      return 'el servidor no pudo completar la solicitud'
  }
}

export async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = config.getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await config.fetchImpl(apiUrl(path, options.query), {
      method,
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch (cause) {
    if (cause instanceof DOMException && cause.name === 'AbortError') throw cause
    throw new ApiError(0, 'NetworkError', 'no pudimos conectar con el servidor, revisa tu conexión e inténtalo de nuevo')
  }

  if (!response.ok) {
    const error = await parseError(response)
    // Only an authenticated call can expire; a failed login is a 403 and must not log anyone out.
    if (token && isRejectedSession(error)) config.onUnauthorized()
    throw error
  }

  if (response.status === 204) return null as T
  const text = await response.text()
  return (text === '' ? null : JSON.parse(text)) as T
}

export const http = {
  get: <T>(path: string, query?: Query, signal?: AbortSignal) => request<T>('GET', path, { query, signal }),
  post: <T>(path: string, body?: unknown, query?: Query) => request<T>('POST', path, { body, query }),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, { body }),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, { body }),
  delete: <T>(path: string) => request<T>('DELETE', path),
}

export function pageQuery(params: PageParams = {}): Query {
  return { page: params.page ?? 0, size: params.size ?? 20 }
}
