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

function card(p: ProductResponse, route = '/') {
  render(
    <MemoryRouter initialEntries={[route]}>
      <ul>
        <ProductCard product={p} />
      </ul>
    </MemoryRouter>,
  )
  return screen.getByRole('article')
}

describe('product card', () => {
  it('shows the name, the brand under it, and the price in a sticker', () => {
    const link = card(product())
    expect(within(link).getByRole('link', { name: 'Morral urbano 22 L' })).toHaveAttribute('href', '/productos/p-1')
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
    expect(within(link).getByText('Páramo').closest('p')).toHaveTextContent('Páramo · 3 opciones')
  })

  it('shows only the number of options when there is no brand', () => {
    const link = card(product({}, [{ name: 'Morral', price: '189000' }, { name: 'Morral', price: '199000' }]))
    expect(within(link).getByText('2 opciones')).toBeInTheDocument()
    expect(within(link).getAllByRole('link')).toHaveLength(1)
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

  it('paints the sticker and the type chip coral for a physical product', () => {
    const link = card(product())
    expect(link.querySelector('.sticker')).toHaveAttribute('data-tone', 'coral')
    expect(within(link).getByText('Físico').className).toContain('bg-coral-wash')
  })

  it('paints the sticker and the type chip lime for a digital product', () => {
    const link = card(product({ productType: 'DIGITAL' }))
    expect(link.querySelector('.sticker')).toHaveAttribute('data-tone', 'lime')
    expect(within(link).getByText('Digital').className).toContain('bg-lime-wash')
  })

  it('describes a real photo with the product name and treats the generated poster as decoration', () => {
    card(product({}, [{ name: 'Morral', price: '1000', image: 'https://cdn.example.test/m.jpg' }]))
    expect(screen.getByAltText('Morral')).toBeInTheDocument()
  })

  it('has no accessible image when it draws the poster', () => {
    card(product())
    expect(screen.queryByRole('img')).toBeNull()
  })

  it('links the whole card to the product through its title, and the store name to the store filter', () => {
    const article = card(product())
    const [title, store] = within(article).getAllByRole('link')
    expect(title).toHaveTextContent('Morral urbano 22 L')
    expect(title?.className).toContain('after:inset-0')
    expect(store).toHaveAccessibleName('Ver la tienda Páramo')
    expect(store).toHaveAttribute('href', '/?tienda=P%C3%A1ramo')
    expect(title?.contains(store ?? null)).toBe(false)
  })

  it('keeps the other filters of the catalogue when it filters by store', () => {
    const article = card(product(), '/?tipo=fisicos&orden=precio-asc')
    expect(within(article).getByRole('link', { name: 'Ver la tienda Páramo' })).toHaveAttribute('href', '/?tipo=fisicos&orden=precio-asc&tienda=P%C3%A1ramo')
  })

  it('shows every photo in the same 4:3 frame on a soft background', () => {
    const article = card(product({}, [{ name: 'Morral', price: '1000', image: '/productos/morral.jpg' }]))
    const frame = screen.getByAltText('Morral').closest('.aspect-\\[4\\/3\\]')
    expect(frame).not.toBeNull()
    expect(frame?.className).toContain('bg-cobalt-wash')
    expect(screen.getByAltText('Morral')).toHaveAttribute('loading', 'lazy')
    expect(article).toContainElement(frame as HTMLElement)
  })
})
