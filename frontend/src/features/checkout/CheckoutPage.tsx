import { useQueryClient } from '@tanstack/react-query'
import { useMemo, useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { describeError } from '@/api/errors'
import type { CartResponse } from '@/api/types'
import { Button, ButtonLink } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { EmptyState, ErrorState, LoadingRows, Notice } from '@/components/ui/States'
import { knownAddresses, readAddressBook, rememberBuyer } from '@/features/auth/addressBook'
import { addressProblem, ADDRESS_HINT } from '@/features/auth/addressRules'
import { useSession } from '@/features/auth/session'
import { cartKey, useCart, usePricedCart } from '@/features/cart/queries'
import { ordersKey } from '@/features/orders/queries'
import { cn } from '@/lib/cn'
import { formatMoney } from '@/lib/money'
import { CheckoutError, placeAndPay, type CheckoutStep } from './placeOrder'

const NEW = '__nueva__'

const stepLabel: Record<CheckoutStep, string> = {
  address: 'registrando la dirección de entrega',
  order: 'creando tu pedido',
  payment: 'confirmando el pago',
}

export function CheckoutPage() {
  const { session } = useSession()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const cart = useCart()
  const { totals, loading } = usePricedCart(cart.data)

  const addresses = useMemo(() => knownAddresses(session ? readAddressBook(session.userId) : null), [session])
  const [choice, setChoice] = useState<string>(addresses[0] ?? NEW)
  const [typed, setTyped] = useState('')
  const [typedError, setTypedError] = useState<string | null>(null)
  const [step, setStep] = useState<CheckoutStep | null>(null)
  const [failure, setFailure] = useState<string | null>(null)

  const address = (choice === NEW ? typed : choice).trim()

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setFailure(null)
    // Only a newly typed address is checked; one already saved is used as it is.
    const problem = choice === NEW ? addressProblem(typed) : null
    if (problem) {
      setTypedError(problem)
      return
    }
    setTypedError(null)
    try {
      const { order, buyer } = await placeAndPay({ address, totals, onStep: setStep })
      rememberBuyer(buyer)
      queryClient.setQueryData<CartResponse>(cartKey, (prev) => (prev ? { ...prev, items: [] } : prev))
      void queryClient.invalidateQueries({ queryKey: ordersKey })
      toast.success('¡listo! recibimos tu pago')
      navigate(`/pedidos/${encodeURIComponent(order.id)}`, { replace: true, state: { fresh: true } })
    } catch (error) {
      if (error instanceof CheckoutError && error.order) {
        void queryClient.invalidateQueries({ queryKey: cartKey })
        void queryClient.invalidateQueries({ queryKey: ordersKey })
        toast.error('creamos tu pedido, pero el pago no se confirmó; puedes pagarlo desde su página')
        navigate(`/pedidos/${encodeURIComponent(error.order.id)}`, { replace: true })
        return
      }
      setFailure(describeError(error instanceof CheckoutError ? error.cause : error))
    } finally {
      setStep(null)
    }
  }

  if (cart.isPending || loading) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <LoadingRows rows={3} label="preparando el pago" />
      </div>
    )
  }
  if (cart.error) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <ErrorState error={cart.error} onRetry={() => void cart.refetch()} />
      </div>
    )
  }
  if (cart.data.items.length === 0) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
        <EmptyState title="no hay nada que pagar" description="tu carrito está vacío." action={<ButtonLink to="/">Ir al catálogo</ButtonLink>} />
      </div>
    )
  }

  const busy = step !== null

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6">
      <PageHeader title="confirmar y pagar" description="elige dónde quieres recibir el pedido. Al pagar, el pedido pasa a preparación." />
      <form onSubmit={(e) => void submit(e)} className="grid gap-8 lg:grid-cols-[1fr_22rem]" noValidate>
        <section className="grid content-start gap-6">
          <fieldset className="rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line" disabled={busy}>
            <legend className="sr-only">Dirección de entrega</legend>
            <h2 className="text-xl font-bold">dirección de entrega</h2>
            <div className="mt-4 grid gap-2.5">
              {addresses.map((a, i) => (
                <label key={a} className={cn('flex cursor-pointer items-start gap-3 rounded-2xl p-3.5 ring-1 ring-inset transition-colors', choice === a ? 'bg-cobalt-wash ring-cobalt' : 'ring-line hover:ring-ink/40')}>
                  <input type="radio" name="direccion" value={a} checked={choice === a} onChange={() => setChoice(a)} className="mt-1 size-4 accent-cobalt" />
                  <span>
                    <span className="block font-semibold">{a}</span>
                    {i === 0 && <span className="text-sm text-ink-soft">dirección principal</span>}
                  </span>
                </label>
              ))}
              <label className={cn('flex cursor-pointer items-start gap-3 rounded-2xl p-3.5 ring-1 ring-inset transition-colors', choice === NEW ? 'bg-cobalt-wash ring-cobalt' : 'ring-line hover:ring-ink/40')}>
                <input type="radio" name="direccion" value={NEW} checked={choice === NEW} onChange={() => setChoice(NEW)} className="mt-1 size-4 accent-cobalt" />
                <span className="font-semibold">{addresses.length ? 'otra dirección' : 'escribir dirección'}</span>
              </label>
            </div>
            {choice === NEW && (
              <Field
                className="mt-4"
                label="Nueva dirección"
                hint={`${ADDRESS_HINT} La guardaremos en tu cuenta como dirección adicional.`}
                error={typedError ?? undefined}
              >
                <Input value={typed} onChange={(e) => setTyped(e.target.value)} autoComplete="street-address" />
              </Field>
            )}
          </fieldset>

          <Notice tone="cobalt" title="pago simulado">
            este proyecto no cobra dinero real: al confirmar, el pedido se registra y su pago queda validado de inmediato.
          </Notice>
        </section>

        <aside className="h-fit rounded-[var(--radius-card)] bg-card p-6 ring-1 ring-line lg:sticky lg:top-24" aria-label="Resumen del pedido">
          <h2 className="text-xl font-bold">tu pedido</h2>
          <ul className="mt-4 grid gap-3">
            {totals.lines.map((line) => (
              <li key={line.item.id} className="flex justify-between gap-3 text-sm">
                <span className="min-w-0">
                  <span className="block truncate font-semibold">{line.title}</span>
                  <span className="text-ink-soft">
                    {line.item.quantity} × {line.unitPrice !== null ? formatMoney(line.unitPrice, line.currency ?? undefined) : '—'}
                    {line.variantText && ` · ${line.variantText}`}
                  </span>
                </span>
                <span className="shrink-0 font-semibold tabular-nums">
                  {line.lineAmount !== null ? formatMoney(line.lineAmount, line.currency ?? undefined) : '—'}
                </span>
              </li>
            ))}
          </ul>
          <div className="mt-5 flex items-end justify-between border-t border-line pt-4">
            <span className="text-ink-soft">Total</span>
            <span className="font-display text-3xl font-extrabold tabular-nums">{formatMoney(totals.total, totals.currency ?? undefined)}</span>
          </div>
          {totals.blocker && (
            <div className="mt-4">
              <Notice tone="coral" title="revisa tu carrito">
                {totals.blocker}
              </Notice>
            </div>
          )}
          {failure && (
            <p role="alert" className="mt-4 rounded-xl bg-coral-wash px-4 py-3 text-sm font-medium text-coral-deep">
              {failure}
            </p>
          )}
          <Button type="submit" size="lg" className="mt-5 w-full" loading={busy} disabled={totals.blocker !== null}>
            {step ? stepLabel[step] : 'Confirmar y pagar'}
          </Button>
          <p className="sr-only" aria-live="polite">
            {step ? stepLabel[step] : ''}
          </p>
          <ButtonLink to="/carrito" variant="ghost" size="sm" className="mt-2 w-full">
            Volver al carrito
          </ButtonLink>
        </aside>
      </form>
    </div>
  )
}
