import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { ButtonLink } from '@/components/ui/Button'
import { buttonClass } from '@/components/ui/buttonStyles'
import { Logo } from '@/components/Logo'
import { roleHome, useSession } from '@/features/auth/session'
import { useCart } from '@/features/cart/queries'
import { cn } from '@/lib/cn'
import { roleLabel } from '@/lib/labels'

// The page you are on is marked with a soft pill, never with a filled button: the only filled
// button of the header is the main call to action.
const navLink = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-full px-3.5 py-2 text-[0.95rem] font-semibold transition-colors',
    isActive ? 'bg-cobalt-wash text-cobalt-deep' : 'text-ink-soft hover:bg-ink/6 hover:text-ink',
  )

// "Ingresar" is a plain text link: underlined on hover, on focus and on its own page.
const textLink = ({ isActive }: { isActive: boolean }) =>
  cn(
    'rounded-full px-3 py-2 text-[0.95rem] font-semibold text-ink underline-offset-[6px] decoration-2 decoration-cobalt hover:underline focus-visible:underline',
    isActive && 'underline',
  )

function CartLink() {
  const { data } = useCart()
  const count = data?.items.reduce((sum, i) => sum + i.quantity, 0) ?? 0
  return (
    <NavLink
      to="/carrito"
      className={({ isActive }) =>
        cn(
          'relative inline-flex h-11 items-center gap-2 rounded-full px-4 font-semibold ring-1 ring-inset transition-colors',
          isActive ? 'bg-cobalt-wash text-cobalt-deep ring-cobalt' : 'bg-card ring-line-strong hover:ring-ink',
        )
      }
      aria-label={count > 0 ? `Carrito, ${count} ${count === 1 ? 'producto' : 'productos'}` : 'Carrito vacío'}
    >
      <svg viewBox="0 0 24 24" className="size-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" aria-hidden="true">
        <path d="M3 4h2.2l2.2 10.4a1.5 1.5 0 0 0 1.5 1.1h8.6a1.5 1.5 0 0 0 1.4-1.1L21 8H6.3" />
        <circle cx="9.5" cy="19.5" r="1.3" />
        <circle cx="17" cy="19.5" r="1.3" />
      </svg>
      <span className="hidden sm:inline">Carrito</span>
      {count > 0 && (
        <span className="grid min-w-6 place-items-center rounded-full bg-lime px-1.5 text-sm font-bold text-ink" aria-hidden="true">
          {count}
        </span>
      )}
    </NavLink>
  )
}

function AccountArea() {
  const { session, signOut } = useSession()
  const navigate = useNavigate()
  if (!session) {
    return (
      <div className="flex items-center gap-1 sm:gap-2">
        <NavLink to="/ingresar" className={textLink}>
          Ingresar
        </NavLink>
        <ButtonLink to="/registro" variant="primary" size="sm" className="h-10 max-sm:px-3.5">
          Crear cuenta
        </ButtonLink>
      </div>
    )
  }
  const leave = () => {
    signOut('manual')
    navigate('/')
  }
  if (session.role !== 'BUYER') {
    return (
      <div className="flex items-center gap-2">
        <ButtonLink to={roleHome[session.role]} variant="accent" size="sm" className="h-10">
          Panel de {roleLabel[session.role].toLowerCase()}
        </ButtonLink>
        <button type="button" onClick={leave} className={buttonClass('ghost', 'sm', 'h-10')}>
          Salir
        </button>
      </div>
    )
  }
  return (
    <div className="flex items-center gap-1.5">
      <CartLink />
      <button type="button" onClick={leave} className={buttonClass('ghost', 'sm', 'h-11')}>
        Salir
      </button>
    </div>
  )
}

const footerLink =
  'rounded-sm text-white/90 underline-offset-4 decoration-lime decoration-2 hover:text-white hover:underline focus-visible:underline'

function Footer() {
  const { session } = useSession()
  return (
    <footer className="mt-20 bg-ink text-white/85">
      <div className="mx-auto grid max-w-7xl gap-10 px-4 py-10 sm:px-6 md:grid-cols-[1.5fr_1fr_1fr]">
        <div>
          <Logo inverted />
          <p className="mt-3 max-w-sm text-sm">muchos vendedores, un solo carrito. Cada pedido se sigue desde el pago hasta la puerta de tu casa.</p>
        </div>
        <nav aria-label="Tienda">
          <h2 className="font-display text-base font-bold text-white">Tienda</h2>
          <ul className="mt-3 grid gap-2 text-sm">
            <li>
              <Link to="/" className={footerLink}>
                Catálogo
              </Link>
            </li>
            {session ? (
              <li>
                <Link to={session.role === 'BUYER' ? '/pedidos' : roleHome[session.role]} className={footerLink}>
                  {session.role === 'BUYER' ? 'Mis pedidos' : 'Mi panel'}
                </Link>
              </li>
            ) : (
              <>
                <li>
                  <Link to="/ingresar" className={footerLink}>
                    Ingresar
                  </Link>
                </li>
                <li>
                  <Link to="/registro" className={footerLink}>
                    Crear cuenta
                  </Link>
                </li>
              </>
            )}
          </ul>
        </nav>
        <nav aria-label="Proyecto">
          <h2 className="font-display text-base font-bold text-white">Proyecto</h2>
          <ul className="mt-3 grid gap-2 text-sm">
            <li>
              <Link to="/acerca" className={footerLink}>
                Acerca del proyecto
              </Link>
            </li>
            <li>
              <Link to="/acerca#datos" className={footerLink}>
                Datos que guardamos
              </Link>
            </li>
            <li>
              <Link to="/acerca#pagos" className={footerLink}>
                Pagos de prueba
              </Link>
            </li>
          </ul>
        </nav>
      </div>
      <div className="border-t border-white/10">
        <p className="mx-auto max-w-7xl px-4 py-4 text-sm text-white/75 sm:px-6">Proyecto académico · Construcción de Software 2</p>
      </div>
    </footer>
  )
}

export function StoreLayout() {
  const { session } = useSession()
  const isBuyer = session?.role === 'BUYER'
  return (
    <div className="flex min-h-dvh flex-col">
      <a href="#contenido" className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-full focus:bg-ink focus:px-4 focus:py-2 focus:text-white">
        Saltar al contenido
      </a>
      <header className="sticky top-0 z-30 border-b border-line/80 bg-paper/85 backdrop-blur-md">
        <div className="mx-auto flex h-18 max-w-7xl items-center justify-between gap-2 px-4 sm:px-6">
          <Logo compact />
          <nav aria-label="Principal" className="hidden items-center gap-1 lg:flex">
            <NavLink to="/" end className={navLink}>
              Catálogo
            </NavLink>
            {isBuyer && (
              <>
                <NavLink to="/pedidos" className={navLink}>
                  Mis pedidos
                </NavLink>
                <NavLink to="/facturas" className={navLink}>
                  Facturas
                </NavLink>
                <NavLink to="/cuenta" className={navLink}>
                  Direcciones
                </NavLink>
              </>
            )}
          </nav>
          <AccountArea />
        </div>
        {isBuyer && (
          <nav aria-label="Principal móvil" className="flex gap-1 overflow-x-auto px-3 pb-2 lg:hidden">
            <NavLink to="/" end className={navLink}>
              Catálogo
            </NavLink>
            <NavLink to="/pedidos" className={navLink}>
              Pedidos
            </NavLink>
            <NavLink to="/facturas" className={navLink}>
              Facturas
            </NavLink>
            <NavLink to="/cuenta" className={navLink}>
              Direcciones
            </NavLink>
          </nav>
        )}
      </header>
      <main id="contenido" className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
