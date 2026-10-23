import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { ProductResponse } from '@/api/types'
import { ProductCard } from './ProductCard'

const product = (over: Partial<ProductResponse> = {}, variants: Array<Record<string, string>> = [{ name: 'Morral urbano 22 L', brand: 'Páramo', price: '189000' }]): ProductResponse => ({
  id: 'p-1',
  sellerId: 's',
  productType: 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: variants.map((attributes, i) => ({ id: `v-${i}`, attributes })),
  ...over,
})

function card(p: ProductResponse) {
  render(
    <MemoryRouter>
      <ul>
        <ProductCard product={p} />
      </ul>
    </MemoryRouter>,
  )
  return screen.getByRole('link')
}

describe('product card', () => {
  it('shows the name, the brand under it, and the price in a sticker', () => {
    const link = card(product())
    expect(link).toHaveAttribute('href', '/productos/p-1')
    expect(within(link).getByRole('heading', { level: 3, name: 'Morral urbano 22 L' })).toBeInTheDocument()
    expect(within(link).getByText('Páramo')).toBeInTheDocument()
    expect(link.querySelector('.sticker')).toHaveTextContent('189.000')
  })

  it('says the type and, for a physical product, how it ships, instead of "una opción"', () => {
    const link = card(product())
    expect(link).toHaveTextContent('Físico')
    expect(link).toHaveTextContent('Envío desde bodega')
    expect(link).not.toHaveTextContent('una opción')
  })

  it('says a digital product is delivered at once', () => {
    const link = card(product({ productType: 'DIGITAL' }))
    expect(link).toHaveTextContent('Digital')
    expect(link).toHaveTextContent('Entrega inmediata al pagar')
    expect(link).not.toHaveTextContent('Envío desde bodega')
  })

  it('shows the number of options only when there are several, as secondary data next to the brand', () => {
    expect(card(product())).not.toHaveTextContent('opciones')
  })

  it('counts the options of a product with several variants, on the line of the brand', () => {
    const link = card(product({}, [{ name: 'Morral', brand: 'Páramo', price: '189000' }, { name: 'Morral', price: '199000' }, { name: 'Morral', price: '209000' }]))
    expect(within(link).getByText('Páramo · 3 opciones')).toBeInTheDocument()
  })

  it('shows only the number of options when there is no brand', () => {
    const link = card(product({}, [{ name: 'Morral', price: '189000' }, { name: 'Morral', price: '199000' }]))
    expect(within(link).getByText('2 opciones')).toBeInTheDocument()
  })

  it('prints "desde" outside the sticker when the variants do not share a price', () => {
    const link = card(product({}, [{ name: 'Morral', price: '189000' }, { name: 'Morral', price: '199000' }]))
    const sticker = link.querySelector('.sticker') as HTMLElement
    const since = within(link).getByText('desde')
    expect(sticker).not.toHaveTextContent('desde')
    expect(sticker.contains(since)).toBe(false)
    expect(since.className).toContain('text-xs')
    expect(sticker).toHaveTextContent('189.000')
  })

  it('does not print "desde" when every variant costs the same', () => {
    const link = card(product({}, [{ name: 'Morral', price: '189000' }, { name: 'Morral', price: '189000' }]))
    expect(within(link).queryByText('desde')).toBeNull()
  })

  it('shows "precio por definir" in a neutral tag, never as an offer, when there is no price', () => {
    const link = card(product({}, [{ name: 'Sin precio aún' }]))
    const sticker = link.querySelector('.sticker') as HTMLElement
    expect(sticker).toHaveTextContent('precio por definir')
    expect(sticker).toHaveAttribute('data-tone', 'neutral')
    expect(link).not.toHaveTextContent('sin precio')
  })

  it('tones a priced sticker, never as neutral', () => {
    const link = card(product())
    expect(['coral', 'lime', 'cobalt']).toContain((link.querySelector('.sticker') as HTMLElement).getAttribute('data-tone'))
  })

  it('describes a real photo with the product name and treats the generated poster as decoration', () => {
    card(product({}, [{ name: 'Morral', price: '1000', image: 'https://cdn.example.test/m.jpg' }]))
    expect(screen.getByAltText('Morral')).toBeInTheDocument()
  })

  it('has no accessible image when it draws the poster', () => {
    card(product())
    expect(screen.queryByRole('img')).toBeNull()
  })
})
