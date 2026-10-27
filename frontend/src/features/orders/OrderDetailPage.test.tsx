import { screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { OrderDetailPage } from './OrderDetailPage'

const product = (id: string, name: string, extra: Record<string, string> = {}) => ({
  id,
  sellerId: 's',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: [{ id: `${id}-v`, attributes: { name, price: '59900', currency: 'COP', ...extra } }],
})

const line = (id: string, productId: string) => ({ id, productId, productVariantId: `${productId}-v`, quantity: 1, lineAmount: 59900, currency: 'COP' })

function open() {
  localStorage.setItem('nexusmarket.session', JSON.stringify({ token: 't', userId: 'u', email: 'u@nexusmarket.test', role: 'BUYER', expiresAt: null }))
  fakeBackend({
    'GET /buyer/me/orders/o-1': () => ({
      status: 200,
      body: { id: 'o-1', buyerId: 'u', status: 'PAID', modifiable: false, deliveryAddress: 'Calle 10 # 5-20, Bogotá', totalAmount: 179700, currency: 'COP', lines: [line('l1', 'mouse'), line('l2', 'shirt'), line('l3', 'gone')] },
    }),
    'GET /catalog/products/mouse': () => ({ status: 200, body: product('mouse', 'Mouse inalámbrico Pulso') }),
    'GET /catalog/products/shirt': () => ({ status: 200, body: product('shirt', 'Camiseta', { Talla: 'M' }) }),
    'GET /catalog/products/gone': () => ({ status: 404, body: { status: 404, rule: 'EntityNotFoundException', message: 'no existe' } }),
    'GET /buyer/me/orders/o-1/invoice': () => ({ status: 404, body: { status: 404, rule: 'EntityNotFoundException', message: 'sin factura' } }),
  })
  renderApp(<OrderDetailPage />, { path: '/pedidos/:orderId', route: '/pedidos/o-1' })
}

describe('order detail', () => {
  beforeEach(() => localStorage.clear())

  it('links the products still in the catalogue to their page, and leaves a retired one as plain text', async () => {
    open()
    const products = await screen.findByRole('region', { name: 'productos' })
    expect(await within(products).findByRole('link', { name: 'Mouse inalámbrico Pulso' })).toHaveAttribute('href', '/productos/mouse')
    expect(within(products).getByRole('link', { name: 'Camiseta' })).toHaveAttribute('href', '/productos/shirt')
    expect(within(products).getAllByRole('link')).toHaveLength(2)
  })

  it('does not repeat the product name as the variant, but keeps a real option', async () => {
    open()
    const products = await screen.findByRole('region', { name: 'productos' })
    await within(products).findByRole('link', { name: 'Mouse inalámbrico Pulso' })
    expect(within(products).getAllByText('1 unidad')).toHaveLength(2)
    expect(within(products).getByText('1 unidad · M')).toBeInTheDocument()
    expect(within(products).queryByText(/· Mouse inalámbrico Pulso/)).toBeNull()
  })

  it('marks the pending invoice with a clock and keeps its text', async () => {
    open()
    const notice = (await screen.findByText('aún no se ha emitido')).closest('[role="note"]') as HTMLElement
    expect(notice).toHaveTextContent('el equipo de NexusMarket la genera después del pago; vuelve a revisar más tarde.')
    expect(notice.querySelector('[aria-hidden="true"] svg circle')).not.toBeNull()
  })

  it('shows the paid order with the dispatch under way', async () => {
    open()
    const timeline = await screen.findByRole('list', { name: 'Estado del pedido' })
    expect(within(timeline).getByText(', en curso')).toBeInTheDocument()
  })
})
