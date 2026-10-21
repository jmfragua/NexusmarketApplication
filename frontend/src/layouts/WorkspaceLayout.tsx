import { useState, type ReactNode } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Logo } from '@/components/Logo'
import { useSession } from '@/features/auth/session'
import { cn } from '@/lib/cn'
import { roleLabel } from '@/lib/labels'

export interface WorkspaceLink {
  to: string
  label: string
  end?: boolean
}

const linkClass = ({ isActive }: { isActive: boolean }) =>
  cn(
    'flex items-center rounded-xl px-3.5 py-2.5 font-semibold transition-colors',
    isActive ? 'bg-lime text-ink' : 'text-white/75 hover:bg-white/8 hover:text-white',
  )

function SignOut({ className }: { className?: string }) {
  const { signOut } = useSession()
  const navigate = useNavigate()
  return (
    <button
      type="button"
      onClick={() => {
        signOut('manual')
        navigate('/')
      }}
      className={cn('rounded-xl px-3.5 py-2.5 text-left font-semibold text-white/75 hover:bg-white/8 hover:text-white', className)}
    >
      Salir
    </button>
  )
}

export function WorkspaceLayout({ links, home, footer }: { links: WorkspaceLink[]; home: string; footer?: ReactNode }) {
  const { session } = useSession()
  const [menuOpen, setMenuOpen] = useState(false)

  const nav = (
    <nav aria-label="Panel" className="grid gap-1">
      {links.map((link) => (
        <NavLink key={link.to} to={link.to} end={link.end} className={linkClass} onClick={() => setMenuOpen(false)}>
          {link.label}
        </NavLink>
      ))}
    </nav>
  )

  return (
    <div className="min-h-dvh lg:grid lg:grid-cols-[16.5rem_1fr]">
      <a href="#panel" className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:rounded-full focus:bg-lime focus:px-4 focus:py-2 focus:text-ink">
        Saltar al contenido
      </a>
      <aside className="hidden bg-ink lg:sticky lg:top-0 lg:flex lg:h-dvh lg:flex-col lg:gap-8 lg:p-5">
        <Logo to={home} inverted />
        <div className="rounded-2xl bg-white/6 p-3.5 text-white">
          <p className="text-xs font-semibold text-lime">{session ? roleLabel[session.role] : ''}</p>
          <p className="truncate text-sm text-white/80" title={session?.email}>
            {session?.email}
          </p>
        </div>
        {nav}
        <div className="mt-auto grid gap-1">
          <NavLink to="/" className={linkClass} end>
            Ver la tienda
          </NavLink>
          <SignOut />
          {footer}
        </div>
      </aside>

      <header className="sticky top-0 z-30 bg-ink lg:hidden">
        <div className="flex h-16 items-center justify-between px-4">
          <Logo to={home} inverted />
          <button
            type="button"
            onClick={() => setMenuOpen((v) => !v)}
            aria-expanded={menuOpen}
            aria-controls="menu-panel"
            className="rounded-full px-4 py-2 font-semibold text-white ring-1 ring-white/30"
          >
            {menuOpen ? 'Cerrar' : 'Menú'}
          </button>
        </div>
        <div id="menu-panel" hidden={!menuOpen} className="grid gap-4 px-4 pb-5">
          <p className="text-sm text-white/70">
            {session ? `${roleLabel[session.role]} · ${session.email}` : ''}
          </p>
          {nav}
          <div className="grid gap-1 border-t border-white/10 pt-3">
            <NavLink to="/" className={linkClass} end>
              Ver la tienda
            </NavLink>
            <SignOut />
          </div>
        </div>
      </header>

      <main id="panel" className="min-w-0 px-4 py-8 sm:px-8 lg:py-10">
        <div className="mx-auto max-w-6xl">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
