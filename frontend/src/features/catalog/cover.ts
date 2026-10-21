// Deterministic cover art: the same product id always draws the same poster, so a product without
// an `image` attribute is still recognisable across the catalogue, the cart and the order.

type Palette = [string, string, string]
const DEFAULT_PALETTE: Palette = ['#1f3fd1', '#d4ff3a', '#ff5a4e']
const PALETTES: Palette[] = [
  ['#1f3fd1', '#d4ff3a', '#ff5a4e'],
  ['#0b1233', '#ff5a4e', '#d4ff3a'],
  ['#d4ff3a', '#1f3fd1', '#0b1233'],
  ['#ff5a4e', '#0b1233', '#f3f6fb'],
  ['#e8edff', '#1f3fd1', '#ff5a4e'],
  ['#14b8a6', '#0b1233', '#d4ff3a'],
]

function hash(input: string): number {
  let h = 2166136261
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

function rng(seed: number): () => number {
  let state = seed || 1
  return () => {
    state ^= state << 13
    state ^= state >>> 17
    state ^= state << 5
    return ((state >>> 0) % 10000) / 10000
  }
}

function escapeXml(text: string): string {
  return text.replace(/[<>&'"]/g, (c) => ({ '<': '&lt;', '>': '&gt;', '&': '&amp;', "'": '&apos;', '"': '&quot;' })[c] ?? c)
}

function initials(title: string): string {
  const words = title.split(/\s+/).filter((w) => /[\p{L}\p{N}]/u.test(w))
  const letters = words.slice(0, 2).map((w) => Array.from(w)[0] ?? '')
  return letters.join('').toUpperCase() || 'N'
}

export function coverSvg(productId: string, title: string): string {
  const seed = hash(productId)
  const random = rng(seed)
  const palette = PALETTES[seed % PALETTES.length] ?? DEFAULT_PALETTE
  const [bg, a, b] = palette
  const variant = seed % 3
  const shapes: string[] = []

  if (variant === 0) {
    const r = 120 + random() * 60
    shapes.push(`<circle cx="${300 + random() * 60}" cy="${90 + random() * 60}" r="${r}" fill="${a}"/>`)
    shapes.push(`<rect x="-20" y="${250 + random() * 40}" width="260" height="70" rx="35" fill="${b}" transform="rotate(-12 120 300)"/>`)
  } else if (variant === 1) {
    for (let i = 0; i < 6; i++) {
      shapes.push(`<rect x="${-40 + i * 80}" y="-20" width="34" height="460" fill="${i % 2 ? a : b}" transform="rotate(18 200 200)" opacity="0.95"/>`)
    }
  } else {
    const cx = 120 + random() * 160
    shapes.push(`<path d="M${cx} 40 L${cx + 150} 330 L${cx - 150} 330 Z" fill="${a}"/>`)
    shapes.push(`<circle cx="${80 + random() * 240}" cy="${300}" r="${50 + random() * 30}" fill="${b}"/>`)
  }

  const text = escapeXml(initials(title))
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" preserveAspectRatio="xMidYMid slice"><rect width="400" height="400" fill="${bg}"/>${shapes.join('')}<text x="28" y="372" font-family="Bricolage Grotesque, system-ui, sans-serif" font-weight="800" font-size="96" fill="${bg === '#0b1233' || bg === '#1f3fd1' ? '#f3f6fb' : '#0b1233'}" letter-spacing="-4">${text}</text></svg>`
}

export function coverDataUri(productId: string, title: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(coverSvg(productId, title))}`
}
