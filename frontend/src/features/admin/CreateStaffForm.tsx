import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { describeError } from '@/api/errors'
import type { StaffRole, UserResponse } from '@/api/types'
import { CopyId } from '@/components/CopyId'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Field, Input, Select } from '@/components/ui/Field'
import { FormAlert } from '@/components/ui/FormAlert'
import { PasswordInput } from '@/components/ui/PasswordInput'
import { MIN_PASSWORD_LENGTH } from '@/features/auth/passwordRules'
import { roleLabel, userStatusLabel } from '@/lib/labels'
import { useRegisterStaff } from './queries'

// Only these two roles can be incorporated as staff: the backend accepts nothing else (RD-ROL-09).
const STAFF_ROLES: StaffRole[] = ['LOGISTICS_OPERATOR', 'SUPERVISOR']

// The same format the backend's EmailAddress checks, so the server never rejects what passed here.
const EMAIL = /^[^@\s]+@[^@\s.]+(\.[^@\s.]+)+$/

// Required like on the server; the lengths are those of the database columns; the password follows
// the minimum the rest of the store asks for, and BCrypt's limit of 72 bytes.
const schema = z.object({
  fullName: z.string().trim().min(1, 'escribe el nombre completo').max(150, 'máximo 150 caracteres'),
  email: z.string().trim().min(1, 'escribe el correo').max(150, 'máximo 150 caracteres').regex(EMAIL, 'ese correo no parece válido, por ejemplo: nombre@nexusmarket.local'),
  identityDocument: z.string().trim().min(1, 'escribe el documento de identidad').max(60, 'máximo 60 caracteres'),
  password: z
    .string()
    .min(MIN_PASSWORD_LENGTH, `la contraseña es muy débil: usa al menos ${MIN_PASSWORD_LENGTH} caracteres`)
    .refine((v) => new TextEncoder().encode(v).length <= 72, 'la contraseña es demasiado larga: máximo 72 caracteres'),
  role: z.enum(['LOGISTICS_OPERATOR', 'SUPERVISOR'], { message: 'elige operador logístico o supervisor' }),
})
type Values = z.infer<typeof schema>

const empty: Values = { fullName: '', email: '', identityDocument: '', password: '', role: 'LOGISTICS_OPERATOR' }

/**
 * Incorporates a logistics operator or a supervisor. The identifier is generated here, as the API
 * asks; once created, the new account is shown with it so it can be blocked or reassigned right away.
 */
export function CreateStaffForm({ created, onCreated, onUseId }: { created: UserResponse | null; onCreated: (user: UserResponse) => void; onUseId: (id: string) => void }) {
  const register = useRegisterStaff()
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: empty })
  const errors = form.formState.errors

  return (
    <section aria-labelledby="crear-personal" className="grid content-start gap-5 rounded-[var(--radius-card)] bg-card p-5 ring-1 ring-line sm:p-6">
      <div>
        <h2 id="crear-personal" className="text-xl font-bold">
          crear personal
        </h2>
        <p className="mt-1 text-sm text-ink-soft">incorpora a un operador logístico o a un supervisor con su contraseña inicial.</p>
      </div>
      <form
        noValidate
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) =>
          register.mutate(
            { ...values, fullName: values.fullName.trim(), email: values.email.trim(), identityDocument: values.identityDocument.trim() },
            {
              onSuccess: (user) => {
                form.reset(empty)
                onCreated(user)
              },
            },
          ),
        )}
      >
        {register.error && <FormAlert>{describeError(register.error)}</FormAlert>}
        <Field label="Nombre completo" error={errors.fullName?.message}>
          <Input autoComplete="off" {...form.register('fullName')} />
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Correo" error={errors.email?.message}>
            <Input type="email" autoComplete="off" {...form.register('email')} />
          </Field>
          <Field label="Documento de identidad" error={errors.identityDocument?.message}>
            <Input autoComplete="off" {...form.register('identityDocument')} />
          </Field>
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Contraseña inicial" hint={`al menos ${MIN_PASSWORD_LENGTH} caracteres; compártela por un canal seguro.`} error={errors.password?.message}>
            <PasswordInput autoComplete="new-password" {...form.register('password')} />
          </Field>
          <Field label="Rol" error={errors.role?.message}>
            <Select {...form.register('role')}>
              {STAFF_ROLES.map((role) => (
                <option key={role} value={role}>
                  {roleLabel[role]}
                </option>
              ))}
            </Select>
          </Field>
        </div>
        <Button type="submit" loading={register.isPending} className="justify-self-start">
          Crear personal
        </Button>
      </form>

      {created && (
        <div className="grid gap-2 rounded-2xl bg-mint-wash/60 p-4 ring-1 ring-mint/25" aria-live="polite" data-testid="staff-created">
          <p className="text-sm font-semibold text-mint">cuenta creada</p>
          <p className="font-semibold">{created.fullName}</p>
          <p className="text-sm break-all text-ink-soft">
            {created.email} · documento {created.identityDocument}
          </p>
          <div className="flex flex-wrap items-center gap-2">
            <Badge tone="cobalt">{roleLabel[created.role]}</Badge>
            <Badge tone={created.status === 'ACTIVE' ? 'mint' : 'coral'}>{userStatusLabel[created.status]}</Badge>
            <CopyId id={created.id} label="identificador del nuevo usuario" />
          </div>
          <Button variant="outline" size="sm" className="justify-self-start" onClick={() => onUseId(created.id)}>
            Usar este identificador para bloquear o cambiar el rol
          </Button>
        </div>
      )}
    </section>
  )
}
