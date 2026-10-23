import type { ReactNode } from 'react'

/** A message about a whole form that was rejected: icon, bold lead and text, besides its colour. */
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="flex items-start gap-2.5 rounded-xl border border-coral-deep/40 bg-coral-wash px-4 py-3 text-sm font-medium text-coral-deep">
      <svg viewBox="0 0 20 20" className="mt-0.5 size-5 shrink-0" fill="none" aria-hidden="true">
        <circle cx="10" cy="10" r="8.25" stroke="currentColor" strokeWidth="1.8" />
        <path d="M10 5.8v5" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round" />
        <circle cx="10" cy="13.9" r="1.1" fill="currentColor" />
      </svg>
      <span>{children}</span>
    </p>
  )
}
