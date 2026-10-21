import { useState, type FormEvent } from 'react'
import { Button } from '@/components/ui/Button'
import { Dialog } from '@/components/ui/Dialog'
import { Field, Input } from '@/components/ui/Field'

interface QuantityDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  title: string
  description: string
  label: string
  submitLabel: string
  pending: boolean
  initial?: number
  onSubmit: (quantity: number) => void
}

/** Asks for a whole positive quantity: the backend rejects zero and negatives for every movement. */
export function QuantityDialog({ open, onOpenChange, title, description, label, submitLabel, pending, initial = 1, onSubmit }: QuantityDialogProps) {
  const [value, setValue] = useState(String(initial))
  const [error, setError] = useState<string | null>(null)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const n = Number(value)
    if (!Number.isInteger(n) || n < 1) {
      setError('escribe un número entero mayor que cero')
      return
    }
    setError(null)
    onSubmit(n)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={title} description={description}>
      <form onSubmit={submit} className="grid gap-5" noValidate>
        <Field label={label} error={error ?? undefined}>
          <Input type="number" inputMode="numeric" min={1} step={1} value={value} onChange={(e) => setValue(e.target.value)} />
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="submit" loading={pending}>
            {submitLabel}
          </Button>
        </div>
      </form>
    </Dialog>
  )
}
