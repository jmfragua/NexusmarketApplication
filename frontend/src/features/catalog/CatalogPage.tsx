import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { ProductResponse, ProductType } from '@/api/types'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Field'
import { EmptyState, ErrorState, Notice, Skeleton } from '@/components/ui/States'
import { cn } from '@/lib/cn'
import { CatalogHero } from './CatalogHero'
import { ProductCard } from './ProductCard'
import { useCatalog } from './queries'
import { readProduct } from './variant'

const PAGE_SIZE = 12

type TypeFilter = 'todos' | 'fisicos' | 'digitales'
const typeOf: Record<TypeFilter, ProductType | null> = { todos: null, fisicos: 'PHYSICAL', digitales: 'DIGITAL' }
const filters: Array<{ value: TypeFilter; label: string }> = [
  { value: 'todos', label: 'Todos' },
  { value: 'fisicos', label: 'Físicos' },
  { value: 'digitales', label: 'Digitales' },
]

function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

function matches(product: ProductResponse, query: string): boolean {
  if (query === '') return true
  const view = readProduct(product)
  const haystack = normalize(
    [view.title, view.brand, view.description, ...view.variants.flatMap((v) => v.options.map(([, value]) => value))]
      .filter(Boolean)
      .join(' '),
  )
  return normalize(query)
    .split(/\s+/)
    .filter(Boolean)
    .every((word) => haystack.includes(word))
}

export function CatalogPage() {
  const { data, isPending, error, refetch } = useCatalog()
  const [params, setParams] = useSearchParams()
  const query = params.get('q') ?? ''
  const filterParam = params.get('tipo')
  const filter: TypeFilter = filterParam === 'fisicos' || filterParam === 'digitales' ? filterParam : 'todos'
  const [page, setPage] = useState(0)

  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value === '' || value === 'todos') next.delete(key)
    else next.set(key, value)
    setParams(next, { replace: true })
    setPage(0)
  }

  const visible = useMemo(() => {
    const type = typeOf[filter]
    return (data?.products ?? []).filter((p) => (type === null || p.productType === type) && matches(p, query))
  }, [data, filter, query])

  const pages = Math.max(1, Math.ceil(visible.length / PAGE_SIZE))
  const current = Math.min(page, pages - 1)
  const slice = visible.slice(current * PAGE_SIZE, current * PAGE_SIZE + PAGE_SIZE)

  return (
    <>
      <CatalogHero featured={data?.products ?? []} />

      <section id="catalogo" className="mx-auto max-w-7xl scroll-mt-20 px-4 pt-10 sm:px-6" aria-labelledby="catalogo-title">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h2 id="catalogo-title" className="text-3xl font-extrabold sm:text-4xl">
              el catálogo
            </h2>
            <p className="mt-1 text-ink-soft" aria-live="polite">
              {isPending ? 'cargando productos…' : `${visible.length} ${visible.length === 1 ? 'producto' : 'productos'}`}
            </p>
          </div>
          <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center">
            <label className="relative block sm:w-80">
              <span className="sr-only">Buscar en el catálogo</span>
              <svg viewBox="0 0 20 20" className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-ink-faint" aria-hidden="true">
                <circle cx="9" cy="9" r="5.5" fill="none" stroke="currentColor" strokeWidth="2" />
                <path d="m13.2 13.2 3.6 3.6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              <Input
                type="search"
                placeholder="buscar por nombre, marca o talla"
                value={query}
                onChange={(e) => update('q', e.target.value)}
                className="rounded-full pl-11"
              />
            </label>
            <fieldset className="inline-flex rounded-full bg-card p-1 ring-1 ring-line-strong">
              <legend className="sr-only">Filtrar por tipo de producto</legend>
              {filters.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  aria-pressed={filter === f.value}
                  onClick={() => update('tipo', f.value)}
                  className={cn(
                    'h-9 flex-1 rounded-full px-4 text-sm font-semibold transition-colors',
                    filter === f.value ? 'bg-ink text-white' : 'text-ink-soft hover:text-ink',
                  )}
                >
                  {f.label}
                </button>
              ))}
            </fieldset>
          </div>
        </div>

        {data && data.source !== 'live' && (
          <div className="mb-6">
            <Notice
              tone={data.source === 'sample-offline' ? 'coral' : 'amber'}
              title={data.source === 'sample-offline' ? 'estás viendo productos de ejemplo' : 'todavía no hay productos publicados'}
            >
              {data.source === 'sample-offline'
                ? 'no pudimos conectar con el servidor. Puedes explorar estos ejemplos, pero para comprar necesitas que el backend esté en línea.'
                : 'mientras los vendedores publican, te mostramos productos de ejemplo. No se pueden comprar.'}
            </Notice>
          </div>
        )}

        {isPending ? (
          <ul className="grid grid-cols-1 gap-5 min-[480px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4" aria-hidden="true">
            {Array.from({ length: 8 }, (_, i) => (
              <li key={i} className="rounded-[var(--radius-card)] bg-card p-2.5 ring-1 ring-line">
                <Skeleton className="aspect-[4/3.4]" />
                <Skeleton className="mt-4 h-4 w-1/3" />
                <Skeleton className="mt-2 h-5 w-3/4" />
              </li>
            ))}
          </ul>
        ) : error ? (
          <ErrorState error={error} onRetry={() => void refetch()} title="no pudimos cargar el catálogo" />
        ) : visible.length === 0 ? (
          <EmptyState
            title="no encontramos coincidencias"
            description={query ? `nada coincide con «${query}». Prueba con otra palabra o quita el filtro.` : 'no hay productos de este tipo por ahora.'}
            action={
              <Button variant="outline" onClick={() => setParams(new URLSearchParams(), { replace: true })}>
                Ver todo el catálogo
              </Button>
            }
          />
        ) : (
          <>
            <ul className="grid grid-cols-1 gap-x-5 gap-y-7 min-[480px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {slice.map((product, i) => (
                <ProductCard key={product.id} product={product} index={i} />
              ))}
            </ul>
            {pages > 1 && (
              <nav className="mt-10 flex items-center justify-center gap-3" aria-label="Páginas del catálogo">
                <Button variant="outline" size="sm" disabled={current === 0} onClick={() => setPage(current - 1)}>
                  Anterior
                </Button>
                <span className="text-sm text-ink-soft">
                  {current + 1} de {pages}
                </span>
                <Button variant="outline" size="sm" disabled={current >= pages - 1} onClick={() => setPage(current + 1)}>
                  Siguiente
                </Button>
              </nav>
            )}
          </>
        )}
      </section>
    </>
  )
}
