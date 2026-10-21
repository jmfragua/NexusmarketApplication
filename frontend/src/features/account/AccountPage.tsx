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
import { EmptyState, Notice } from '@/components/ui/States'
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
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
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

        <Notice tone="cobalt" title="¿por qué solo veo algunas direcciones?">
          el servidor no ofrece una consulta del perfil; mostramos las direcciones que confirmó la última vez que las modificaste desde este navegador.
        </Notice>
      </div>
    </div>
  )
}
