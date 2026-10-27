import type { ReactNode } from 'react'

// Illustrations for empty states, on the same lime disc as the default bag of EmptyState.

function Disc({ name, children }: { name: string; children: ReactNode }) {
  return (
    <svg data-art={name} width="64" height="64" viewBox="0 0 64 64" aria-hidden="true" fill="none" stroke="var(--color-ink)" strokeWidth="3" strokeLinejoin="round" strokeLinecap="round">
      <circle cx="32" cy="32" r="28" fill="var(--color-lime)" stroke="none" />
      {children}
    </svg>
  )
}

/** A closed parcel: no orders yet. */
export function BoxArt() {
  return (
    <Disc name="box">
      <path d="M18 25 32 18l14 7v15l-14 7-14-7z" />
      <path d="m18 25 14 7 14-7M32 32v15" />
      <path d="m25 21.5 14 7" strokeWidth="2.5" />
    </Disc>
  )
}

/** A receipt with a torn edge: no invoices yet. */
export function ReceiptArt() {
  return (
    <Disc name="receipt">
      <path d="M22 17h20v29l-3.3-2.2-3.4 2.2-3.3-2.2-3.4 2.2-3.3-2.2L22 46z" />
      <path d="M27 25h10M27 31h10M27 37h6" strokeWidth="2.5" />
    </Disc>
  )
}
