import type { ReactNode } from 'react'

export function PageHeader({ title, eyebrow, description, actions }: { title: string; eyebrow?: string; description?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div className="max-w-2xl min-w-0 [overflow-wrap:anywhere]">
        {eyebrow && <p className="mb-2 text-sm font-semibold text-cobalt">{eyebrow}</p>}
        <h1 className="text-[clamp(2rem,5vw,3rem)] font-extrabold">{title}</h1>
        {description && <p className="mt-2 text-lg text-ink-soft">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  )
}
