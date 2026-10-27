import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { beforeEach, describe, expect, it } from 'vitest'
import { InvoicesPage } from '@/features/orders/InvoicesPage'
import { OrdersPage } from '@/features/orders/OrdersPage'
import { fakeBackend, renderApp } from '@/test/render'
import { AccountPage } from './AccountPage'

const emptyPage = { status: 200, body: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } }

function open(page: ReactElement) {
  localStorage.setItem('nexusmarket.session', JSON.stringify({ token: 't', userId: 'u', email: 'u@nexusmarket.test', role: 'BUYER', expiresAt: null }))
  fakeBackend({ 'GET /buyer/me/orders': () => emptyPage, 'GET /buyer/me/invoices': () => emptyPage })
  renderApp(page)
}

describe('account pages', () => {
  beforeEach(() => localStorage.clear())

  it('draws a parcel when there are no orders yet', async () => {
    open(<OrdersPage />)
    await screen.findByText('aún no tienes pedidos')
    expect(document.querySelector('svg[data-art="box"]')).not.toBeNull()
  })

  it('draws a receipt when there are no invoices yet', async () => {
    open(<InvoicesPage />)
    await screen.findByText('todavía no tienes facturas')
    expect(document.querySelector('svg[data-art="receipt"]')).not.toBeNull()
    expect(document.querySelector('svg[data-art="box"]')).toBeNull()
  })

  it.each([
    ['mis pedidos', <OrdersPage key="o" />],
    ['facturas', <InvoicesPage key="i" />],
    ['direcciones de entrega', <AccountPage key="a" />],
  ])('lines "%s" up with the logo of the header: same width and side padding', (title, page) => {
    open(page)
    const frame = screen.getByTestId('store-page')
    expect(frame.className).toContain('max-w-7xl')
    expect(frame.className).toContain('px-4')
    expect(frame.className).toContain('sm:px-6')
    expect(within(frame).getByRole('heading', { level: 1, name: title })).toBeInTheDocument()
  })
})

describe('addresses page', () => {
  beforeEach(() => localStorage.clear())

  it('explains in plain words, behind a disclosure, where the addresses are kept', async () => {
    open(<AccountPage />)
    const summary = screen.getByText('Tus direcciones se guardan en este navegador')
    const details = summary.closest('details') as HTMLDetailsElement
    expect(details.open).toBe(false)
    expect(screen.queryByText(/no ofrece una consulta del perfil/)).toBeNull()
    await userEvent.click(summary)
    expect(details.open).toBe(true)
    expect(within(details).getByText(/siguen guardadas en tu cuenta/)).toBeVisible()
  })

  it('offers no edit or delete action, which the API does not have', () => {
    open(<AccountPage />)
    expect(screen.queryByRole('button', { name: /editar|borrar|eliminar/i })).toBeNull()
  })
})
