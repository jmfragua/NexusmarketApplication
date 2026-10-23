import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { Field, Input } from './Field'

describe('Field', () => {
  it('links the label to the control', () => {
    render(
      <Field label="Correo">
        <Input />
      </Field>,
    )
    expect(screen.getByLabelText('Correo')).toBeInstanceOf(HTMLInputElement)
  })

  it('shows an error with an icon and a message, not just a colour, and ties it to the control', () => {
    render(
      <Field label="Correo" error="escribe tu correo">
        <Input />
      </Field>,
    )
    const input = screen.getByLabelText('Correo')
    const alert = screen.getByRole('alert')

    expect(alert).toHaveTextContent('escribe tu correo')
    expect(alert.querySelector('svg')).not.toBeNull()
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input.getAttribute('aria-describedby')).toContain(alert.id)
  })

  it('replaces the hint by the error, but keeps the extra content', () => {
    render(
      <Field label="Contraseña" hint="una ayuda" error="usa más caracteres" extra={<span>reglas de la contraseña</span>}>
        <Input />
      </Field>,
    )
    expect(screen.queryByText('una ayuda')).toBeNull()
    expect(screen.getByText('reglas de la contraseña')).toBeInTheDocument()
    const describedBy = screen.getByLabelText('Contraseña').getAttribute('aria-describedby') ?? ''
    expect(describedBy.split(' ')).toHaveLength(2)
  })

  it('describes the control by its hint when there is no error', () => {
    render(
      <Field label="Dirección" hint="con ciudad">
        <Input />
      </Field>,
    )
    const input = screen.getByLabelText('Dirección')
    expect(input).not.toHaveAttribute('aria-invalid')
    expect(document.getElementById(input.getAttribute('aria-describedby') ?? '')).toHaveTextContent('con ciudad')
  })
})
