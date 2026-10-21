import { cn } from '@/lib/cn'

export type Variant = 'primary' | 'accent' | 'outline' | 'ghost' | 'danger'
export type Size = 'sm' | 'md' | 'lg'

const base =
  'inline-flex items-center justify-center gap-2 rounded-full font-semibold whitespace-nowrap transition-[background-color,color,border-color,transform] duration-150 active:translate-y-px disabled:pointer-events-none disabled:opacity-55 select-none'

const variants: Record<Variant, string> = {
  primary: 'bg-cobalt text-white hover:bg-cobalt-deep',
  accent: 'bg-lime text-ink hover:bg-[#c3f01f] ring-1 ring-inset ring-ink/15',
  outline: 'bg-card text-ink ring-1 ring-inset ring-line-strong hover:ring-ink hover:bg-paper',
  ghost: 'text-ink hover:bg-ink/6',
  danger: 'bg-card text-coral-deep ring-1 ring-inset ring-coral/50 hover:bg-coral-wash',
}

const sizes: Record<Size, string> = {
  sm: 'h-9 px-4 text-sm',
  md: 'h-11 px-5 text-[0.95rem]',
  lg: 'h-13 px-7 text-base',
}

export function buttonClass(variant: Variant = 'primary', size: Size = 'md', className?: string): string {
  return cn(base, variants[variant], sizes[size], className)
}
