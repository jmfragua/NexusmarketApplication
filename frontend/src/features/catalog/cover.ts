// Deterministic cover art: the same product id always draws the same poster, so a product without
// an `image` attribute is still recognisable across the catalogue, the cart and the order.
//
// The canvas is 4:3 (400 × 300), the ratio of a catalogue card. Every composition keeps the bottom
// right corner (x ≥ 228, y ≥ 222) free of motif: that is where the price sticker sits, and it must
// never cover the artwork. A square crop of the same canvas keeps the centre, so that zone stays free
// there too.

export const COVER_WIDTH = 400
export const COVER_HEIGHT = 300

export interface Palette {
  bg: string
  a: string
  b: string
  c: string
  name: string
}

export const PALETTES: readonly Palette[] = [
  { name: 'cobalto', bg: '#1f3fd1', a: '#d4ff3a', b: '#ff5a4e', c: '#0b1233' },
  { name: 'noche', bg: '#0b1233', a: '#ff5a4e', b: '#d4ff3a', c: '#1f3fd1' },
  { name: 'lima', bg: '#d4ff3a', a: '#1f3fd1', b: '#0b1233', c: '#ff5a4e' },
  { name: 'coral', bg: '#ff5a4e', a: '#0b1233', b: '#f2f5fb', c: '#d4ff3a' },
  { name: 'niebla', bg: '#e7ecff', a: '#1f3fd1', b: '#ff5a4e', c: '#0b1233' },
  { name: 'durazno', bg: '#ffe3df', a: '#ff5a4e', b: '#1f3fd1', c: '#0b1233' },
  { name: 'limón claro', bg: '#f4ffd0', a: '#0b1233', b: '#1f3fd1', c: '#ff5a4e' },
  { name: 'cielo', bg: '#9db0ff', a: '#0b1233', b: '#d4ff3a', c: '#ffffff' },
]

type Random = () => number
type Colors = readonly [string, string, string]
type Painter = (random: Random, colors: Colors, bg: string) => string

function hash(input: string): number {
  let h = 2166136261
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i)
    h = Math.imul(h, 16777619)
  }
  return h >>> 0
}

// An avalanche step after the hash, so ids that differ in one character (p-1, p-2) do not pick
// neighbouring compositions and palettes. The constant spreads the products of the demo catalogue.
function mixedHash(input: string): number {
  let h = hash(input)
  h ^= h >>> 16
  h = Math.imul(h, 73254853)
  h ^= h >>> 13
  return h >>> 0
}

function rng(seed: number): Random {
  let state = seed || 1
  return () => {
    state ^= state << 13
    state ^= state >>> 17
    state ^= state << 5
    return ((state >>> 0) % 10000) / 10000
  }
}

function between(random: Random, min: number, max: number): number {
  return min + random() * (max - min)
}

function n(value: number): string {
  return String(Math.round(value * 10) / 10)
}

function shuffled(random: Random, colors: Colors): Colors {
  const copy = [...colors]
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(random() * (i + 1))
    const a = copy[i] as string
    copy[i] = copy[j] as string
    copy[j] = a
  }
  return [copy[0] as string, copy[1] as string, copy[2] as string]
}

// Every composition has two layouts, so two products that draw the same composition still differ in
// structure and not just in a few pixels.

// 1. Diagonal stripes in a disc, or in a rounded panel, with a small moon.
const discStripes: Painter = (random, [c1, c2, c3]) => {
  const panel = random() < 0.5
  const width = between(random, 15, 24)
  const angle = Math.floor(random() * 6) * 30
  const stripes = (cx: number, cy: number, span: number) => {
    let out = ''
    for (let i = -10; i <= 10; i++) {
      out += `<rect x="${n(cx + i * width * 2 - width / 2)}" y="${n(cy - span * 1.6)}" width="${n(width)}" height="${n(span * 3.2)}" fill="${c1}"/>`
    }
    return out
  }
  if (panel) {
    const x = between(random, 30, 50)
    const y = between(random, 28, 42)
    const w = between(random, 150, 176)
    const h = between(random, 150, 172)
    const cx = x + w / 2
    const cy = y + h / 2
    return (
      `<defs><clipPath id="panel"><rect x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" rx="34"/></clipPath></defs>` +
      `<rect x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" rx="34" fill="${c2}"/>` +
      `<g clip-path="url(#panel)"><g transform="rotate(${angle} ${n(cx)} ${n(cy)})">${stripes(cx, cy, Math.max(w, h) / 2)}</g></g>` +
      `<rect x="${n(x)}" y="${n(y)}" width="${n(w)}" height="${n(h)}" rx="34" fill="none" stroke="${c3}" stroke-width="7"/>` +
      `<circle cx="${n(x + w)}" cy="${n(y + h * 0.22)}" r="${n(between(random, 22, 30))}" fill="${c3}"/>`
    )
  }
  const cx = between(random, 110, 170)
  const cy = between(random, 108, 138)
  const radius = between(random, 82, 104)
  return (
    `<defs><clipPath id="disc"><circle cx="${n(cx)}" cy="${n(cy)}" r="${n(radius)}"/></clipPath></defs>` +
    `<circle cx="${n(cx)}" cy="${n(cy)}" r="${n(radius)}" fill="${c2}"/>` +
    `<g clip-path="url(#disc)"><g transform="rotate(${angle} ${n(cx)} ${n(cy)})">${stripes(cx, cy, radius)}</g></g>` +
    `<circle cx="${n(cx)}" cy="${n(cy)}" r="${n(radius)}" fill="none" stroke="${c3}" stroke-width="7"/>` +
    `<circle cx="${n(cx + radius * 0.82)}" cy="${n(cy - radius * 0.7)}" r="${n(between(random, 20, 28))}" fill="${c3}"/>`
  )
}

