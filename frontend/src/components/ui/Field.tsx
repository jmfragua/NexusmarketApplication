import {
  cloneElement,
  forwardRef,
  isValidElement,
  useId,
  type InputHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react'
import { cn } from '@/lib/cn'

// The border meets 3:1 against its background (WCAG 1.4.11); focus adds a solid 2px ring on top of a
// cobalt border; an invalid field gets a coral border, doubled inside, next to the icon and the text
// of its message, so the state is never carried by colour alone.
const control =
  'block w-full rounded-xl border border-field bg-card px-3.5 text-ink placeholder:text-ink-faint transition-[border-color,box-shadow] hover:border-ink focus-visible:border-cobalt focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-cobalt aria-[invalid=true]:border-coral-deep aria-[invalid=true]:shadow-[inset_0_0_0_1px_var(--color-coral-deep)] aria-[invalid=true]:focus-visible:ring-coral-deep disabled:bg-paper disabled:text-ink-faint'

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement>>(function Input(
  { className, ...rest },
  ref,
) {
  return <input ref={ref} className={cn(control, 'h-11', className)} {...rest} />
})

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement>>(
  function Textarea({ className, ...rest }, ref) {
    return <textarea ref={ref} className={cn(control, 'min-h-24 py-2.5', className)} {...rest} />
  },
)

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement>>(function Select(
  { className, children, ...rest },
  ref,
) {
  return (
    <select
      ref={ref}
      className={cn(
        control,
        'h-11 appearance-none bg-[url("data:image/svg+xml;utf8,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 20 20%22 fill=%22%230b1233%22><path d=%22M5.5 7.5 10 12l4.5-4.5%22 stroke=%22%230b1233%22 stroke-width=%221.8%22 fill=%22none%22 stroke-linecap=%22round%22/></svg>")] bg-[length:1.1rem] bg-[right_0.8rem_center] bg-no-repeat pr-10',
        className,
      )}
      {...rest}
    >
      {children}
    </select>
  )
})

interface FieldProps {
  label: string
  hint?: ReactNode
  error?: string
  className?: string
  /** Always shown under the control, even with an error: the rules of a password, a match check. */
  extra?: ReactNode
  /** A single form control; the field wires `id`, `aria-invalid` and `aria-describedby` into it. */
  children: ReactElement<Record<string, unknown>>
}

function ErrorIcon() {
  return (
    <svg viewBox="0 0 20 20" className="mt-0.5 size-4 shrink-0" fill="none" aria-hidden="true">
      <circle cx="10" cy="10" r="8.25" stroke="currentColor" strokeWidth="1.8" />
      <path d="M10 5.8v5" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
      <circle cx="10" cy="13.9" r="1.1" fill="currentColor" />
    </svg>
  )
}

export function Field({ label, hint, error, extra, className, children }: FieldProps) {
  const id = useId()
  // The hint is not rendered while there is an error, so it must not be referenced either.
  const hintId = hint && !error ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const extraId = extra ? `${id}-extra` : undefined
  const describedBy = [hintId, errorId, extraId].filter(Boolean).join(' ') || undefined
  const wired = isValidElement(children)
    ? cloneElement(children, { id, 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })
    : children

  return (
    <div className={cn('grid gap-1.5', className)}>
      <label htmlFor={id} className="text-sm font-semibold text-ink">
        {label}
      </label>
      {wired}
      {hint && !error && (
        <div id={hintId} className="text-sm text-ink-soft">
          {hint}
        </div>
      )}
      {error && (
        <p id={errorId} className="flex items-start gap-1.5 text-sm font-medium text-coral-deep" role="alert">
          <ErrorIcon />
          <span>{error}</span>
        </p>
      )}
      {extra && (
        <div id={extraId} className="text-sm text-ink-soft">
          {extra}
        </div>
      )}
    </div>
  )
}
