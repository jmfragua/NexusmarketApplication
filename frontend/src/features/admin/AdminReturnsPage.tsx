import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { describeError } from '@/api/errors'
import { adminApi } from '@/api/endpoints'
import type { ProductReturnResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows, Notice } from '@/components/ui/States'
import { shortId } from '@/lib/id'
import { DEFAULT_CURRENCY, formatMoney, parsePrice } from '@/lib/money'
import { readIssuedInvoices, useIssueRefund, useRefunds } from './queries'

function RefundForm({ productReturn }: { productReturn: ProductReturnResponse }) {
  const known = readIssuedInvoices().find((i) => i.orderId === productReturn.orderId)
  const [invoiceId, setInvoiceId] = useState(known?.id ?? '')
  const [amount, setAmount] = useState('')
  const [currency, setCurrency] = useState(known?.currency ?? DEFAULT_CURRENCY)
  const [errors, setErrors] = useState<{ invoice?: string; amount?: string }>({})
  const refund = useIssueRefund()

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const value = parsePrice(amount)
    const next = {
      invoice: invoiceId.trim() === '' ? 'pega el número de la factura del pedido' : undefined,
      amount: value === null ? 'escribe un monto válido, por ejemplo 68500' : undefined,
    }
    setErrors(next)
    if (next.invoice || next.amount || value === null) return
    refund.mutate({ productReturnId: productReturn.id, invoiceId: invoiceId.trim(), amount: value, currency: currency.trim().toUpperCase() })
  }

  return (
    <form onSubmit={submit} noValidate className="grid gap-4">
      <Field label="Factura del pedido" hint={known ? 'la tomamos de las facturas que emitiste aquí.' : 'la ves en facturación o en los reportes del supervisor.'} error={errors.invoice}>
        <Input value={invoiceId} onChange={(e) => setInvoiceId(e.target.value)} autoComplete="off" />
      </Field>
      <div className="grid gap-4 sm:grid-cols-[1fr_7rem]">
        <Field label="Monto a reembolsar" hint={known ? `total facturado: ${formatMoney(known.totalAmount, known.currency)}` : undefined} error={errors.amount}>
          <Input inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} />
        </Field>
        <Field label="Moneda">
          <Input value={currency} maxLength={3} className="uppercase" onChange={(e) => setCurrency(e.target.value)} />
        </Field>
      </div>
      <Button type="submit" loading={refund.isPending} className="justify-self-start">
        Emitir reembolso
      </Button>
      {refund.isSuccess && <p className="font-semibold text-mint" aria-live="polite">reembolso #{shortId(refund.data.id)} emitido por {formatMoney(refund.data.amount, refund.data.currency)}.</p>}
    </form>
  )
}

export function AdminReturnsPage() {
  const [returnId, setReturnId] = useState('')
  const [idError, setIdError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const refunds = useRefunds(page)
  const lookup = useMutation({ mutationFn: adminApi.productReturn })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (returnId.trim() === '') {
      setIdError('pega el identificador de la devolución')
      return
    }
    setIdError(null)
    lookup.mutate(returnId.trim())
  }

  return (
    <>
      <PageHeader eyebrow="Administración" title="devoluciones y reembolsos" description="consulta una devolución y, si procede, reembolsa sin superar el total facturado." />
      <div className="grid gap-6 lg:grid-cols-2">
        <section className="grid content-start gap-5 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:p-6">
          <form onSubmit={submit} noValidate className="grid gap-3 sm:grid-cols-[1fr_auto] sm:items-end">
            <Field label="Devolución" error={idError ?? undefined}>
              <Input value={returnId} onChange={(e) => setReturnId(e.target.value)} autoComplete="off" />
            </Field>
            <Button type="submit" variant="outline" loading={lookup.isPending}>
              Consultar
            </Button>
          </form>
          {lookup.error && <p role="alert" className="rounded-xl bg-coral-wash px-4 py-3 text-sm font-medium text-coral-deep">{describeError(lookup.error)}</p>}
          {lookup.data && (
            <div className="grid gap-4 border-t border-line pt-5">
              <dl className="grid grid-cols-[auto_1fr] gap-x-5 gap-y-1.5 text-sm">
                <dt className="text-ink-soft">Pedido</dt>
                <dd>
                  <CopyId id={lookup.data.orderId} label="número de pedido" />
                </dd>
                <dt className="text-ink-soft">Línea</dt>
                <dd className="font-mono">#{shortId(lookup.data.orderLineId)}</dd>
                <dt className="text-ink-soft">Unidades</dt>
                <dd className="font-semibold">{lookup.data.quantity}</dd>
              </dl>
              <RefundForm key={lookup.data.id} productReturn={lookup.data} />
            </div>
          )}
          {!lookup.data && !lookup.error && (
            <Notice tone="cobalt" title="¿de dónde sale el identificador?">el comprador lo genera al solicitar la devolución y lo ve, para copiarlo, en el detalle de su pedido.</Notice>
          )}
        </section>

        <section aria-labelledby="reembolsos">
          <h2 id="reembolsos" className="mb-3 text-2xl font-bold">
            reembolsos emitidos
          </h2>
          {refunds.isPending ? (
            <LoadingRows rows={3} label="cargando reembolsos" />
          ) : refunds.error ? (
            <ErrorState error={refunds.error} onRetry={() => void refunds.refetch()} />
          ) : refunds.data.content.length === 0 ? (
            <EmptyState title="no hay reembolsos" description="cuando emitas uno aparecerá aquí." />
          ) : (
            <>
              <ul className="grid gap-2.5">
                {refunds.data.content.map((r) => (
                  <li key={r.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-card p-4 ring-1 ring-line">
                    <span className="grid gap-0.5 text-sm">
                      <span className="font-semibold">reembolso #{shortId(r.id)}</span>
                      <span className="text-ink-soft">
                        devolución #{shortId(r.productReturnId)} · factura #{shortId(r.invoiceId)}
                      </span>
                    </span>
                    <span className="font-display text-lg font-bold tabular-nums">{formatMoney(r.amount, r.currency)}</span>
                  </li>
                ))}
              </ul>
              <Pagination page={refunds.data} onChange={setPage} />
            </>
          )}
        </section>
      </div>
    </>
  )
}
