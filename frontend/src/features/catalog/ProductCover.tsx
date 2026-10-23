import { useState } from 'react'
import { cn } from '@/lib/cn'
import { coverDataUri } from './cover'

interface ProductCoverProps {
  productId: string
  title: string
  /** The `image` attribute of the variant, when it has one. */
  image: string | null
  className?: string
  /**
   * Overrides the alternative text. By default a real photo is described with the product name and
   * the generated poster is decorative (empty text): it carries no information of its own, and the
   * name is always printed next to it.
   */
  alt?: string
}

function GeneratedCover({ productId, className }: { productId: string; className?: string }) {
  return (
    <img
      src={coverDataUri(productId)}
      alt=""
      loading="lazy"
      decoding="async"
      draggable={false}
      className={cn('size-full object-cover', className)}
    />
  )
}

type Status = 'loading' | 'loaded' | 'failed'

/** A photo that fades in over a skeleton, and gives way to the generated poster if it cannot load. */
function PhotoCover({ productId, title, image, className, alt }: Omit<ProductCoverProps, 'image'> & { image: string }) {
  const [status, setStatus] = useState<Status>('loading')
  if (status === 'failed') return <GeneratedCover productId={productId} className={className} />
  return (
    <span className="relative block size-full">
      {status === 'loading' && <span className="skeleton absolute inset-0 rounded-none" aria-hidden="true" />}
      <img
        src={image}
        alt={alt ?? title}
        loading="lazy"
        decoding="async"
        draggable={false}
        onLoad={() => setStatus('loaded')}
        onError={() => setStatus('failed')}
        className={cn('size-full object-cover transition-opacity duration-300', status === 'loaded' ? 'opacity-100' : 'opacity-0', className)}
      />
    </span>
  )
}

/**
 * Uses the `image` attribute whenever there is one, and draws the deterministic poster otherwise.
 * The parent decides the size and the ratio (4:3 in the catalogue); this fills it.
 */
export function ProductCover({ productId, title, image, className, alt }: ProductCoverProps) {
  if (!image) return <GeneratedCover productId={productId} className={className} />
  // Keyed by URL: choosing another variant starts over with its own skeleton and its own failure.
  return <PhotoCover key={image} productId={productId} title={title} image={image} className={className} alt={alt} />
}
