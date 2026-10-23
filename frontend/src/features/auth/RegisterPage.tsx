import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm, useWatch } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { z } from 'zod'
import { authApi } from '@/api/endpoints'
import { describeError } from '@/api/errors'
import { Button } from '@/components/ui/Button'
import { Field, Input } from '@/components/ui/Field'
import { FormAlert } from '@/components/ui/FormAlert'
import { PasswordInput } from '@/components/ui/PasswordInput'
import { newId } from '@/lib/id'
import { rememberPrimaryAddress } from './addressBook'
import { AuthShell } from './AuthShell'
import { PasswordMatch, PasswordRules } from './PasswordFeedback'
import { MIN_PASSWORD_LENGTH } from './passwordRules'
import { useSession } from './session'

const schema = z
  .object({
    fullName: z.string().trim().min(3, 'escribe tu nombre completo'),
    email: z.string().trim().min(1, 'escribe tu correo').email('ese correo no parece válido'),
    identityDocument: z.string().trim().min(5, 'el documento debe tener al menos 5 caracteres'),
    primaryAddress: z.string().trim().min(8, 'escribe una dirección completa, con ciudad'),
    password: z.string().min(MIN_PASSWORD_LENGTH, `usa al menos ${MIN_PASSWORD_LENGTH} caracteres`),
    confirm: z.string(),
  })
  .refine((v) => v.password === v.confirm, { path: ['confirm'], message: 'las contraseñas no coinciden' })
type Values = z.infer<typeof schema>

export function RegisterPage() {
  const { signIn } = useSession()
  const navigate = useNavigate()
  const form = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { fullName: '', email: '', identityDocument: '', primaryAddress: '', password: '', confirm: '' },
  })
  const errors = form.formState.errors
  const password = useWatch({ control: form.control, name: 'password' })
  const confirm = useWatch({ control: form.control, name: 'confirm' })

  const register = useMutation({
    mutationFn: (values: Values) =>
      authApi.register({
        buyerId: newId(),
        fullName: values.fullName,
        email: values.email,
        identityDocument: values.identityDocument,
        password: values.password,
        primaryAddress: values.primaryAddress,
      }),
    onSuccess: (auth, values) => {
      signIn(auth)
      rememberPrimaryAddress(auth.userId, values.primaryAddress.trim())
      toast.success('tu cuenta está lista, ya puedes comprar')
      navigate('/', { replace: true })
    },
  })

  return (
    <AuthShell
      title="crea tu cuenta"
      description="las cuentas que se crean aquí son de comprador. Los vendedores los incorpora el equipo de NexusMarket."
      aside={<p>una cuenta, todas las tiendas.</p>}
    >
      <form noValidate onSubmit={form.handleSubmit((values) => register.mutate(values))} className="grid gap-5">
        <Field label="Nombre completo" error={errors.fullName?.message}>
          <Input autoComplete="name" {...form.register('fullName')} />
        </Field>
        <div className="grid gap-5 sm:grid-cols-2">
          <Field label="Correo" error={errors.email?.message}>
            <Input type="email" autoComplete="email" {...form.register('email')} />
          </Field>
          <Field label="Documento de identidad" error={errors.identityDocument?.message}>
            <Input inputMode="numeric" autoComplete="off" {...form.register('identityDocument')} />
          </Field>
        </div>
        <Field
          label="Dirección principal"
          hint="Con ciudad, por ejemplo: Calle 10 # 5-20, Bogotá. La usaremos como dirección de entrega por defecto."
          error={errors.primaryAddress?.message}
        >
          <Input autoComplete="street-address" {...form.register('primaryAddress')} />
        </Field>
        <div className="grid gap-5">
          <Field label="Contraseña" error={errors.password?.message} extra={<PasswordRules password={password} />}>
            <PasswordInput autoComplete="new-password" {...form.register('password')} />
          </Field>
          <Field
            label="Repite la contraseña"
            error={errors.confirm?.message}
            extra={errors.confirm ? undefined : <PasswordMatch password={password} confirm={confirm} />}
          >
            <PasswordInput autoComplete="new-password" {...form.register('confirm')} />
          </Field>
        </div>
        {register.error && <FormAlert>{describeError(register.error)}</FormAlert>}
        <Button type="submit" size="lg" loading={register.isPending}>
          Crear mi cuenta
        </Button>
        <p className="text-ink-soft">
          ¿ya tienes cuenta?{' '}
          <Link to="/ingresar" className="font-semibold text-cobalt underline-offset-4 hover:underline">
            Ingresa aquí
          </Link>
        </p>
      </form>
    </AuthShell>
  )
}
