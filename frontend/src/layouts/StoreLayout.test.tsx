import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { StoreLayout } from './StoreLayout'

function shell(route: string) {
  renderApp(
    <Routes>
      <Route element={<StoreLayout />}>
        <Route index element={<p>pagina de inicio</p>} />
        <Route path="ingresar" element={<p>pagina de ingreso</p>} />
        <Route path="pedidos" element={<p>pagina de pedidos</p>} />
        <Route path="carrito" element={<p>pagina del carrito</p>} />
        <Route path="acerca" element={<p>pagina acerca</p>} />
      </Route>
    </Routes>,
    { path: '/*', route },
  )
}

function signIn(role: 'BUYER' | 'SELLER') {
  localStorage.setItem('nexusmarket.session', JSON.stringify({ token: 't', userId: 'u', email: 'u@nexusmarket.test', role, expiresAt: null }))
}

const header = () => screen.getByRole('banner')

describe('store header', () => {
  beforeEach(() => {
    localStorage.clear()
    fakeBackend({ 'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [{ id: 'i', productId: 'p', productVariantId: 'v', quantity: 3 }] } }) })
  })

  it('has a single filled button for a visitor: "Crear cuenta"; "Ingresar" is a text link', () => {
    shell('/')
    const create = within(header()).getByRole('link', { name: 'Crear cuenta' })
    const login = within(header()).getByRole('link', { name: 'Ingresar' })

    expect(create.className).toContain('bg-cobalt')
    expect(login.className).not.toMatch(/\bbg-(ink|cobalt|lime|card)\b/)
    expect(login.className).toContain('hover:underline')
    expect(login.className).toContain('focus-visible:underline')
  })

  it('marks the page you are on with a soft pill and aria-current, not with a filled button', () => {
    shell('/')
    const current = within(header()).getByRole('link', { name: 'Catálogo' })
    expect(current).toHaveAttribute('aria-current', 'page')
    expect(current.className).toContain('bg-cobalt-wash')
    expect(current.className).not.toContain('bg-ink ')
  })

  it('underlines "Ingresar" on its own page', () => {
    shell('/ingresar')
    const login = within(header()).getByRole('link', { name: 'Ingresar' })
    expect(login).toHaveAttribute('aria-current', 'page')
    expect(login.className).toMatch(/(^|\s)underline(\s|$)/)
  })

  it('keeps the cart always visible for a buyer, with its count, and marks it when open', async () => {
    signIn('BUYER')
    shell('/carrito')
    const cart = await within(header()).findByRole('link', { name: 'Carrito, 3 productos' })
    expect(cart).toHaveAttribute('aria-current', 'page')
    expect(cart.className).toContain('bg-cobalt-wash')
    expect(within(header()).queryByRole('link', { name: 'Crear cuenta' })).toBeNull()
  })

  it('offers the purchase menu to a buyer, in both navigations', async () => {
    signIn('BUYER')
    shell('/pedidos')
    expect(await screen.findAllByRole('link', { name: /Pedidos|Mis pedidos/ })).not.toHaveLength(0)
    expect(within(screen.getByRole('navigation', { name: 'Principal' })).getByRole('link', { name: 'Mis pedidos' })).toHaveAttribute('aria-current', 'page')
  })

  it('goes back to the visitor header after signing out', async () => {
    signIn('BUYER')
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: 'Salir' }))
    expect(within(header()).getByRole('link', { name: 'Crear cuenta' })).toBeInTheDocument()
  })
})

describe('store footer', () => {
  beforeEach(() => {
    localStorage.clear()
    fakeBackend({})
  })

  const footer = () => screen.getByRole('contentinfo')

  it('links to the pages that exist, and says it is an academic project', () => {
    shell('/')
    expect(within(footer()).getByRole('link', { name: 'Acerca del proyecto' })).toHaveAttribute('href', '/acerca')
    expect(within(footer()).getByRole('link', { name: 'Datos que guardamos' })).toHaveAttribute('href', '/acerca#datos')
    expect(within(footer()).getByRole('link', { name: 'Pagos de prueba' })).toHaveAttribute('href', '/acerca#pagos')
    expect(within(footer()).getByText(/Proyecto académico/)).toBeInTheDocument()
  })

  it('offers a visitor to sign in or sign up, and a buyer their orders', () => {
    shell('/')
    expect(within(footer()).getByRole('link', { name: 'Ingresar' })).toHaveAttribute('href', '/ingresar')
    expect(within(footer()).getByRole('link', { name: 'Crear cuenta' })).toHaveAttribute('href', '/registro')
  })

  it('points a signed-in buyer to their orders instead', async () => {
    signIn('BUYER')
    shell('/')
    expect(await within(footer()).findByRole('link', { name: 'Mis pedidos' })).toHaveAttribute('href', '/pedidos')
    expect(within(footer()).queryByRole('link', { name: 'Crear cuenta' })).toBeNull()
  })

  it('has labelled navigations', () => {
    shell('/')
    expect(within(footer()).getByRole('navigation', { name: 'Tienda' })).toBeInTheDocument()
    expect(within(footer()).getByRole('navigation', { name: 'Proyecto' })).toBeInTheDocument()
  })
})
