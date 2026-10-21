import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { LoginPage } from './LoginPage'

describe('login flow', () => {
  beforeEach(() => localStorage.clear())

  it('validates the form before calling the backend', async () => {
    const { calls } = fakeBackend({})
    renderApp(<LoginPage />, { path: '/ingresar', route: '/ingresar' })

    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('escribe tu correo')).toBeInTheDocument()
    expect(screen.getByText('escribe tu contraseña')).toBeInTheDocument()
    expect(calls).toHaveLength(0)
  })

  it('shows a Spanish message when the credentials do not match', async () => {
    fakeBackend({
      'POST /auth/login': () => ({ status: 403, body: { status: 403, rule: 'UnauthorizedOperationException', message: 'invalid credentials' } }),
    })
    renderApp(<LoginPage />, { path: '/ingresar', route: '/ingresar' })

    await userEvent.type(screen.getByLabelText('Correo'), 'ana@nexusmarket.test')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'equivocada')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('el correo o la contraseña no coinciden')
    expect(localStorage.getItem('nexusmarket.session')).toBeNull()
  })

  it('stores the session and sends each role to its own home', async () => {
    const { calls } = fakeBackend({
      'POST /auth/login': () => ({
        status: 200,
        body: { token: 'header.e30.sig', userId: 'seller-1', email: 'tienda@nexusmarket.test', role: 'SELLER' },
      }),
    })
    renderApp(<LoginPage />, { path: '/ingresar', route: '/ingresar' })

    await userEvent.type(screen.getByLabelText('Correo'), 'tienda@nexusmarket.test')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Vendedor123')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/vendedor'))
    expect(calls[0]?.body).toEqual({ email: 'tienda@nexusmarket.test', password: 'Vendedor123' })
    const stored = JSON.parse(localStorage.getItem('nexusmarket.session') ?? '{}') as { role?: string; token?: string }
    expect(stored.role).toBe('SELLER')
    expect(stored.token).toBe('header.e30.sig')
  })
})
