import { ButtonLink } from '@/components/ui/Button'
import { roleHome, useSession } from '@/features/auth/session'

export function NotFoundPage() {
  const { session } = useSession()
  return (
    <div className="mx-auto grid max-w-2xl justify-items-center px-4 py-24 text-center">
      <span className="sticker text-4xl" data-tone="lime">
        404
      </span>
      <h1 className="mt-8 text-4xl font-extrabold">esta página no existe</h1>
      <p className="mt-3 text-lg text-ink-soft">puede que el enlace esté mal escrito o que la página se haya movido.</p>
      <ButtonLink to={session ? roleHome[session.role] : '/'} className="mt-8" size="lg">
        Volver al inicio
      </ButtonLink>
    </div>
  )
}
