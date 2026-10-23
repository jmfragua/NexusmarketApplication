import { cn } from '@/lib/cn'
import { passwordRules } from './passwordRules'

function Mark({ met }: { met: boolean }) {
  // A check in a filled circle against an empty ring: the state reads without colour.
  return met ? (
    <svg viewBox="0 0 20 20" className="size-4 shrink-0" aria-hidden="true">
      <circle cx="10" cy="10" r="9" fill="currentColor" />
      <path d="m5.8 10.4 2.7 2.7 5.7-5.9" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  ) : (
    <svg viewBox="0 0 20 20" className="size-4 shrink-0" aria-hidden="true">
      <circle cx="10" cy="10" r="8.2" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  )
}

/** The rules of the password, checked live as it is typed. */
export function PasswordRules({ password }: { password: string }) {
  return (
    <ul className="grid gap-1" aria-label="Reglas de la contraseña">
      {passwordRules(password).map((rule) => (
        <li key={rule.label} className={cn('flex items-center gap-2', rule.met ? 'font-medium text-mint' : 'text-ink-soft')}>
          <Mark met={rule.met} />
          <span>
            {rule.label}
            {rule.recommended && <span className="text-ink-soft"> (recomendado)</span>}
            <span className="sr-only">{rule.met ? ', cumplida' : ', pendiente'}</span>
          </span>
        </li>
      ))}
    </ul>
  )
}

/** Says, as the second password is typed, whether it equals the first. Nothing until there is text. */
export function PasswordMatch({ password, confirm }: { password: string; confirm: string }) {
  if (confirm === '') return null
  const matches = password === confirm
  return (
    <output aria-live="polite" className={cn('flex items-center gap-2', matches ? 'font-medium text-mint' : 'text-ink-soft')}>
      <Mark met={matches} />
      <span>{matches ? 'Las contraseñas coinciden' : 'Todavía no coinciden'}</span>
    </output>
  )
}
