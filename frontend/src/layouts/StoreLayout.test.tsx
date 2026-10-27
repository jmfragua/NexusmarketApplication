import { act, screen, waitFor, within } from '@testing-library/react'
import { focusManager } from '@tanstack/react-query'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { RequireRole } from '@/features/auth/RequireRole'
import { StoreLayout } from './StoreLayout'

function shell(route: string) {
  renderApp(
    <Routes>
      <Route element={<StoreLayout />}>
        <Route index element={<p>pagina de inicio</p>} />
        <Route path="ingresar" element={<p>pagina de ingreso</p>} />
        <Route path="pedidos" element={<p>pagina de pedidos</p>} />
        <Route path="registro" element={<p>pagina de registro</p>} />
        <Route
          path="carrito"
          element={
            <RequireRole allow="BUYER">
              <p>pagina del carrito</p>
            </RequireRole>
          }
        />
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

  it('shows the cart to a visitor too, and sends them to sign in when they open it', async () => {
    shell('/')
    const cart = within(header()).getByRole('link', { name: 'Carrito: ingresa para ver tu carrito' })
    expect(cart).toHaveAttribute('href', '/carrito')
    await userEvent.click(cart)
    expect(screen.getByTestId('location')).toHaveTextContent('/ingresar')
    expect(screen.getByText('pagina de ingreso')).toBeInTheDocument()
  })

  it('marks "Crear cuenta" when you are on the sign-up page', () => {
    shell('/registro')
    const create = within(header()).getByRole('link', { name: 'Crear cuenta' })
    expect(create).toHaveAttribute('aria-current', 'page')
    expect(create.className).toContain('bg-cobalt-deep')
    // A ring would look like a keyboard focus ring.
    expect(create.className).not.toMatch(/\bring-(2|cobalt|offset)/)
  })

  it('underlines "Ingresar" on its own page', () => {
    shell('/ingresar')
    const login = within(header()).getByRole('link', { name: 'Ingresar' })
    expect(login).toHaveAttribute('aria-current', 'page')
    expect(login.className).toMatch(/(^|\s)underline(\s|$)/)
  })

  it('keeps the cart always visible for a buyer, with its count, and marks it softly on the cart page', async () => {
    signIn('BUYER')
    shell('/carrito')
    const cart = await within(header()).findByRole('button', { name: 'Carrito, 3 productos' })
    expect(cart).toHaveAttribute('aria-haspopup', 'dialog')
    expect(cart.className).toContain('bg-cobalt-wash')
    expect(cart.className).not.toMatch(/ring-cobalt(\s|$)/)
    expect(within(header()).queryByRole('link', { name: 'Crear cuenta' })).toBeNull()
  })

  it('shows the initial of the account and says who is signed in, next to "Salir"', async () => {
    signIn('BUYER')
    shell('/')
    expect(await within(header()).findByText('Sesión iniciada como u@nexusmarket.test')).toBeInTheDocument()
    expect(within(header()).getByText('U')).toHaveAttribute('aria-hidden', 'true')
    expect(within(header()).getByRole('button', { name: 'Salir' })).toBeInTheDocument()
  })

  it('gives the other roles the avatar too', () => {
    signIn('SELLER')
    shell('/')
    expect(within(header()).getByText('Sesión iniciada como u@nexusmarket.test')).toBeInTheDocument()
  })

  it('makes the header links show a ring for the keyboard only', () => {
    shell('/')
    for (const link of within(header()).getAllByRole('link')) expect(link.className).not.toMatch(/(^|\s)focus:ring/)
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

describe('cart side panel', () => {
  let units = 3
  beforeEach(() => {
    localStorage.clear()
    units = 3
    fakeBackend({
      'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [{ id: 'i', productId: 'p', productVariantId: 'v', quantity: units }] } }),
      'GET /catalog/products/p': () => ({
        status: 200,
        body: { id: 'p', sellerId: 's', productType: 'PHYSICAL', status: 'PUBLISHED', visibleInCatalog: true, variants: [{ id: 'v', attributes: { name: 'Morral urbano', price: '100000', currency: 'COP', Color: 'Arena' } }] },
      }),
    })
    signIn('BUYER')
  })

  it('opens from the cart button with the lines, the total and the two ways out', async () => {
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: 'Carrito, 3 productos' }))
    const panel = screen.getByRole('dialog', { name: 'tu carrito' })
    expect(await within(panel).findByText('Morral urbano')).toBeInTheDocument()
    expect(within(panel).getByText('3 artículos en tu carrito')).toBeInTheDocument()
    expect(within(panel).getByText('Arena')).toBeInTheDocument()
    expect(await within(panel).findByText(/300\.000/, { selector: '.font-display' })).toBeInTheDocument()
    expect(within(panel).getByRole('link', { name: 'Ir a pagar' })).toHaveAttribute('href', '/pagar')
    expect(within(panel).getByRole('button', { name: 'Seguir comprando' })).toBeInTheDocument()
  })

  it('closes with Esc and gives the focus back to the cart button', async () => {
    shell('/')
    const button = await within(header()).findByRole('button', { name: 'Carrito, 3 productos' })
    await userEvent.click(button)
    expect(screen.getByRole('dialog')).toBeInTheDocument()
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).toBeNull()
    await waitFor(() => expect(button).toHaveFocus())
  })

  it('keeps the keyboard inside while open', async () => {
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: 'Carrito, 3 productos' }))
    const panel = screen.getByRole('dialog')
    await within(panel).findByText('Morral urbano')
    for (let i = 0; i < 8; i++) {
      await userEvent.tab()
      expect(panel.contains(document.activeElement)).toBe(true)
    }
  })

  it('"Seguir comprando" closes the panel', async () => {
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: 'Carrito, 3 productos' }))
    await userEvent.click(screen.getByRole('button', { name: 'Seguir comprando' }))
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('makes the count hop when the cart grows, not on the first load', async () => {
    shell('/')
    await within(header()).findByRole('button', { name: 'Carrito, 3 productos' })
    expect(within(header()).getByText('3')).not.toHaveAttribute('data-bumped')

    units = 4
    act(() => {
      focusManager.setFocused(false)
      focusManager.setFocused(true)
    })
    await within(header()).findByRole('button', { name: 'Carrito, 4 productos' })
    const badge = within(header()).getByText('4')
    expect(badge).toHaveAttribute('data-bumped', 'true')
    expect(badge.className).toContain('motion-safe:animate-bump')
    focusManager.setFocused(undefined)
  })

  it('shows no misleading total when no line can be priced', async () => {
    fakeBackend({
      'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: [{ id: 'i', productId: 'gone', productVariantId: 'v', quantity: 1 }] } }),
      'GET /catalog/products/gone': () => ({ status: 404, body: { status: 404, rule: 'EntityNotFoundException', message: 'no existe' } }),
    })
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: 'Carrito, 1 producto' }))
    const panel = screen.getByRole('dialog')
    expect(await within(panel).findByText('Producto no disponible')).toBeInTheDocument()
    expect(within(panel).getByText('—', { selector: '.font-display' })).toBeInTheDocument()
    expect(within(panel).getByRole('button', { name: 'Ir a pagar' })).toBeDisabled()
  })
})

