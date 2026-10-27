import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ButtonLink } from '@/components/ui/Button'
import { ReceiptArt } from '@/components/ui/EmptyArt'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { StorePage } from '@/components/ui/StorePage'
import { shortId } from '@/lib/id'
import { formatMoney } from '@/lib/money'
import { useMyInvoices } from './queries'

export function InvoicesPage() {
  const [page, setPage] = useState(0)
  const { data, isPending, error, refetch } = useMyInvoices(page)
  return (
    <StorePage>
      <PageHeader title="facturas" description="cada factura corresponde a un pedido pagado." />
      {isPending ? (
        <LoadingRows label="cargando tus facturas" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState
          art={<ReceiptArt />}
          title="todavía no tienes facturas"
          description="la factura se emite después de confirmar el pago de un pedido."
          action={<ButtonLink to="/pedidos" variant="outline">Ver mis pedidos</ButtonLink>}
        />
      ) : (
        <>
          <div className="relative overflow-x-auto rounded-[var(--radius-card)] bg-card ring-1 ring-line">
            <table className="w-full min-w-[32rem] text-left">
              <caption className="sr-only">Facturas emitidas a tu nombre</caption>
              <thead className="border-b border-line text-sm text-ink-soft">
                <tr>
                  <th scope="col" className="px-5 py-3 font-semibold">Factura</th>
                  <th scope="col" className="px-5 py-3 font-semibold">Pedido</th>
                  <th scope="col" className="px-5 py-3 text-right font-semibold">Total</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {data.content.map((invoice) => (
                  <tr key={invoice.id}>
                    <td className="px-5 py-4 font-mono font-semibold">#{shortId(invoice.id)}</td>
                    <td className="px-5 py-4">
                      <Link to={`/pedidos/${encodeURIComponent(invoice.orderId)}`} className="font-semibold text-cobalt underline-offset-4 hover:underline">
                        Pedido #{shortId(invoice.orderId)}
                      </Link>
                    </td>
                    <td className="px-5 py-4 text-right font-semibold tabular-nums">{formatMoney(invoice.totalAmount, invoice.currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data} onChange={setPage} />
        </>
      )}
    </StorePage>
  )
}
