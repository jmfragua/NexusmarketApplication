import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { CopyId } from '@/components/CopyId'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { Pagination } from '@/components/ui/Pagination'
import { EmptyState, ErrorState, LoadingRows } from '@/components/ui/States'
import { userStatusLabel } from '@/lib/labels'
import { useActivateUser, useBlockUser, useRegisterSeller, useSellers } from './queries'

const schema = z.object({
  fullName: z.string().trim().min(3, 'escribe el nombre del vendedor o de la tienda'),
  email: z.string().trim().email('ese correo no parece válido'),
  identityDocument: z.string().trim().min(5, 'mínimo 5 caracteres'),
  password: z.string().min(8, 'usa al menos 8 caracteres'),
})
type Values = z.infer<typeof schema>

function RegisterSellerDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (v: boolean) => void }) {
  const register = useRegisterSeller()
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { fullName: '', email: '', identityDocument: '', password: '' } })
  const errors = form.formState.errors
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title="incorporar vendedor" description="los vendedores no se registran solos: se crean aquí junto con su primera bodega.">
      <form
        noValidate
        className="grid gap-4"
        onSubmit={form.handleSubmit((v) =>
          register.mutate(v, {
            onSuccess: () => {
              form.reset()
              onOpenChange(false)
            },
          }),
        )}
      >
        <Field label="Nombre o razón social" error={errors.fullName?.message}>
          <Input {...form.register('fullName')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Correo" error={errors.email?.message}>
            <Input type="email" autoComplete="off" {...form.register('email')} />
          </Field>
          <Field label="Documento o NIT" error={errors.identityDocument?.message}>
            <Input autoComplete="off" {...form.register('identityDocument')} />
          </Field>
        </div>
        <Field label="Contraseña inicial" hint="compártela con el vendedor por un canal seguro." error={errors.password?.message}>
          <Input type="password" autoComplete="new-password" {...form.register('password')} />
        </Field>
        <Button type="submit" loading={register.isPending} className="justify-self-start">
          Incorporar vendedor
        </Button>
      </form>
    </Dialog>
  )
}

export function AdminSellersPage() {
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState(false)
  const { data, isPending, error, refetch } = useSellers(page)
  const block = useBlockUser()
  const activate = useActivateUser()

  return (
    <>
      <PageHeader
        eyebrow="Administración"
        title="vendedores"
        description="incorpora vendedores y controla si pueden operar."
        actions={<Button onClick={() => setOpen(true)}>Incorporar vendedor</Button>}
      />
      {isPending ? (
        <LoadingRows label="cargando vendedores" />
      ) : error ? (
        <ErrorState error={error} onRetry={() => void refetch()} />
      ) : data.content.length === 0 ? (
        <EmptyState title="aún no hay vendedores" description="incorpora el primero para que pueda publicar productos." action={<Button onClick={() => setOpen(true)}>Incorporar vendedor</Button>} />
      ) : (
        <>
          <ul className="grid gap-3">
            {data.content.map((s) => (
              <li key={s.id} className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-card)] bg-card p-4 ring-1 ring-line">
                <div className="min-w-0">
                  <p className="text-lg font-bold">{s.fullName}</p>
                  <p className="text-sm break-all text-ink-soft">
                    {s.email} · documento {s.identityDocument}
                  </p>
                  <div className="mt-1.5 flex flex-wrap items-center gap-2">
                    <Badge tone={s.status === 'ACTIVE' ? 'mint' : 'coral'}>{userStatusLabel[s.status]}</Badge>
                    <CopyId id={s.id} label="identificador del vendedor" />
                  </div>
                </div>
                {s.status === 'ACTIVE' ? (
                  <Button variant="danger" size="sm" loading={block.isPending && block.variables === s.id} onClick={() => block.mutate(s.id)}>
                    Bloquear
                  </Button>
                ) : (
                  <Button variant="outline" size="sm" loading={activate.isPending && activate.variables === s.id} onClick={() => activate.mutate(s.id)}>
                    Reactivar
                  </Button>
                )}
              </li>
            ))}
          </ul>
          <Pagination page={data} onChange={setPage} />
        </>
      )}
      <RegisterSellerDialog open={open} onOpenChange={setOpen} />
    </>
  )
}
