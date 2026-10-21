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

const control =
  'block w-full rounded-xl border border-line-strong bg-card px-3.5 text-ink placeholder:text-ink-faint transition-colors hover:border-ink/50 focus-visible:border-cobalt focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-cobalt/25 aria-[invalid=true]:border-coral-deep aria-[invalid=true]:ring-coral/20 disabled:bg-paper disabled:text-ink-faint'

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
  /** A single form control; the field wires `id`, `aria-invalid` and `aria-describedby` into it. */
  children: ReactElement<Record<string, unknown>>
}

export function Field({ label, hint, error, className, children }: FieldProps) {
  const id = useId()
  const hintId = hint ? `${id}-hint` : undefined
  const errorId = error ? `${id}-error` : undefined
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined
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
        <p id={hintId} className="text-sm text-ink-soft">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-sm font-medium text-coral-deep" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}
