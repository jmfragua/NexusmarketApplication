import { useState, type FormEvent } from 'react'
import { CopyId } from '@/components/CopyId'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, Notice } from '@/components/ui/States'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { readIssuedInvoices, useIssueInvoice } from './queries'

export function AdminBillingPage() {
  const issue = useIssueInvoice()
  const [orderId, setOrderId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [issued, setIssued] = useState(readIssuedInvoices)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (orderId.trim() === '') {
      setError('pega el número completo del pedido')
      return
    }
    setError(null)
    issue.mutate(orderId.trim(), {
      onSuccess: () => {
        setIssued(readIssuedInvoices())
        setOrderId('')
      },
    })
  }

  return (
    <>
      <PageHeader eyebrow="Administración" title="facturación" description="emite la factura de un pedido pagado. El total sale del pedido; no se escribe a mano." />
      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <div className="grid content-start gap-6">
          <form onSubmit={submit} noValidate className="grid gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:grid-cols-[1fr_auto] sm:items-end">
            <Field label="Número de pedido" hint="el comprador lo copia desde el detalle de su pedido." error={error ?? undefined}>
              <Input value={orderId} onChange={(e) => setOrderId(e.target.value)} autoComplete="off" />
            </Field>
            <Button type="submit" loading={issue.isPending}>
              Emitir factura
            </Button>
          </form>

          <section aria-labelledby="emitidas">
            <h2 id="emitidas" className="mb-3 text-2xl font-bold">
              emitidas desde este navegador
            </h2>
            {issued.length === 0 ? (
              <EmptyState title="todavía no emites facturas aquí" description="las que emitas aparecerán en esta lista con su total." />
            ) : (
              <ul className="grid gap-2.5">
                {issued.map((invoice) => (
                  <li key={invoice.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-card p-4 ring-1 ring-line">
                    <div className="grid gap-1">
                      <span className="font-semibold">pedido #{shortId(invoice.orderId)}</span>
                      <span className="flex items-center gap-2 text-sm text-ink-soft">
                        factura <CopyId id={invoice.id} label="número de factura" />
                      </span>
                    </div>
                    <span className="font-display text-xl font-bold tabular-nums">{formatMoney(invoice.totalAmount, invoice.currency)}</span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
        <aside className="grid content-start gap-4">
          <Notice tone="cobalt" title="cuándo se puede facturar">solo pedidos con el pago validado: pagados, despachados o entregados.</Notice>
          <Notice title="sin listado en el servidor">el administrador no tiene una consulta de facturas; el supervisor sí las ve todas en sus reportes.</Notice>
        </aside>
      </div>
    </>
  )
}
