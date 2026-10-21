import { useState } from 'react'
import { CopyId } from '@/components/CopyId'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { useIssuedInvoices, useSupervisorRefunds } from './queries'

export function SupervisorBillingPage() {
  const [invoicePage, setInvoicePage] = useState(0)
  const [refundPage, setRefundPage] = useState(0)
  const invoices = useIssuedInvoices(invoicePage)
  const refunds = useSupervisorRefunds(refundPage)

  return (
    <>
      <PageHeader eyebrow="Supervisión · solo lectura" title="facturación" description="facturas emitidas y reembolsos pagados." />
      <div className="grid grid-cols-1 gap-8 xl:grid-cols-[1.4fr_1fr]">
        <section aria-labelledby="facturas">
          <h2 id="facturas" className="mb-3 text-2xl font-bold">
            facturas
          </h2>
          {invoices.isPending ? (
            <LoadingRows label="cargando facturas" />
          ) : invoices.error ? (
            <ErrorState error={invoices.error} onRetry={() => void invoices.refetch()} />
          ) : invoices.data.content.length === 0 ? (
            <EmptyState title="aún no hay facturas" description="se emiten sobre pedidos con el pago validado." />
          ) : (
            <>
              <div className="relative overflow-x-auto rounded-[var(--radius-card)] bg-card ring-1 ring-line">
                <table className="w-full min-w-[34rem] text-left text-[0.95rem]">
                  <caption className="sr-only">Facturas emitidas</caption>
                  <thead className="border-b border-line text-sm text-ink-soft">
                    <tr>
                      <th scope="col" className="px-4 py-3 font-semibold">Factura</th>
                      <th scope="col" className="px-4 py-3 font-semibold">Pedido</th>
                      <th scope="col" className="px-4 py-3 font-semibold">Comprador</th>
                      <th scope="col" className="px-4 py-3 text-right font-semibold">Total</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {invoices.data.content.map((i) => (
                      <tr key={i.id}>
                        <td className="px-4 py-3">
                          <CopyId id={i.id} label="número de factura" />
                        </td>
                        <td className="px-4 py-3 font-mono text-sm">#{shortId(i.orderId)}</td>
                        <td className="px-4 py-3 font-mono text-sm">#{shortId(i.buyerId)}</td>
                        <td className="px-4 py-3 text-right font-semibold tabular-nums">{formatMoney(i.totalAmount, i.currency)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={invoices.data} onChange={setInvoicePage} />
            </>
          )}
        </section>

        <section aria-labelledby="reembolsos">
          <h2 id="reembolsos" className="mb-3 text-2xl font-bold">
            reembolsos
          </h2>
          {refunds.isPending ? (
            <LoadingRows rows={3} label="cargando reembolsos" />
          ) : refunds.error ? (
            <ErrorState error={refunds.error} onRetry={() => void refunds.refetch()} />
          ) : refunds.data.content.length === 0 ? (
            <EmptyState title="no hay reembolsos" />
          ) : (
            <>
              <ul className="grid gap-2.5">
                {refunds.data.content.map((r) => (
                  <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-card p-4 ring-1 ring-line">
                    <span className="grid gap-0.5 text-sm">
                      <span className="font-semibold">reembolso #{shortId(r.id)}</span>
                      <span className="text-ink-soft">factura #{shortId(r.invoiceId)}</span>
                    </span>
                    <span className="font-display text-lg font-bold tabular-nums">{formatMoney(r.amount, r.currency)}</span>
                  </li>
                ))}
              </ul>
              <Pagination page={refunds.data} onChange={setRefundPage} />
            </>
          )}
        </section>
      </div>
    </>
  )
}
