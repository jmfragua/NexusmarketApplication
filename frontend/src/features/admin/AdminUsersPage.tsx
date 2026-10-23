import { useState, type FormEvent } from 'react'
import type { Role, UserResponse } from '@/api/types'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select } from '@/components/ui/Field'
import { PageHeader } from '@/components/ui/PageHeader'
import { Notice } from '@/components/ui/States'
import { roleLabel, userStatusLabel } from '@/lib/labels'
import { useActivateUser, useBlockUser, useChangeRole } from './queries'

// Buyers and sellers keep their role (RD-ROL-08); only the staff roles can be swapped among themselves.
const staffRoles: Role[] = ['LOGISTICS_OPERATOR', 'ADMINISTRATOR', 'SUPERVISOR']

export function AdminUsersPage() {
  const [userId, setUserId] = useState('')
  const [role, setRole] = useState<Role>('LOGISTICS_OPERATOR')
  const [idError, setIdError] = useState<string | null>(null)
  const [result, setResult] = useState<UserResponse | null>(null)
  const block = useBlockUser()
  const activate = useActivateUser()
  const changeRole = useChangeRole()

  const id = userId.trim()
  const guard = (run: () => void) => (e?: FormEvent) => {
    e?.preventDefault()
    if (id === '') {
      setIdError('escribe el identificador del usuario')
      return
    }
    setIdError(null)
    run()
  }
  const onResult = { onSuccess: (u: UserResponse) => setResult(u) }

  return (
    <>
      <PageHeader eyebrow="Administración" title="usuarios" description="bloquea, reactiva o reasigna el rol de cualquier usuario a partir de su identificador." />
      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <section className="grid content-start gap-5 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:p-6">
          <Field label="Identificador del usuario" hint="el de cada vendedor aparece en la lista de vendedores." error={idError ?? undefined}>
            <Input value={userId} onChange={(e) => setUserId(e.target.value)} placeholder="Por ejemplo: admin-001" autoComplete="off" />
          </Field>
          <div className="flex flex-wrap gap-2">
            <Button variant="danger" loading={block.isPending} onClick={guard(() => block.mutate(id, onResult))}>
              Bloquear
            </Button>
            <Button variant="outline" loading={activate.isPending} onClick={guard(() => activate.mutate(id, onResult))}>
              Reactivar
            </Button>
          </div>
          <form onSubmit={guard(() => changeRole.mutate({ userId: id, role }, onResult))} className="grid gap-3 border-t border-line pt-5 sm:grid-cols-[1fr_auto] sm:items-end">
            <Field label="Nuevo rol">
              <Select value={role} onChange={(e) => setRole(e.target.value as Role)}>
                {staffRoles.map((r) => (
                  <option key={r} value={r}>
                    {roleLabel[r]}
                  </option>
                ))}
              </Select>
            </Field>
            <Button type="submit" loading={changeRole.isPending}>
              Cambiar rol
            </Button>
          </form>
          {result && (
            <div className="rounded-2xl bg-paper p-4" aria-live="polite">
              <p className="font-semibold">{result.fullName}</p>
              <p className="text-sm break-all text-ink-soft">{result.email}</p>
              <div className="mt-2 flex flex-wrap gap-2">
                <Badge tone="cobalt">{roleLabel[result.role]}</Badge>
                <Badge tone={result.status === 'ACTIVE' ? 'mint' : 'coral'}>{userStatusLabel[result.status]}</Badge>
              </div>
            </div>
          )}
        </section>
        <aside className="grid content-start gap-4">
          <Notice tone="cobalt" title="reglas de rol">
            cada usuario tiene un único rol. Compradores y vendedores conservan el suyo; operador logístico, administrador y supervisor
            pueden intercambiarse.
          </Notice>
          <Notice title="sin listado de usuarios">el backend no ofrece una consulta de todos los usuarios, por eso se trabaja con el identificador.</Notice>
        </aside>
      </div>
    </>
  )
}
