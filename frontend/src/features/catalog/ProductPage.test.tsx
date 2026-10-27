import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { ProductPage } from './ProductPage'

const lamp = {
  id: 'p',
  sellerId: 's',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: [{ id: 'v', attributes: { name: 'Lámpara de escritorio Eje', price: '134500', currency: 'COP', image: '/productos/lampara.jpg' } }],
}

describe('product page', () => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem('nexusmarket.session', JSON.stringify({ token: 't', userId: 'u', email: 'u@nexusmarket.test', role: 'BUYER', expiresAt: null }))
  })

  it('says "añadido al carrito" with a link to the cart after adding', async () => {
    const { calls } = fakeBackend({
      'GET /catalog/products/p': () => ({ status: 200, body: lamp }),
      'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [] } }),
      'POST /buyer/me/cart/items': () => ({ status: 201, body: { id: 'c', buyerId: 'u', items: [{ id: 'i', productId: 'p', productVariantId: 'v', quantity: 1 }] } }),
    })
    renderApp(<ProductPage />, { path: '/productos/:productId', route: '/productos/p' })
    await userEvent.click(await screen.findByRole('button', { name: 'Agregar al carrito' }))

    expect(await screen.findByText('añadido al carrito')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ver carrito' })).toBeInTheDocument()
    expect(calls.some((c) => c.key === 'POST /buyer/me/cart/items')).toBe(true)
  })

  it('shows the whole lamp in the product frame', async () => {
    fakeBackend({ 'GET /catalog/products/p': () => ({ status: 200, body: lamp }), 'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [] } }) })
    renderApp(<ProductPage />, { path: '/productos/:productId', route: '/productos/p' })
    expect((await screen.findByAltText('Lámpara de escritorio Eje')).className).toContain('object-contain')
  })
})
