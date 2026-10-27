import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { buyerApi } from '@/api/endpoints'
import type { BuyerResponse } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { describeError } from '@/api/errors'
import { EmptyState } from '@/components/ui/States'
import { StorePage } from '@/components/ui/StorePage'
import { knownAddresses, readAddressBook, rememberBuyer, type AddressBook } from '@/features/auth/addressBook'
import { useSession } from '@/features/auth/session'

export function AccountPage() {
  const { session } = useSession()
  const userId = session?.userId ?? ''
  const [book, setBook] = useState<AddressBook | null>(() => readAddressBook(userId))
  const [draft, setDraft] = useState('')
  const [draftError, setDraftError] = useState<string | null>(null)

  const onBuyer = (buyer: BuyerResponse) => setBook(rememberBuyer(buyer))
  const add = useMutation({
    mutationFn: buyerApi.addAddress,
    onSuccess: (buyer) => {
      onBuyer(buyer)
      setDraft('')
      toast.success('guardamos la dirección')
    },
    onError: (error) => toast.error(describeError(error)),
  })
  const promote = useMutation({
    mutationFn: buyerApi.setPrimaryAddress,
    onSuccess: (buyer) => {
      onBuyer(buyer)
      toast.success('cambiamos tu dirección principal')
    },
    onError: (error) => toast.error(describeError(error)),
  })

  const submit = (e: FormEvent) => {
    e.preventDefault()
    if (draft.trim().length < 8) {
      setDraftError('escribe una dirección completa, con ciudad')
      return
    }
    setDraftError(null)
    add.mutate(draft.trim())
  }

  const addresses = knownAddresses(book)

  return (
    <StorePage className="max-w-3xl">
      <PageHeader title="direcciones de entrega" description={session?.email} />
      <div className="grid gap-6">
        {addresses.length === 0 ? (
          <EmptyState
            title="no tenemos tus direcciones en este navegador"
            description="agrega una dirección y la verás aquí. La que registraste al crear la cuenta sigue guardada en el servidor."
          />
        ) : (
          <ul className="grid gap-2.5">
            {addresses.map((address, i) => (
              <li key={address} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-card p-4 ring-1 ring-line">
                <span className="font-semibold">{address}</span>
                {i === 0 ? (
                  <Badge tone="cobalt">Principal</Badge>
                ) : (
                  <Button variant="outline" size="sm" loading={promote.isPending && promote.variables === address} onClick={() => promote.mutate(address)}>
                    Usar como principal
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}

        <form onSubmit={submit} className="grid gap-4 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line" noValidate>
          <h2 className="text-xl font-bold">agregar una dirección</h2>
          <Field label="Dirección" error={draftError ?? undefined}>
            <Input value={draft} onChange={(e) => setDraft(e.target.value)} autoComplete="street-address" placeholder="Avenida 6N # 23-45, Cali" />
          </Field>
          <Button type="submit" loading={add.isPending} className="justify-self-start">
            Guardar dirección
          </Button>
        </form>

        <details className="group rounded-2xl bg-cobalt-wash/60 px-4 py-3 text-ink ring-1 ring-cobalt/20">
          <summary className="flex cursor-pointer list-none items-center gap-2.5 rounded-lg font-semibold [&::-webkit-details-marker]:hidden">
            <svg viewBox="0 0 20 20" className="size-5 shrink-0 text-cobalt" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
              <circle cx="10" cy="10" r="7.5" />
              <path d="M10 9v4.5M10 6.4v.1" strokeWidth="2.2" />
            </svg>
            Tus direcciones se guardan en este navegador
            <svg viewBox="0 0 20 20" className="ml-auto size-4 shrink-0 text-ink-soft transition-transform group-open:rotate-180" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="m5 7.5 5 5 5-5" />
            </svg>
          </summary>
          <p className="mt-2 pl-7.5 text-ink-soft">
            aquí ves las direcciones que agregaste o elegiste como principal desde este navegador. Si entras desde otro equipo puede que no aparezcan, pero siguen guardadas en tu cuenta, igual que la que usaste al registrarte.
          </p>
        </details>
      </div>
    </StorePage>
  )
}