// 2. Three overlapping circles, or a target of offset rings.
const overlappingCircles: Painter = (random, [c1, c2, c3], bg) => {
  if (random() < 0.5) {
    const cx = between(random, 120, 168)
    const cy = between(random, 118, 140)
    const fills = [c1, c2, c3, c2]
    let rings = ''
    for (let i = 0; i < 4; i++) {
      rings += `<circle cx="${n(cx + i * 6)}" cy="${n(cy - i * 5)}" r="${n(96 - i * 24)}" fill="${fills[i]}" stroke="${bg}" stroke-width="3"/>`
    }
    return rings + `<circle cx="${n(between(random, 300, 352))}" cy="${n(between(random, 50, 80))}" r="${n(between(random, 20, 28))}" fill="${c3}"/>`
  }
  const radius = between(random, 60, 76)
  const centres: Array<[number, number, string]> = [
    [between(random, 100, 124), between(random, 108, 124), c1],
    [between(random, 180, 206), between(random, 92, 112), c2],
    [between(random, 142, 168), between(random, 164, 178), c3],
  ]
  return centres
    .map(([x, y, fill]) => `<circle cx="${n(x)}" cy="${n(y)}" r="${n(radius)}" fill="${fill}" fill-opacity="0.94" stroke="${bg}" stroke-width="4"/>`)
    .join('')
}

// 3. Stacked waves, horizontal or vertical, that stop short of the free zone, with a sun.
const waves: Painter = (random, [c1, c2, c3]) => {
  const vertical = random() < 0.5
  const length = vertical ? COVER_HEIGHT : COVER_WIDTH
  const bands = 4 + Math.floor(random() * 2)
  const bottom = between(random, 182, 214)
  const amplitude = between(random, 9, 20)
  const wavelength = between(random, 90, 150)
  const phase = random() * wavelength
  const cols = [c1, c2, c3]
  let paths = ''
  for (let k = 0; k < bands; k++) {
    const y = ((k + 0.8) * bottom) / (bands + 0.4)
    const shift = phase + k * wavelength * 0.37
    let d = `M-10 ${n(y)}`
    for (let x = -10; x <= length + 10; x += 10) {
      d += ` L${x} ${n(y + Math.sin(((x + shift) / wavelength) * Math.PI * 2) * amplitude)}`
    }
    d += ` L${length + 10} ${n(bottom)} L-10 ${n(bottom)} Z`
    paths += `<path d="${d}" fill="${cols[k % 3]}"/>`
  }
  const sun = `<circle cx="${n(between(random, 300, 350))}" cy="${n(between(random, 52, 78))}" r="${n(between(random, 24, 34))}" fill="${c3}"/>`
  // Rotated, the bottom edge of the bands becomes their right edge, which stays left of the free zone.
  return sun + (vertical ? `<g transform="translate(0 ${COVER_HEIGHT}) rotate(-90)">${paths}</g>` : paths)
}

