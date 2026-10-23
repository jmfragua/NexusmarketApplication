import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'
import { authApi } from '@/api/endpoints'
import { describeError } from '@/api/errors'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { FormAlert } from '@/components/ui/FormAlert'
import { PasswordInput } from '@/components/ui/PasswordInput'
import { roleLabel } from '@/lib/labels'
import { AuthShell } from './AuthShell'
import { roleHome, useSession } from './session'

const schema = z.object({
  email: z.string().trim().min(1, 'escribe tu correo').email('ese correo no parece válido'),
  password: z.string().min(1, 'escribe tu contraseña'),
})
type Values = z.infer<typeof schema>

/** Only a path inside the buyer's store may be resumed after login; anything else goes home. */
function safeReturnPath(from: unknown): string | null {
  return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : null
}

export function LoginPage() {
  const { signIn } = useSession()
  const navigate = useNavigate()
  const location = useLocation()
  const from = safeReturnPath((location.state as { from?: unknown } | null)?.from)
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { email: '', password: '' } })

  const login = useMutation({
    mutationFn: authApi.login,
    onSuccess: (auth) => {
      const session = signIn(auth)
      toast.success(`hola de nuevo · ${roleLabel[session.role].toLowerCase()}`)
      const home = roleHome[session.role]
      const resumable = from && (session.role === 'BUYER' ? !/^\/(vendedor|logistica|admin|supervisor)/.test(from) : from.startsWith(home))
      navigate(resumable ? from : home, { replace: true })
    },
  })

  return (
    <AuthShell
      title="ingresa a tu cuenta"
      description="compradores, vendedores y equipo de operación usan la misma puerta; cada quien llega a su espacio."
      aside={<p>tu pedido sigue donde lo dejaste.</p>}
    >
      <form noValidate onSubmit={form.handleSubmit((values) => login.mutate(values))} className="grid gap-5">
        <Field label="Correo" error={form.formState.errors.email?.message}>
          <Input type="email" autoComplete="email" {...form.register('email')} />
        </Field>
        <Field label="Contraseña" error={form.formState.errors.password?.message}>
          <PasswordInput autoComplete="current-password" {...form.register('password')} />
        </Field>
        {login.error && <FormAlert>{describeError(login.error, 'login')}</FormAlert>}
        <Button type="submit" size="lg" loading={login.isPending}>
          Ingresar
        </Button>
        <p className="text-ink-soft">
          ¿aún no tienes cuenta?{' '}
          <Link to="/registro" className="font-semibold text-cobalt underline-offset-4 hover:underline">
            Crea una como comprador
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}
