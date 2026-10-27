import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { configureApiClient } from '@/api/client'
import type { ProductResponse } from '@/api/types'
import { fakeBackend, renderApp } from '@/test/render'
import { CatalogPage } from './CatalogPage'

const BRANDS = ['Páramo', 'Kilo', 'Malecón']

/** 21 products, as in the local catalogue: prices 10.000, 20.000, … 210.000. */
const catalogue: ProductResponse[] = Array.from({ length: 21 }, (_, i) => ({
  id: `p-${i + 1}`,
  sellerId: 's',
  productType: i % 4 === 3 ? 'DIGITAL' : 'PHYSICAL',
  status: 'PUBLISHED',
  visibleInCatalog: true,
  variants: [{ id: `v-${i + 1}`, attributes: { name: `Producto número ${i + 1}`, brand: BRANDS[i % 3] ?? 'Kilo', price: String((i + 1) * 10000), currency: 'COP' } }],
}))

function serve(products = catalogue) {
  fakeBackend({ 'GET /catalog/products': () => ({ status: 200, body: { content: products, page: 0, size: 200, totalElements: products.length, totalPages: 1 } }) })
}

const catalog = () => screen.getByRole('region', { name: 'el catálogo' })
const cardTitles = () => within(catalog()).queryAllByRole('heading', { level: 3 }).map((h) => h.textContent)

async function open(route = '/') {
  renderApp(<CatalogPage />, { route })
  await screen.findAllByRole('article')
}

