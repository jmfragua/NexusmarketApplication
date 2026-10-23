import { useEffect, type ReactNode } from 'react'
import { useLocation } from 'react-router-dom'
import { ButtonLink } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'

function Section({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className="scroll-mt-24 rounded-[var(--radius-card)] bg-card p-6 ring-1 ring-line sm:p-7">
      <h2 id={`${id}-title`} className="text-2xl font-bold">
        {title}
      </h2>
      <div className="mt-3 grid gap-3 text-ink-soft">{children}</div>
    </section>
  )
}

/**
 * Plain statements about this project, nothing more: what it is, how the payment works and which
 * data it keeps. Every line describes what the application does today; it is not a legal text.
 */
export function AboutPage() {
  const { hash } = useLocation()
  useEffect(() => {
    if (hash.length > 1) document.getElementById(decodeURIComponent(hash.slice(1)))?.scrollIntoView()
  }, [hash])

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      <PageHeader
        title="acerca del proyecto"
        description="NexusMarket es un trabajo de la asignatura Construcción de Software 2: un marketplace con varios vendedores. No es una tienda real."
      />
      <div className="grid gap-5">
        <Section id="pagos" title="pagos de prueba">
          <p>
            El pago es simulado. Al confirmar y pagar, el pedido se registra y su pago queda validado de inmediato: no se pide ninguna
            tarjeta y no se cobra dinero real.
          </p>
        </Section>

        <Section id="datos" title="datos que guardamos">
          <p>Para que el marketplace funcione se guardan estos datos, y no se usan para nada más:</p>
          <ul className="grid list-disc gap-1 pl-5">
            <li>tu nombre, tu correo y tu documento de identidad;</li>
            <li>tus direcciones de entrega;</li>
            <li>tus pedidos, facturas y devoluciones.</li>
          </ul>
          <p>
            Tu contraseña se guarda cifrada, de modo que nadie puede leerla. Al ingresar, tu sesión queda en este navegador y se borra
            cuando sales.
          </p>
          <p>
            La aplicación no instala cookies ni usa herramientas de analítica. Lo único que se pide a un servicio externo son las
            tipografías, que se cargan desde Google Fonts.
          </p>
        </Section>

        <Section id="contacto" title="contacto">
          <p>Este proyecto no tiene un canal de atención al público: es un trabajo de curso y no hay una empresa detrás.</p>
        </Section>

        <p className="text-sm text-ink-soft">Esta página describe cómo funciona la aplicación; no es un documento legal.</p>
        <div>
          <ButtonLink to="/" variant="outline">
            Volver al catálogo
          </ButtonLink>
        </div>
      </div>
    </div>
  )
}