// 4. Dots with a few bigger blocks, or a tile pattern with quarter discs; a block to the right.
const dotGrid: Painter = (random, [c1, c2, c3], bg) => {
  if (random() < 0.5) {
    const x0 = between(random, 22, 30)
    const y0 = between(random, 24, 32)
    const size = 46
    let tiles = ''
    for (let row = 0; row < 4; row++) {
      for (let col = 0; col < 4; col++) {
        const x = x0 + col * (size + 4)
        const y = y0 + row * (size + 4)
        const roll = random()
        if (roll < 0.34) {
          tiles += `<rect x="${n(x)}" y="${n(y)}" width="${size}" height="${size}" rx="8" fill="${c1}"/>`
        } else if (roll < 0.62) {
          tiles += `<rect x="${n(x)}" y="${n(y)}" width="${size}" height="${size}" rx="8" fill="${c2}"/><circle cx="${n(x + size / 2)}" cy="${n(y + size / 2)}" r="13" fill="${bg}"/>`
        } else {
          const flip = Math.floor(random() * 4)
          const corners: Array<[number, number, number]> = [[x, y, 0], [x + size, y, 90], [x + size, y + size, 180], [x, y + size, 270]]
          const [qx, qy, rot] = corners[flip] as [number, number, number]
          tiles += `<path d="M${n(qx)} ${n(qy)} m0 0 h${size} a${size} ${size} 0 0 1 -${size} ${size} z" fill="${c3}" transform="rotate(${rot} ${n(qx)} ${n(qy)})"/>`
        }
      }
    }
    return tiles + `<circle cx="${n(between(random, 300, 344))}" cy="${n(between(random, 76, 110))}" r="${n(between(random, 42, 54))}" fill="${c2}"/>`
  }
  const x0 = between(random, 28, 38)
  const y0 = between(random, 30, 40)
  const gap = 34
  let cells = ''
  for (let row = 0; row < 5; row++) {
    for (let col = 0; col < 7; col++) {
      const x = x0 + col * gap
      const y = y0 + row * gap
      const roll = random()
      if (roll < 0.2) {
        cells += `<rect x="${n(x - 12)}" y="${n(y - 12)}" width="24" height="24" rx="7" fill="${c2}"/>`
      } else if (roll < 0.3) {
        cells += `<circle cx="${n(x)}" cy="${n(y)}" r="14" fill="${c3}"/>`
      } else {
        cells += `<circle cx="${n(x)}" cy="${n(y)}" r="8" fill="${c1}"/>`
      }
    }
  }
  const bx = between(random, 268, 280)
  const by = between(random, 26, 38)
  return (
    cells +
    `<rect x="${n(bx)}" y="${n(by)}" width="${n(between(random, 92, 104))}" height="${n(between(random, 118, 136))}" rx="30" fill="${c2}"/>` +
    `<circle cx="${n(bx + 48)}" cy="${n(by + 92)}" r="22" fill="${c3}"/>`
  )
}

// 5. Concentric half discs rising from the left edge, or hanging from the top, with a sun.
const halfRings: Painter = (random, [c1, c2, c3], bg) => {
  const hanging = random() < 0.5
  const centre = hanging ? between(random, 70, 190) : between(random, 96, 200)
  const rings = 5 + Math.floor(random() * 2)
  const outer = 186
  const cols = [c1, c2, c3, bg]
  let discs = ''
  for (let i = 0; i < rings; i++) {
    const radius = outer - (i * outer) / rings
    discs += hanging
      ? `<circle cx="${n(centre)}" cy="0" r="${n(radius)}" fill="${cols[i % 4]}"/>`
      : `<circle cx="0" cy="${n(centre)}" r="${n(radius)}" fill="${cols[i % 4]}"/>`
  }
  const sx = hanging ? between(random, 40, 120) : between(random, 310, 352)
  const sy = hanging ? between(random, 232, 262) : between(random, 46, 72)
  return `<circle cx="${n(sx)}" cy="${n(sy)}" r="${n(between(random, 22, 30))}" fill="${c2}"/>` + discs
}

// 6. A small range of mountains, or a row of pennants, with a sun and a ground rule.
const mountains: Painter = (random, [c1, c2, c3]) => {
  const base = between(random, 206, 220)
  const sun = `<circle cx="${n(between(random, 306, 352))}" cy="${n(between(random, 58, 84))}" r="${n(between(random, 26, 34))}" fill="${c3}"/>`
  if (random() < 0.5) {
    const count = 4
    const w = between(random, 46, 54)
    let flags = ''
    const cols = [c1, c2, c3, c1]
    for (let i = 0; i < count; i++) {
      const height = between(random, 70, 150)
      const x = 30 + i * (w + 8)
      flags += `<polygon points="${n(x)},${n(base)} ${n(x + w / 2)},${n(base - height)} ${n(x + w)},${n(base)}" fill="${cols[i]}"/>`
    }
    return sun + flags + `<rect x="20" y="${n(base)}" width="${n(30 + count * (w + 8))}" height="7" rx="3.5" fill="${c3}"/>`
  }
  const ax = between(random, 84, 124)
  const ay = between(random, 50, 84)
  const half = between(random, 92, 112)
  const bx = ax + between(random, 66, 100)
  const by = ay + between(random, 40, 66)
  const bhalf = between(random, 66, 86)
  return (
    sun +
    `<polygon points="${n(ax - half)},${n(base)} ${n(ax)},${n(ay)} ${n(ax + half)},${n(base)}" fill="${c1}"/>` +
    `<polygon points="${n(bx - bhalf)},${n(base)} ${n(bx)},${n(by)} ${n(bx + bhalf)},${n(base)}" fill="${c2}"/>` +
    `<rect x="20" y="${n(base)}" width="${n(bx + bhalf - 20)}" height="7" rx="3.5" fill="${c3}"/>`
  )
}

