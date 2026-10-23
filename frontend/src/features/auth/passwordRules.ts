export const MIN_PASSWORD_LENGTH = 8

export interface PasswordRule {
  label: string
  met: boolean
  /** A recommendation is shown and checked, but does not block the form. */
  recommended?: boolean
}

export function passwordRules(password: string): PasswordRule[] {
  return [
    { label: `Al menos ${MIN_PASSWORD_LENGTH} caracteres`, met: password.length >= MIN_PASSWORD_LENGTH },
    { label: 'Combina letras y números', met: /\p{L}/u.test(password) && /\d/.test(password), recommended: true },
  ]
}
