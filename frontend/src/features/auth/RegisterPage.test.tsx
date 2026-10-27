import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { fakeBackend, renderApp } from '@/test/render'
import { RegisterPage } from './RegisterPage'

function open() {
  fakeBackend({})
  renderApp(<RegisterPage />, { path: '/registro', route: '/registro' })
}

describe('register page', () => {
  beforeEach(() => localStorage.clear())

  it('checks the password rules as it is typed', async () => {
    open()
    const rules = screen.getByRole('list', { name: 'Reglas de la contraseña' })
    expect(rules).toHaveTextContent('Al menos 8 caracteres, pendiente')

    await userEvent.type(screen.getByLabelText('Contraseña', { selector: 'input' }), 'Compra12345')

    expect(rules).toHaveTextContent('Al menos 8 caracteres, cumplida')
    expect(rules).toHaveTextContent('Combina letras y números (recomendado), cumplida')
  })

  it('tells whether the repeated password matches, live', async () => {
    open()
    await userEvent.type(screen.getByLabelText('Contraseña', { selector: 'input' }), 'Compra12345')
    expect(screen.queryByText('Las contraseñas coinciden')).toBeNull()

    await userEvent.type(screen.getByLabelText('Repite la contraseña', { selector: 'input' }), 'Compra1')
    expect(screen.getByText('Todavía no coinciden')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Repite la contraseña', { selector: 'input' }), '2345')
    expect(screen.getByText('Las contraseñas coinciden')).toBeInTheDocument()
  })

  it('reveals each password on its own button', async () => {
    open()
    const [first, second] = screen.getAllByRole('button', { name: 'Mostrar contraseña' })
    await userEvent.click(first as HTMLElement)

    expect((screen.getByLabelText('Contraseña', { selector: 'input' }) as HTMLInputElement).type).toBe('text')
    expect((screen.getByLabelText('Repite la contraseña', { selector: 'input' }) as HTMLInputElement).type).toBe('password')
    expect(second).toBeDefined()
  })

  it('uses no placeholders at all, so the fields are coherent', () => {
    open()
    expect(document.querySelectorAll('input[placeholder]')).toHaveLength(0)
  })

  it('still refuses mismatching passwords on submit', async () => {
    open()
    await userEvent.type(screen.getByLabelText('Nombre completo'), 'Lucía Prueba')
    await userEvent.type(screen.getByLabelText('Correo'), 'lucia@nexusmarket.test')
    await userEvent.type(screen.getByLabelText('Documento de identidad'), '123456789')
    await userEvent.type(screen.getByLabelText('Dirección principal'), 'Calle 10 # 5-20, Bogotá')
    await userEvent.type(screen.getByLabelText('Contraseña', { selector: 'input' }), 'Compra12345')
    await userEvent.type(screen.getByLabelText('Repite la contraseña', { selector: 'input' }), 'Otra123456')
    await userEvent.click(screen.getByRole('button', { name: 'Crear mi cuenta' }))

    expect(await screen.findByText('las contraseñas no coinciden')).toBeInTheDocument()
  })

  it('gives the email and the identity document a full row each', () => {
    open()
    const email = screen.getByLabelText('Correo')
    const document = screen.getByLabelText('Documento de identidad')
    for (const input of [email, document]) {
      expect(input.closest('.sm\\:grid-cols-2')).toBeNull()
    }
  })

  it('shows six real product photos in the side panel, with the three labels', () => {
    open()
    const panel = document.querySelector('aside') as HTMLElement
    const photos = [...panel.querySelectorAll('img')].map((img) => img.getAttribute('src'))
    expect(photos).toHaveLength(6)
    expect(photos.every((src) => src?.startsWith('/productos/'))).toBe(true)
    expect(panel).toHaveTextContent('pago único')
    expect(panel).toHaveTextContent('envío rastreado')
    expect(panel).toHaveTextContent('muchas tiendas')
    expect(panel.className).toContain('md:sticky')
  })
})
