import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Field'
import { ErrorState, Notice, Skeleton } from '@/components/ui/States'
import { cn } from '@/lib/cn'
import { applyFilters, hasActiveFilters, PAGE_SIZE, PARAM, readFilters, sameStore, SORT_OPTIONS, storesOf, type TypeFilter } from './catalogFilters'
import { CatalogHero } from './CatalogHero'
import { HowItWorks } from './HowItWorks'
import { ProductCard } from './ProductCard'
import { useCatalog } from './queries'

const typeFilters: Array<{ value: TypeFilter; label: string }> = [
  { value: 'todos', label: 'Todos' },
  { value: 'fisicos', label: 'Físicos' },
  { value: 'digitales', label: 'Digitales' },
]

function SearchIcon() {
  return (
    <svg viewBox="0 0 20 20" className="pointer-events-none absolute top-1/2 left-3.5 size-5 -translate-y-1/2 text-ink-faint" aria-hidden="true">
      <circle cx="9" cy="9" r="5.5" fill="none" stroke="currentColor" strokeWidth="2" />
      <path d="m13.2 13.2 3.6 3.6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  )
}

function CardSkeletons() {
  return (
    <ul className="grid grid-cols-1 gap-5 min-[480px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4" aria-hidden="true" data-testid="catalog-skeletons">
      {Array.from({ length: 8 }, (_, i) => (
        <li key={i} className="rounded-[var(--radius-card)] bg-card p-2.5 ring-1 ring-line">
          <Skeleton className="aspect-[4/3]" />
          <Skeleton className="mt-5 h-5 w-3/4" />
          <Skeleton className="mt-2.5 h-4 w-1/3" />
          <Skeleton className="mt-4 h-4 w-1/2" />
        </li>
      ))}
    </ul>
  )
}

export function CatalogPage() {
  const { data, isPending, error, refetch } = useCatalog()
  const [params, setParams] = useSearchParams()
  const filters = readFilters(params)
  const active = hasActiveFilters(filters)
  // "Ver más" counts per filtered view: any change of the filters starts again from the first 12.
  const viewKey = params.toString()
  const [more, setMore] = useState({ key: viewKey, shown: PAGE_SIZE })
  const shown = more.key === viewKey ? more.shown : PAGE_SIZE

  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params)
    if (value.trim() === '' || (key === PARAM.type && value === 'todos') || (key === PARAM.sort && value === 'destacados')) next.delete(key)
    else next.set(key, value)
    setParams(next, { replace: true })
  }
  const clearAll = () => setParams(new URLSearchParams(), { replace: true })

  const products = data?.products
  const found = useMemo(() => applyFilters(products ?? [], readFilters(params)), [products, params])
  const slice = found.slice(0, shown)
  const stores = useMemo(() => storesOf(products ?? []), [products])
  const knownStore = filters.store !== null && stores.some((s) => sameStore(s, filters.store))

  return (
    <>
      <CatalogHero featured={data?.products ?? []} />
      <HowItWorks />

      <section id="catalogo" className="mx-auto max-w-7xl scroll-mt-20 px-4 pt-10 sm:px-6" aria-labelledby="catalogo-title">
        <div className="mb-4 flex flex-wrap items-end justify-between gap-x-4 gap-y-3">
          <div className="flex flex-wrap items-baseline gap-x-3 gap-y-0.5">
            <h2 id="catalogo-title" className="text-3xl font-extrabold sm:text-4xl">
              el catálogo
            </h2>
            <p className="text-ink-soft" aria-live="polite" data-testid="catalog-count">
              {isPending ? 'cargando productos…' : `${found.length} ${found.length === 1 ? 'producto' : 'productos'}`}
            </p>
          </div>
          <div className="flex w-full flex-col gap-3 sm:w-auto sm:flex-row sm:items-center">
            <label className="relative block sm:w-80">
              <span className="sr-only">Buscar en el catálogo</span>
              <SearchIcon />
              <Input
                type="search"
                placeholder="Buscar por nombre, marca o talla"
                value={filters.query}
                onChange={(e) => update(PARAM.query, e.target.value)}
                className="rounded-full pl-11"
              />
            </label>
            <fieldset className="inline-flex rounded-full bg-card p-1 ring-1 ring-line-strong">
              <legend className="sr-only">Filtrar por tipo de producto</legend>
              {typeFilters.map((f) => (
                <button
                  key={f.value}
                  type="button"
                  aria-pressed={filters.type === f.value}
                  onClick={() => update(PARAM.type, f.value)}
                  className={cn(
                    'h-9 flex-1 rounded-full px-4 text-sm font-semibold transition-colors',
                    filters.type === f.value ? 'bg-ink text-white' : 'text-ink-soft hover:text-ink',
                  )}
                >
                  {f.label}
                </button>
              ))}
            </fieldset>
          </div>
        </div>

        <div className="mb-6 flex flex-wrap items-end gap-x-4 gap-y-3 rounded-[var(--radius-card)] bg-card p-3 ring-1 ring-line sm:px-4">
          <label className="grid gap-1 text-sm font-semibold">
            Ordenar por
            <select
              value={filters.sort}
              onChange={(e) => update(PARAM.sort, e.target.value)}
              className="h-11 rounded-xl border border-field bg-card px-3 font-normal text-ink hover:border-ink focus-visible:border-cobalt focus-visible:ring-2 focus-visible:ring-cobalt focus-visible:outline-none"
            >
              {SORT_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
          <fieldset className="grid gap-1">
            <legend className="mb-1 text-sm font-semibold">Precio (COP)</legend>
            <div className="flex items-center gap-2">
              <label className="sr-only" htmlFor="precio-min">
                Precio mínimo
              </label>
              <div className="w-28">
                <Input id="precio-min" inputMode="numeric" placeholder="Mínimo" value={params.get(PARAM.min) ?? ''} onChange={(e) => update(PARAM.min, e.target.value)} />
              </div>
              <span aria-hidden="true" className="text-ink-soft">
                –
              </span>
              <label className="sr-only" htmlFor="precio-max">
                Precio máximo
              </label>
              <div className="w-28">
                <Input id="precio-max" inputMode="numeric" placeholder="Máximo" value={params.get(PARAM.max) ?? ''} onChange={(e) => update(PARAM.max, e.target.value)} />
              </div>
            </div>
          </fieldset>
          {stores.length > 1 && (
            <fieldset className="grid min-w-0 gap-1">
              <legend className="mb-1 text-sm font-semibold">Tiendas</legend>
              <div className="flex flex-wrap gap-1.5">
                {stores.map((store) => {
                  const on = sameStore(store, filters.store)
                  return (
                    <button
                      key={store}
                      type="button"
                      aria-pressed={on}
                      onClick={() => update(PARAM.store, on ? '' : store)}
                      className={cn(
                        'inline-flex h-11 items-center gap-1.5 rounded-full px-3.5 text-sm font-semibold ring-1 transition-colors ring-inset',
                        on ? 'bg-cobalt text-white ring-cobalt' : 'bg-card text-ink-soft ring-line-strong hover:text-ink hover:ring-ink',
                      )}
                    >
                      {on && (
                        <svg viewBox="0 0 20 20" className="size-4" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                          <path d="m4.5 10.5 3.5 3.5 7.5-8" />
                        </svg>
                      )}
                      {store}
                    </button>
                  )
                })}
              </div>
            </fieldset>
          )}
          {filters.store && !knownStore && (
            <span className="inline-flex h-11 items-center gap-1 rounded-full bg-cobalt-wash pr-1 pl-3.5 text-sm font-semibold text-cobalt-deep ring-1 ring-cobalt/40">
              tienda: {filters.store}
              <button
                type="button"
                onClick={() => update(PARAM.store, '')}
                className="grid size-8 place-items-center rounded-full hover:bg-cobalt/10"
                aria-label={`Quitar el filtro de la tienda ${filters.store}`}
              >
                <svg viewBox="0 0 20 20" className="size-4" aria-hidden="true">
                  <path d="m5.5 5.5 9 9m0-9-9 9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
              </button>
            </span>
          )}
          {active && (
            <Button variant="ghost" size="sm" className="h-11 sm:ml-auto" onClick={clearAll}>
              Limpiar filtros
            </Button>
          )}
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
          <CardSkeletons />
        ) : error ? (
          <ErrorState error={error} onRetry={() => void refetch()} title="no pudimos cargar el catálogo" />
        ) : found.length === 0 ? (
          <div className="grid justify-items-center gap-3 rounded-[var(--radius-card)] bg-card px-6 py-12 text-center ring-1 ring-line">
            <h3 className="text-2xl font-extrabold">ningún producto coincide</h3>
            <p className="max-w-md text-ink-soft">
              {filters.query.trim() ? `nada coincide con «${filters.query.trim()}» con estos filtros. ` : 'no hay productos con estos filtros. '}
              Prueba con otra palabra, amplía el rango de precio o quita algún filtro.
            </p>
            <Button variant="outline" onClick={clearAll}>
              Limpiar filtros
            </Button>
          </div>
        ) : (
          <>
            <ul className="grid grid-cols-1 gap-x-5 gap-y-7 min-[480px]:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {slice.map((product, i) => (
                <ProductCard key={product.id} product={product} index={i % PAGE_SIZE} />
              ))}
            </ul>
            <div className="mt-10 grid justify-items-center gap-3">
              <p className="text-sm text-ink-soft">
                mostrando {slice.length} de {found.length}
              </p>
              {slice.length < found.length && (
                <Button variant="outline" onClick={() => setMore({ key: viewKey, shown: shown + PAGE_SIZE })}>
                  Ver más productos
                </Button>
              )}
            </div>
          </>
        )}
      </section>
    </>
  )
}