// 7. Bauhaus-style blocks: a square, a disc and a pill; or a row of pills of different heights.
const blocks: Painter = (random, [c1, c2, c3]) => {
  if (random() < 0.5) {
    const cols = [c1, c2, c3, c1]
    let pills = ''
    for (let i = 0; i < 4; i++) {
      const height = between(random, 90, 176)
      pills += `<rect x="${n(30 + i * 48)}" y="${n(214 - height)}" width="40" height="${n(height)}" rx="20" fill="${cols[i]}"/>`
    }
    return (
      pills +
      `<circle cx="${n(between(random, 270, 340))}" cy="${n(between(random, 66, 100))}" r="${n(between(random, 40, 54))}" fill="${c3}"/>` +
      `<rect x="${n(between(random, 236, 270))}" y="${n(between(random, 124, 140))}" width="64" height="64" rx="18" fill="${c2}" opacity="0.95"/>`
    )
  }
  const ax = between(random, 24, 42)
  const ay = between(random, 28, 44)
  const side = between(random, 138, 160)
  const dx = ax + between(random, 120, 150)
  const dy = ay + between(random, 78, 100)
  return (
    `<rect x="${n(ax)}" y="${n(ay)}" width="${n(side)}" height="${n(side)}" rx="26" fill="${c1}"/>` +
    `<circle cx="${n(dx)}" cy="${n(dy)}" r="${n(between(random, 56, 68))}" fill="${c2}"/>` +
    `<rect x="${n(ax + 56)}" y="${n(ay + side - 30)}" width="${n(between(random, 88, 108))}" height="${n(between(random, 56, 66))}" rx="30" fill="${c3}"/>` +
    `<path d="M${n(ax)} ${n(ay + side + 14)} h${n(46)} a46 46 0 0 1 -46 46 z" fill="${c2}"/>`
  )
}

export const COMPOSITIONS: ReadonlyArray<{ name: string; paint: Painter }> = [
  { name: 'rayas', paint: discStripes },
  { name: 'círculos', paint: overlappingCircles },
  { name: 'ondas', paint: waves },
  { name: 'cuadrícula', paint: dotGrid },
  { name: 'medios arcos', paint: halfRings },
  { name: 'montañas', paint: mountains },
  { name: 'bloques', paint: blocks },
]

export function coverChoice(productId: string): { composition: number; palette: number } {
  const h = mixedHash(productId)
  // The two choices read different bits, so a composition does not always come with the same palette.
  return { composition: h % COMPOSITIONS.length, palette: (h >>> 9) % PALETTES.length }
}

export type StickerTone = 'coral' | 'lime' | 'cobalt'

// The sticker must not blend into the poster below it: each palette lists the tones that stand out.
const STICKER_TONES: Record<string, readonly [StickerTone, StickerTone]> = {
  cobalto: ['lime', 'coral'],
  noche: ['lime', 'coral'],
  lima: ['coral', 'cobalt'],
  coral: ['cobalt', 'lime'],
  niebla: ['coral', 'cobalt'],
  durazno: ['cobalt', 'lime'],
  'limón claro': ['coral', 'cobalt'],
  cielo: ['coral', 'lime'],
}

export function stickerTone(productId: string): StickerTone {
  const palette = PALETTES[coverChoice(productId).palette] as Palette
  const tones = STICKER_TONES[palette.name] ?? (['coral', 'lime'] as const)
  return tones[(mixedHash(productId) >>> 17) % 2] as StickerTone
}

export function coverSvg(productId: string): string {
  const seed = mixedHash(productId)
  const { composition, palette } = coverChoice(productId)
  const random = rng(seed ^ 0x9e3779b9)
  const colours = PALETTES[palette] as Palette
  const roles = shuffled(random, [colours.a, colours.b, colours.c])
  const art = (COMPOSITIONS[composition] as (typeof COMPOSITIONS)[number]).paint(random, roles, colours.bg)
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${COVER_WIDTH} ${COVER_HEIGHT}" preserveAspectRatio="xMidYMid slice"><rect width="${COVER_WIDTH}" height="${COVER_HEIGHT}" fill="${colours.bg}"/>${art}</svg>`
}

export function coverDataUri(productId: string): string {
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(coverSvg(productId))}`
}
