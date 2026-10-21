import type { Page } from '@/api/types'
import { Button } from './Button'

export function Pagination<T>({ page, onChange }: { page: Page<T>; onChange: (next: number) => void }) {
  if (page.totalPages <= 1) return null
  return (
    <nav className="mt-6 flex items-center justify-between gap-3" aria-label="Paginación">
      <Button variant="outline" size="sm" disabled={page.first} onClick={() => onChange(page.page - 1)}>
        Anterior
      </Button>
      <p className="text-sm text-ink-soft" aria-live="polite">
        página {page.page + 1} de {page.totalPages} · {page.totalElements} en total
      </p>
      <Button variant="outline" size="sm" disabled={page.last} onClick={() => onChange(page.page + 1)}>
        Siguiente
      </Button>
    </nav>
  )
}
