import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

/**
 * The frame of a store page: the same width and side padding as the header, so the title starts
 * on the same line as the logo. The content keeps a readable width but is not centred.
 */
export function StorePage({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className="mx-auto w-full max-w-7xl px-4 pt-10 pb-4 sm:px-6" data-testid="store-page">
      <div className={cn('max-w-4xl', className)}>{children}</div>
    </div>
  )
}
