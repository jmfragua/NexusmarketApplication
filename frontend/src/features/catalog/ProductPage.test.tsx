import { screen, within } from '@testing-library/react'
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

const mk = (id: string, brand: string, type: 'PHYSICAL' | 'DIGITAL', variants: Array<Record<string, string>>) => ({
  id,
  sellerId: 's',
  productType: type,
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: variants.map((attributes, i) => ({ id: `${id}-v${i}`, attributes: { brand, currency: 'COP', ...attributes } })),
})

const headphones = mk('aud', 'Kilo', 'PHYSICAL', [
  { name: 'Audífonos Onda', price: '249900', Color: 'Negro' },
  { name: 'Audífonos Onda', price: '269900', Color: 'Arena' },
])
const others = [
  mk('mouse', 'Kilo', 'PHYSICAL', [{ name: 'Mouse Pulso', price: '59900' }]),
  mk('yoga', 'Kilo', 'PHYSICAL', [{ name: 'Tapete Calma', price: '79000' }]),
  mk('gafas', 'Malecón', 'PHYSICAL', [{ name: 'Gafas Costa', price: '89000' }]),
]
const course = mk('curso', 'Estudio Faro', 'DIGITAL', [{ name: 'Curso de fotografía', price: '89000' }])

function openProduct(id: string, catalogue = [headphones, ...others, course]) {
  fakeBackend({
    [`GET /catalog/products/${id}`]: () => ({ status: 200, body: catalogue.find((p) => p.id === id) }),
    'GET /catalog/products': () => ({ status: 200, body: { content: catalogue, page: 0, size: 200, totalElements: catalogue.length, totalPages: 1 } }),
    'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [] } }),
  })
  renderApp(<ProductPage />, { path: '/productos/:productId', route: `/productos/${id}` })
}

describe('product page layout', () => {
  beforeEach(() => localStorage.clear())

  it('shows breadcrumbs Catálogo › brand › product', async () => {
    openProduct('aud')
    const crumbs = await screen.findByRole('navigation', { name: 'Ruta de navegación' })
    expect(within(crumbs).getByRole('link', { name: 'Catálogo' })).toHaveAttribute('href', '/')
    expect(within(crumbs).getByRole('link', { name: 'Kilo' })).toHaveAttribute('href', '/?tienda=Kilo')
    expect(within(crumbs).getByText('Audífonos Onda')).toHaveAttribute('aria-current', 'page')
  })

  it('links the brand next to the type chip to the catalogue filtered by it', async () => {
    openProduct('aud')
    expect(await screen.findByRole('link', { name: 'Ver la tienda Kilo' })).toHaveAttribute('href', '/?tienda=Kilo')
  })

  it('shows the price large in the buying box, and both price and sticker follow the chosen option', async () => {
    openProduct('aud')
    const price = await screen.findByTestId('buy-price')
    expect(price).toHaveTextContent(/249\.900/)
    const sticker = () => document.querySelector('.sticker') as HTMLElement
    expect(sticker()).toHaveTextContent(/249\.900/)
    await userEvent.click(screen.getByRole('radio', { name: /Arena/ }))
    expect(price).toHaveTextContent(/269\.900/)
    expect(sticker()).toHaveTextContent(/269\.900/)
  })

  it('promises tracked shipping for a physical product and instant delivery for a digital one', async () => {
    openProduct('aud')
    const strip = await screen.findByRole('list', { name: 'Lo que incluye tu compra' })
    expect(within(strip).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'envío rastreado',
      'pago único para todo tu carrito',
      'devoluciones cuando el pedido esté entregado',
    ])
  })

  it('adapts the first promise for a digital product', async () => {
    openProduct('curso')
    const strip = await screen.findByRole('list', { name: 'Lo que incluye tu compra' })
    expect(within(strip).getAllByRole('listitem')[0]).toHaveTextContent('entrega inmediata al pagar')
  })

  it('ends with up to four other products of the same brand', async () => {
    openProduct('aud')
    const more = await screen.findByRole('region', { name: 'más de Kilo' })
    const titles = (await within(more).findAllByRole('heading', { level: 3 })).map((h) => h.textContent)
    expect(titles).toEqual(['Mouse Pulso', 'Tapete Calma'])
    expect(within(more).getByRole('link', { name: 'Ver todo de Kilo' })).toHaveAttribute('href', '/?tienda=Kilo')
  })

  it('falls back to other products of the catalogue when the brand has no others', async () => {
    openProduct('curso')
    const more = await screen.findByRole('region', { name: 'también te puede gustar' })
    expect(await within(more).findAllByRole('article')).toHaveLength(4)
    expect(within(more).queryByText('Curso de fotografía')).toBeNull()
  })
})