describe('catalogue', () => {
  beforeEach(() => serve())

  it('shows card skeletons while the catalogue loads', () => {
    renderApp(<CatalogPage />)
    expect(screen.getByTestId('catalog-skeletons')).toBeInTheDocument()
    expect(screen.getByText('cargando productos…')).toBeInTheDocument()
  })

  it('shows 12 products, then the rest with "Ver más productos"', async () => {
    await open()
    expect(screen.getAllByRole('article')).toHaveLength(12)
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('21 productos')
    expect(screen.getByText('mostrando 12 de 21')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Ver más productos' }))
    expect(screen.getAllByRole('article')).toHaveLength(21)
    expect(screen.getByText('mostrando 21 de 21')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Ver más productos' })).toBeNull()
  })

  it('orders by price, lowest or highest first', async () => {
    await open()
    await userEvent.selectOptions(screen.getByLabelText('Ordenar por'), 'precio-desc')
    expect(cardTitles().slice(0, 2)).toEqual(['Producto número 21', 'Producto número 20'])
    await userEvent.selectOptions(screen.getByLabelText('Ordenar por'), 'precio-asc')
    expect(cardTitles().slice(0, 2)).toEqual(['Producto número 1', 'Producto número 2'])
  })

  it('filters by price range together with the type filter, and counts the result', async () => {
    await open()
    await userEvent.type(screen.getByLabelText('Precio mínimo'), '50000')
    await userEvent.type(screen.getByLabelText('Precio máximo'), '100.000')
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('6 productos')
    await userEvent.click(screen.getByRole('button', { name: 'Digitales' }))
    // Between 50.000 and 100.000, only products 8 (digital) qualifies.
    expect(cardTitles()).toEqual(['Producto número 8'])
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('1 producto')
  })

  it('filters by the store picked on a card, marks its chip and keeps the other filters', async () => {
    await open('/?tipo=fisicos')
    const card = screen.getAllByRole('article')[0] as HTMLElement
    await userEvent.click(within(card).getByRole('link', { name: 'Ver la tienda Páramo' }))

    const stores = screen.getByRole('group', { name: 'Tiendas' })
    expect(within(stores).getByRole('button', { name: 'Páramo' })).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getAllByRole('article').every((a) => a.textContent?.includes('Páramo'))).toBe(true)
    expect(screen.getByRole('button', { name: 'Físicos' })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(within(stores).getByRole('button', { name: 'Páramo' }))
    expect(within(stores).getByRole('button', { name: 'Páramo' })).toHaveAttribute('aria-pressed', 'false')
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('16 productos')
  })

  it('offers one chip per brand of the catalogue, which filters like the card link', async () => {
    await open()
    const stores = screen.getByRole('group', { name: 'Tiendas' })
    expect(within(stores).getAllByRole('button').map((b) => b.textContent)).toEqual(['Kilo', 'Malecón', 'Páramo'])
    await userEvent.click(within(stores).getByRole('button', { name: 'Kilo' }))
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('7 productos')
    await userEvent.click(within(stores).getByRole('button', { name: 'Malecón' }))
    expect(within(stores).getByRole('button', { name: 'Kilo' })).toHaveAttribute('aria-pressed', 'false')
    expect(within(stores).getByRole('button', { name: 'Malecón' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('shows "Limpiar filtros" only while a filter is on', async () => {
    await open()
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).toBeNull()
    await userEvent.click(within(screen.getByRole('group', { name: 'Tiendas' })).getByRole('button', { name: 'Kilo' }))
    await userEvent.click(screen.getByRole('button', { name: 'Limpiar filtros' }))
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).toBeNull()
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('21 productos')
  })

  it('keeps a removable chip for a store that comes in the URL but is not in the catalogue', async () => {
    renderApp(<CatalogPage />, { route: '/?tienda=Otra' })
    await screen.findByRole('heading', { name: 'ningún producto coincide' })
    await userEvent.click(screen.getByRole('button', { name: 'Quitar el filtro de la tienda Otra' }))
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('21 productos')
  })

  it('explains clearly when nothing matches, and "Limpiar filtros" brings everything back', async () => {
    await open()
    await userEvent.type(screen.getByRole('searchbox', { name: 'Buscar en el catálogo' }), 'paraguas')
    expect(screen.getByRole('heading', { name: 'ningún producto coincide' })).toBeInTheDocument()
    expect(screen.getByText(/nada coincide con «paraguas»/)).toBeInTheDocument()
    expect(screen.getByTestId('catalog-count')).toHaveTextContent('0 productos')

    const [clear] = screen.getAllByRole('button', { name: 'Limpiar filtros' })
    await userEvent.click(clear as HTMLElement)
    expect(screen.getAllByRole('article')).toHaveLength(12)
    expect(screen.queryByRole('button', { name: 'Limpiar filtros' })).toBeNull()
  })

  it('starts "ver más" again from 12 when a filter changes', async () => {
    await open()
    await userEvent.click(screen.getByRole('button', { name: 'Ver más productos' }))
    expect(screen.getAllByRole('article')).toHaveLength(21)
    await userEvent.click(screen.getByRole('button', { name: 'Físicos' }))
    expect(screen.getAllByRole('article')).toHaveLength(12)
  })

  it('explains how buying works in three steps, between the hero and the catalogue', async () => {
    await open()
    const steps = screen.getByRole('region', { name: 'cómo funciona' })
    expect(within(steps).getAllByRole('listitem')).toHaveLength(3)
    expect(within(steps).getByRole('heading', { name: 'paga una sola vez' })).toBeInTheDocument()
    expect(steps.compareDocumentPosition(catalog()) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('works with the demo products when the backend is down', async () => {
    configureApiClient({ fetchImpl: (() => Promise.reject(new TypeError('Failed to fetch'))) as typeof fetch, baseUrl: 'http://api.test' })
    await open()
    expect(screen.getByText('estás viendo productos de ejemplo')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Digitales' }))
    await userEvent.selectOptions(screen.getByLabelText('Ordenar por'), 'precio-asc')
    expect(cardTitles()).toEqual(['Plantillas de presupuesto personal', 'Libro digital: cocina de mercado', 'Curso de fotografía con celular'])
  })

  it('names each featured product of the hero once, with its price, and describes its photo', async () => {
    const withPhoto = catalogue.map((p, i) => (i === 0 ? { ...p, variants: [{ id: 'v', attributes: { ...p.variants[0]?.attributes, image: '/productos/morral.jpg' } }] } : p))
    serve(withPhoto)
    await open()
    const featured = screen.getByRole('list', { name: 'Productos destacados' })
    const first = within(featured).getAllByRole('link')[0] as HTMLElement
    expect(first).toHaveAccessibleName(/^Producto número 1, \$\s10\.000$/)
    expect(within(first).getByRole('img')).toHaveAttribute('alt', 'Producto número 1')
  })
})