describe('cart side panel controls', () => {
  let units = 1
  let calls: Array<{ key: string; body: unknown }> = []
  beforeEach(() => {
    localStorage.clear()
    units = 1
    const backend = fakeBackend({
      'GET /buyer/me/cart': () => ({ status: 200, body: { id: 'c', buyerId: 'u', items: units ? [{ id: 'i', productId: 'p', productVariantId: 'v', quantity: units }] : [] } }),
      'PATCH /buyer/me/cart/items/i': (init) => {
        units = (JSON.parse(String(init.body)) as { quantity: number }).quantity
        return { status: 200, body: { id: 'c', buyerId: 'u', items: [{ id: 'i', productId: 'p', productVariantId: 'v', quantity: units }] } }
      },
      'DELETE /buyer/me/cart/items/i': () => {
        units = 0
        return { status: 200, body: { id: 'c', buyerId: 'u', items: [] } }
      },
      'GET /catalog/products/p': () => ({
        status: 200,
        body: { id: 'p', sellerId: 's', productType: 'PHYSICAL', status: 'PUBLISHED', visibleInCatalog: true, variants: [{ id: 'v', attributes: { name: 'Mouse inalámbrico Pulso', price: '59900', currency: 'COP' } }] },
      }),
    })
    calls = backend.calls
    signIn('BUYER')
  })

  async function openPanel() {
    shell('/')
    await userEvent.click(await within(header()).findByRole('button', { name: /^Carrito, / }))
    const panel = screen.getByRole('dialog', { name: 'tu carrito' })
    await within(panel).findByRole('link', { name: 'Mouse inalámbrico Pulso' })
    return panel
  }

  it('says how many items, in the singular, and does not repeat the variant name', async () => {
    const panel = await openPanel()
    expect(within(panel).getByText('1 artículo en tu carrito')).toBeInTheDocument()
    expect(within(panel).getAllByText('Mouse inalámbrico Pulso')).toHaveLength(1)
    expect(within(panel).queryByText(/precios del catálogo/)).toBeNull()
  })

  it('links each product to its page and closes the panel on the way', async () => {
    const panel = await openPanel()
    const link = within(panel).getByRole('link', { name: 'Mouse inalámbrico Pulso' })
    expect(link).toHaveAttribute('href', '/productos/p')
    await userEvent.click(link)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('changes the quantity and removes a line with the cart endpoints', async () => {
    const panel = await openPanel()
    await userEvent.click(within(panel).getByRole('button', { name: 'Sumar uno' }))
    expect(calls.some((c) => c.key === 'PATCH /buyer/me/cart/items/i' && (c.body as { quantity: number }).quantity === 2)).toBe(true)
    expect(await within(panel).findByText('2 artículos en tu carrito')).toBeInTheDocument()
    await userEvent.click(within(panel).getByRole('button', { name: 'Quitar Mouse inalámbrico Pulso del carrito' }))
    expect(calls.some((c) => c.key === 'DELETE /buyer/me/cart/items/i')).toBe(true)
    expect(await within(panel).findByText('todavía no tienes productos.')).toBeInTheDocument()
  })

  it('dims the page behind with a soft overlay and no blur, and makes no promise about shipping or taxes', async () => {
    const panel = await openPanel()
    const overlay = document.querySelector('.bg-ink\\/35') as HTMLElement
    expect(overlay).not.toBeNull()
    expect(overlay.className).not.toContain('blur')
    expect(panel).not.toHaveTextContent(/impuestos/)
  })
})
