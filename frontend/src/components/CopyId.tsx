import { useState } from 'react'
import { toast } from 'sonner'
import { shortId } from '@/lib/id'

/** Shows the short form of an identifier and copies the full one, which other roles need to act on it. */
export function CopyId({ id, label = 'identificador' }: { id: string; label?: string }) {
  const [copied, setCopied] = useState(false)
  const copy = async () => {
    try {
      await navigator.clipboard.writeText(id)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1600)
    } catch {
      toast.error('no pudimos copiar; selecciona el texto manualmente')
    }
  }
  return (
    <button
      type="button"
      onClick={() => void copy()}
      title={id}
      className="inline-flex items-center gap-1.5 rounded-full bg-ink/6 px-2.5 py-1 font-mono text-xs font-semibold text-ink-soft hover:bg-ink/10 hover:text-ink"
      aria-label={`Copiar ${label} completo`}
    >
      {shortId(id)}
      <span className="font-sans" aria-live="polite">
        {copied ? 'copiado' : 'copiar'}
      </span>
    </button>
  )
}
