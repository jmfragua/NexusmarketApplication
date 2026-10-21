import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import type { CartResponse, ProductResponse } from '@/api/types'
import { fakeBackend, renderApp } from '@/test/render'
import { CartPage } from './CartPage'

const termo: ProductResponse = {
  id: 'p-termo',
  sellerId: 's-1',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: [{ id: 'v-coral', attributes: { name: 'Termo de acero', price: '68500', currency: 'COP', Color: 'Coral' } }],
}

function signedInBuyer() {
  localStorage.setItem(
    'nexusmarket.session',
    JSON.stringify({ token: 'buyer-token', userId: 'b-1', email: 'ana@nexusmarket.test', role: 'BUYER', expiresAt: null }),
  )
}

const money = (text: string) => text.replace(/\s/g, ' ')

describe('cart flow', () => {
  beforeEach(() => {
    localStorage.clear()
    signedInBuyer()
  })

  it('prices the cart from the catalogue, changes quantities and removes lines', async () => {
    let cart: CartResponse = { id: 'cart-1', buyerId: 'b-1', items: [{ id: 'ci-1', productId: 'p-termo', productVariantId: 'v-coral', quantity: 1 }] }
    const { calls } = fakeBackend({
      'GET /buyer/me/cart': () => ({ status: 200, body: cart }),
      'GET /catalog/products/p-termo': () => ({ status: 200, body: termo }),
      'PATCH /buyer/me/cart/items/ci-1': (init) => {
        const { quantity } = JSON.parse(String(init.body)) as { quantity: number }
        cart = { ...cart, items: cart.items.map((i) => ({ ...i, quantity })) }
        return { status: 200, body: cart }
      },
      'DELETE /buyer/me/cart/items/ci-1': () => {
        cart = { ...cart, items: [] }
        return { status: 200, body: cart }
      },
    })
    renderApp(<CartPage />, { path: '/carrito', route: '/carrito' })

    expect(await screen.findByRole('heading', { name: 'Termo de acero' })).toBeInTheDocument()
    const summary = screen.getByRole('complementary', { name: 'Resumen' })
    await waitFor(() => expect(money(summary.textContent ?? '')).toContain('$ 68.500'))

    await userEvent.click(within(screen.getByRole('group', { name: 'Cantidad de Termo de acero' })).getByRole('button', { name: 'Sumar uno' }))

    await waitFor(() => expect(money(summary.textContent ?? '')).toContain('$ 137.000'))
    const patch = calls.find((c) => c.key === 'PATCH /buyer/me/cart/items/ci-1')
    expect(patch?.body).toEqual({ quantity: 2 })
    expect(patch?.auth).toBe('Bearer buyer-token')

    await userEvent.click(screen.getByRole('button', { name: 'Quitar Termo de acero del carrito' }))
    expect(await screen.findByText('tu carrito está vacío')).toBeInTheDocument()
  })

  it('blocks checkout and explains why when a product is no longer published', async () => {
    fakeBackend({
      'GET /buyer/me/cart': () => ({
        status: 200,
        body: { id: 'cart-1', buyerId: 'b-1', items: [{ id: 'ci-1', productId: 'p-gone', productVariantId: 'v-1', quantity: 1 }] },
      }),
      'GET /catalog/products/p-gone': () => ({ status: 409, body: { status: 409, rule: 'ProductNotAvailableException', message: 'not visible' } }),
    })
    renderApp(<CartPage />, { path: '/carrito', route: '/carrito' })

    expect(await screen.findByText('ya no está disponible')).toBeInTheDocument()
    expect(screen.getByText('quita los productos que ya no están disponibles para continuar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ir a pagar' })).toBeDisabled()
  })
})
