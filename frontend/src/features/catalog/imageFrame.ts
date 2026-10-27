/**
 * How a product photo sits in its frame. Every frame of the store has a fixed ratio (4:3 in the
 * catalogue and the hero, square on the product page), so a portrait photo is cropped; this says
 * which part of it to keep, or that it must be shown whole on a matching background.
 */
export interface ImageFrame {
  /** CSS object-position of the part to keep when cropping. */
  position: string
  /** 'contain' shows the whole photo; the free space takes `background`. */
  fit: 'cover' | 'contain'
  /** Fills the free space of a contained photo; sampled from the photo's own edges. */
  background?: string
}

const DEFAULT_FRAME: ImageFrame = { position: 'center', fit: 'cover' }

// Only photos that crop badly are listed; any other image keeps the centred crop.
const FRAMES: Record<string, Partial<ImageFrame>> = {
  // Lamp, bottle and plant are as tall as the photo: any crop loses the head or the base.
  '/productos/lampara.jpg': { fit: 'contain', background: 'linear-gradient(#363332, #605c5d)' },
  '/productos/termo.jpg': { fit: 'contain', background: 'linear-gradient(#f6f6f4, #e0e6ed)' },
  '/productos/matera.jpg': { fit: 'contain', background: '#f8f8f6' },
  // The subject sits below the middle of the photo.
  '/productos/tenis.jpg': { position: '50% 58%' },
  '/productos/curso.jpg': { position: '50% 58%' },
  // The open guide lies right of centre.
  '/productos/guia.jpg': { position: '65% 50%' },
}

export function imageFrame(image: string | null): ImageFrame {
  if (!image) return DEFAULT_FRAME
  const frame = FRAMES[image]
  return frame ? { ...DEFAULT_FRAME, ...frame } : DEFAULT_FRAME
}
