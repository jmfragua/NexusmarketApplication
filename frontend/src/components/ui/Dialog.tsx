import * as RadixDialog from '@radix-ui/react-dialog'
import type { ReactNode } from 'react'

interface DialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description?: ReactNode
  children: ReactNode
  wide?: boolean
}

export function Dialog({ open, onOpenChange, title, description, children, wide = false }: DialogProps) {
  return (
    <RadixDialog.Root open={open} onOpenChange={onOpenChange}>
      <RadixDialog.Portal>
        <RadixDialog.Overlay className="fixed inset-0 z-40 bg-ink/45 backdrop-blur-[2px] data-[state=open]:animate-[rise_200ms_ease-out]" />
        <RadixDialog.Content className={`fixed inset-x-3 bottom-3 z-50 max-h-[88vh] overflow-y-auto rounded-[var(--radius-card)] bg-card p-6 shadow-[0_24px_60px_-20px_rgba(11,18,51,0.45)] outline-none sm:inset-x-auto sm:top-1/2 sm:bottom-auto sm:left-1/2 ${wide ? 'sm:max-w-3xl' : 'sm:max-w-lg'} sm:w-full sm:-translate-x-1/2 sm:-translate-y-1/2 data-[state=open]:animate-rise`}>
          <div className="mb-4 flex items-start justify-between gap-4">
            <div>
              <RadixDialog.Title className="font-display text-2xl font-bold">{title}</RadixDialog.Title>
              {description ? (
                <RadixDialog.Description className="mt-1 text-ink-soft">{description}</RadixDialog.Description>
              ) : (
                <RadixDialog.Description className="sr-only">{title}</RadixDialog.Description>
              )}
            </div>
            <RadixDialog.Close className="grid size-9 shrink-0 place-items-center rounded-full text-ink-soft hover:bg-ink/6 hover:text-ink" aria-label="Cerrar">
              <svg viewBox="0 0 20 20" className="size-5" aria-hidden="true">
                <path d="m5 5 10 10M15 5 5 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
            </RadixDialog.Close>
          </div>
          {children}
        </RadixDialog.Content>
      </RadixDialog.Portal>
    </RadixDialog.Root>
  )
}
