import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PasswordMatch, PasswordRules } from './PasswordFeedback'
import { MIN_PASSWORD_LENGTH, passwordRules } from './passwordRules'

describe('passwordRules', () => {
  it('requires the minimum length, and only recommends mixing letters and numbers', () => {
    expect(passwordRules('').map((r) => r.met)).toEqual([false, false])
    expect(passwordRules('abcdefgh').map((r) => r.met)).toEqual([true, false])
    expect(passwordRules('abc12').map((r) => r.met)).toEqual([false, true])
    expect(passwordRules('abc12345').map((r) => r.met)).toEqual([true, true])
    expect(passwordRules('x'.repeat(MIN_PASSWORD_LENGTH - 1))[0]?.met).toBe(false)
    expect(passwordRules('x')[1]?.recommended).toBe(true)
    expect(passwordRules('x')[0]?.recommended).toBeUndefined()
  })

  it('counts accented letters as letters', () => {
    expect(passwordRules('contraseña1')[1]?.met).toBe(true)
  })
})

describe('PasswordRules', () => {
  it('reports each rule as pending or met in text, not only by colour', () => {
    const { rerender } = render(<PasswordRules password="abc" />)
    const list = screen.getByRole('list', { name: 'Reglas de la contraseña' })
    expect(list).toHaveTextContent('Al menos 8 caracteres, pendiente')
    expect(list).toHaveTextContent('Combina letras y números (recomendado), pendiente')

    rerender(<PasswordRules password="abc12345" />)
    expect(list).toHaveTextContent('Al menos 8 caracteres, cumplida')
    expect(list).toHaveTextContent('Combina letras y números (recomendado), cumplida')
  })
})

describe('PasswordMatch', () => {
  it('says nothing until the second password has text', () => {
    const { container } = render(<PasswordMatch password="abc12345" confirm="" />)
    expect(container).toBeEmptyDOMElement()
  })

  it('says whether the two passwords are the same', () => {
    const { rerender } = render(<PasswordMatch password="abc12345" confirm="abc1" />)
    expect(screen.getByText('Todavía no coinciden')).toBeInTheDocument()

    rerender(<PasswordMatch password="abc12345" confirm="abc12345" />)
    expect(screen.getByText('Las contraseñas coinciden')).toBeInTheDocument()
  })
})
