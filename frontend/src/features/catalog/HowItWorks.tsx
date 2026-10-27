import type { CSSProperties } from 'react'

const STEPS = [
  {
    title: 'elige de varias tiendas',
    text: 'explora productos físicos y digitales de distintos vendedores y júntalos en un mismo carrito.',
    tone: 'bg-lime text-ink',
  },
  {
    title: 'paga una sola vez',
    text: 'confirmas el carrito y pagas todo junto, sin crear una cuenta en cada tienda.',
    tone: 'bg-coral text-ink',
  },
  {
    title: 'sigue cada pedido',
    text: 'ves el estado de tu pedido desde el pago hasta que llega a tu puerta, y tu factura queda guardada.',
    tone: 'bg-cobalt text-white',
  },
]

/** Three steps between the hero and the catalogue: what buying here looks like. */
export function HowItWorks() {
  return (
    <section aria-labelledby="como-funciona-title" className="mx-auto max-w-7xl px-4 pt-10 pb-2 sm:px-6">
      <h2 id="como-funciona-title" className="text-2xl font-extrabold sm:text-3xl">
        cómo funciona
      </h2>
      <ol className="mt-5 grid gap-4 md:grid-cols-3">
        {STEPS.map((step, i) => (
          <li key={step.title} className="flex gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line">
            <span
              className={`sticker grid size-11 shrink-0 place-items-center self-start p-0 text-lg ${step.tone}`}
              style={{ '--sticker-tilt': `${i % 2 === 0 ? -6 : 5}deg` } as CSSProperties}
              aria-hidden="true"
            >
              {i + 1}
            </span>
            <div>
              <h3 className="font-display text-lg leading-tight font-bold">{step.title}</h3>
              <p className="mt-1.5 text-ink-soft">{step.text}</p>
            </div>
          </li>
        ))}
      </ol>
    </section>
  )
}
