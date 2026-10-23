import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { Field } from './Field'
import { PasswordInput } from './PasswordInput'

function field() {
  render(
    <Field label="Contraseña">
      <PasswordInput />
    </Field>,
  )
  return screen.getByLabelText('Contraseña', { selector: 'input' }) as HTMLInputElement
}

describe('PasswordInput', () => {
  it('hides what is typed, and offers a named button to reveal it', () => {
    const input = field()
    expect(input.type).toBe('password')
    const toggle = screen.getByRole('button', { name: 'Mostrar contraseña' })
    expect(toggle).toHaveAttribute('aria-pressed', 'false')
    expect(toggle).toHaveAttribute('type', 'button')
  })

  it('reveals and hides the password, keeping the typed value and announcing the state', async () => {
    const input = field()
    await userEvent.type(input, 'secreto-123')

    await userEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(input.type).toBe('text')
    expect(input.value).toBe('secreto-123')
    expect(screen.getByRole('button', { name: 'Ocultar contraseña' })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña' }))
    expect(input.type).toBe('password')
    expect(input.value).toBe('secreto-123')
  })

  it('can be reached and used with the keyboard, right after the input', async () => {
    const input = field()
    input.focus()

    await userEvent.tab()
    expect(screen.getByRole('button', { name: 'Mostrar contraseña' })).toHaveFocus()

    await userEvent.keyboard('{Enter}')
    expect(input.type).toBe('text')
    await userEvent.keyboard(' ')
    expect(input.type).toBe('password')
  })

  it('does not submit the form when the button is pressed', async () => {
    let submitted = false
    render(
      <form onSubmit={(e) => { e.preventDefault(); submitted = true }}>
        <Field label="Contraseña">
          <PasswordInput />
        </Field>
      </form>,
    )
    await userEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña' }))
    expect(submitted).toBe(false)
  })
})
