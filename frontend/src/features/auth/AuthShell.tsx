import type { ReactNode } from 'react'

export function AuthShell({ title, description, children, aside }: { title: string; description: ReactNode; children: ReactNode; aside?: ReactNode }) {
  return (
    <div className="mx-auto grid max-w-6xl gap-10 px-4 py-10 sm:px-6 md:grid-cols-[1fr_0.9fr] md:py-16">
      <div className="motion-safe:animate-rise">
        <h1 className="text-[clamp(2.2rem,5vw,3.4rem)] font-extrabold">{title}</h1>
        <p className="mt-3 max-w-md text-lg text-ink-soft">{description}</p>
        <div className="mt-8 max-w-md">{children}</div>
      </div>
      <aside className="relative hidden overflow-hidden rounded-[1.8rem] bg-ink p-8 text-white md:block" aria-hidden="true">
        <div className="grain absolute inset-0 opacity-30" />
        <div className="relative flex h-full flex-col justify-between gap-10">
          {aside}
          <div className="flex flex-wrap gap-3">
            <span className="sticker text-lg" data-tone="lime">
              pago único
            </span>
            <span className="sticker text-lg" data-tone="coral" style={{ ['--sticker-tilt' as string]: '6deg' }}>
              envío rastreado
            </span>
            <span className="sticker text-lg" data-tone="cobalt" style={{ ['--sticker-tilt' as string]: '-3deg' }}>
              muchas tiendas
            </span>
          </div>
        </div>
      </aside>
    </div>
  )
}
