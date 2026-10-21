import { zodResolver } from '@hookform/resolvers/zod'
import type { ReactNode } from 'react'
import { useFieldArray, useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '@/components/ui/Button'
import { Field, Input, Textarea } from '@/components/ui/Field'
import { buildAttributes, isReservedKey } from '@/features/catalog/variant'
import { parsePrice } from '@/lib/money'

const schema = z.object({
  name: z.string().trim().min(2, 'escribe el nombre que verá el comprador'),
  price: z.string().refine((v) => parsePrice(v) !== null && (parsePrice(v) ?? 0) > 0, 'escribe un precio mayor que cero, por ejemplo 189000'),
  currency: z.string().trim().regex(/^[A-Za-z]{3}$/, 'usa un código de 3 letras, como COP o USD'),
  brand: z.string().trim().optional(),
  description: z.string().trim().max(400, 'máximo 400 caracteres').optional(),
  image: z
    .string()
    .trim()
    .refine((v) => v === '' || /^https?:\/\/\S+$/i.test(v), 'pega una dirección que empiece por https://')
    .optional(),
  options: z.array(
    z.object({
      key: z.string().trim().refine((k) => k === '' || !isReservedKey(k), 'ese nombre está reservado'),
      value: z.string().trim(),
    }),
  ),
})
export type VariantFormValues = z.infer<typeof schema>

interface VariantFormProps {
  defaults?: Partial<VariantFormValues>
  submitLabel: string
  pending: boolean
  onSubmit: (attributes: Record<string, string>) => void
  children?: ReactNode
}

export function VariantForm({ defaults, submitLabel, pending, onSubmit, children }: VariantFormProps) {
  const form = useForm<VariantFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: '',
      price: '',
      currency: 'COP',
      brand: '',
      description: '',
      image: '',
      options: [{ key: '', value: '' }],
      ...defaults,
    },
  })
  const options = useFieldArray({ control: form.control, name: 'options' })
  const errors = form.formState.errors

  return (
    <form noValidate className="grid gap-5" onSubmit={form.handleSubmit((values) => onSubmit(buildAttributes(values)))}>
      {children}
      <Field label="Nombre" error={errors.name?.message}>
        <Input placeholder="Morral urbano 22 L" {...form.register('name')} />
      </Field>
      <div className="grid gap-5 sm:grid-cols-[1fr_8rem]">
        <Field label="Precio" error={errors.price?.message}>
          <Input inputMode="decimal" placeholder="189000" {...form.register('price')} />
        </Field>
        <Field label="Moneda" error={errors.currency?.message}>
          <Input maxLength={3} className="uppercase" {...form.register('currency')} />
        </Field>
      </div>
      <Field label="Marca (opcional)" error={errors.brand?.message}>
        <Input {...form.register('brand')} />
      </Field>
      <Field label="Descripción (opcional)" error={errors.description?.message}>
        <Textarea rows={3} {...form.register('description')} />
      </Field>
      <Field label="Imagen (opcional)" hint="URL pública de la foto. Si no hay, dibujamos una portada." error={errors.image?.message}>
        <Input type="url" placeholder="https://…" {...form.register('image')} />
      </Field>

      <fieldset className="grid gap-3 rounded-2xl bg-paper p-4">
        <legend className="sr-only">Opciones de la variante</legend>
        <div>
          <p className="font-semibold">Opciones que elige el comprador</p>
          <p className="text-sm text-ink-soft">por ejemplo Color: Azul, Talla: M. Deja vacío lo que no aplique.</p>
        </div>
        {options.fields.map((field, i) => (
          <div key={field.id} className="grid grid-cols-[1fr_1fr_auto] items-start gap-2">
            <Field label={`Opción ${i + 1}`} error={errors.options?.[i]?.key?.message}>
              <Input placeholder="Color" {...form.register(`options.${i}.key`)} />
            </Field>
            <Field label={`Valor ${i + 1}`}>
              <Input placeholder="Azul" {...form.register(`options.${i}.value`)} />
            </Field>
            <Button variant="ghost" size="sm" className="mt-7" onClick={() => options.remove(i)} aria-label={`Quitar opción ${i + 1}`}>
              Quitar
            </Button>
          </div>
        ))}
        <Button variant="outline" size="sm" className="justify-self-start" onClick={() => options.append({ key: '', value: '' })}>
          Agregar opción
        </Button>
      </fieldset>

      <Button type="submit" size="lg" loading={pending} className="justify-self-start">
        {submitLabel}
      </Button>
    </form>
  )
}
