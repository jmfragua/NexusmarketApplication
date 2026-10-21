import { useState } from 'react'
import { cn } from '@/lib/cn'
import { coverDataUri } from './cover'

interface ProductCoverProps {
  productId: string
  title: string
  image: string | null
  className?: string
}

/** Uses the `image` attribute when it loads; otherwise draws the deterministic poster. */
export function ProductCover({ productId, title, image, className }: ProductCoverProps) {
  const [failed, setFailed] = useState(false)
  const src = image && !failed ? image : coverDataUri(productId, title)
  return (
    <img
      src={src}
      alt=""
      loading="lazy"
      decoding="async"
      onError={() => setFailed(true)}
      className={cn('size-full object-cover', className)}
    />
  )
}
