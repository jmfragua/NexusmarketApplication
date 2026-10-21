import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, apiUrl, configureApiClient, request, setAuthToken } from './client'
import { describeError } from './errors'

function respond(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
  })
}

describe('api client', () => {
  const fetchImpl = vi.fn<typeof fetch>()
  const onUnauthorized = vi.fn<() => void>()

  beforeEach(() => {
    configureApiClient({ fetchImpl, onUnauthorized, baseUrl: 'http://api.test' })
    setAuthToken(null)
  })

  afterEach(() => {
    fetchImpl.mockReset()
    onUnauthorized.mockReset()
  })

  it('prefixes /api/v1 and serialises the query', () => {
    expect(apiUrl('/catalog/products', { page: 2, size: 20, status: undefined })).toBe(
      'http://api.test/api/v1/catalog/products?page=2&size=20',
    )
  })

  it('sends the bearer token and a JSON body', async () => {
    setAuthToken('secret-token')
    fetchImpl.mockResolvedValue(respond(201, { id: 'p-1' }))

    const result = await request<{ id: string }>('POST', '/seller/products', { body: { productId: 'p-1' } })

    expect(result).toEqual({ id: 'p-1' })
    const init = fetchImpl.mock.calls[0]?.[1]
    const headers = init?.headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer secret-token')
    expect(headers['Content-Type']).toBe('application/json')
    expect(init?.body).toBe('{"productId":"p-1"}')
  })

  it('turns the backend error body into an ApiError with its rule', async () => {
    setAuthToken('t')
    fetchImpl.mockResolvedValue(respond(409, { status: 409, rule: 'EmptyCartException', message: 'cart is empty' }))

    const error = await request('POST', '/buyer/me/orders').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 409, rule: 'EmptyCartException', message: 'cart is empty' })
    expect(describeError(error)).toBe('tu carrito está vacío; agrega algo antes de confirmar')
    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('signs out on 401 and on a bare 403 from the security chain', async () => {
    setAuthToken('expired')
    fetchImpl.mockResolvedValueOnce(respond(401))
    await expect(request('GET', '/buyer/me/cart')).rejects.toMatchObject({ status: 401 })

    fetchImpl.mockResolvedValueOnce(respond(403))
    await expect(request('GET', '/buyer/me/cart')).rejects.toMatchObject({ status: 403, rule: 'SessionRejected' })

    expect(onUnauthorized).toHaveBeenCalledTimes(2)
  })

  it('keeps the session on a domain 403 and on a failed login', async () => {
    setAuthToken('valid')
    fetchImpl.mockResolvedValueOnce(
      respond(403, { status: 403, rule: 'ProductNotOwnedException', message: 'not yours' }),
    )
    await expect(request('POST', '/seller/products/x/publication')).rejects.toMatchObject({ status: 403 })

    setAuthToken(null)
    fetchImpl.mockResolvedValueOnce(
      respond(403, { status: 403, rule: 'UnauthorizedOperationException', message: 'invalid credentials' }),
    )
    const error = await request('POST', '/auth/login', { body: {} }).catch((e: unknown) => e)
    expect(describeError(error, 'login')).toBe('el correo o la contraseña no coinciden')

    expect(onUnauthorized).not.toHaveBeenCalled()
  })

  it('reports an unreachable backend as a network error', async () => {
    fetchImpl.mockRejectedValue(new TypeError('Failed to fetch'))
    const error = await request('GET', '/catalog/products').catch((e: unknown) => e)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).isNetwork).toBe(true)
  })

  it('returns null for 204 No Content', async () => {
    setAuthToken('t')
    fetchImpl.mockResolvedValue(respond(204))
    await expect(request('GET', '/supervisor/reports/invoiced-total')).resolves.toBeNull()
  })
})
