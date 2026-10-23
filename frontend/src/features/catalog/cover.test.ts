import { describe, expect, it } from 'vitest'
import { COMPOSITIONS, COVER_HEIGHT, COVER_WIDTH, PALETTES, coverChoice, coverDataUri, coverSvg, stickerTone } from './cover'

const ids = Array.from({ length: 300 }, (_, i) => `product-${i}`)

describe('generated covers', () => {
  it('draws the same poster for the same product, and a different one for another', () => {
    expect(coverSvg('abc')).toBe(coverSvg('abc'))
    expect(coverSvg('abc')).not.toBe(coverSvg('abd'))
  })

  it('has a 4:3 canvas, like a catalogue card', () => {
    const svg = coverSvg('abc')
    expect(svg).toContain(`viewBox="0 0 ${COVER_WIDTH} ${COVER_HEIGHT}"`)
    expect(COVER_WIDTH / COVER_HEIGHT).toBeCloseTo(4 / 3)
  })

  it('carries no lettering: nothing can collide with the price sticker', () => {
    for (const id of ids.slice(0, 60)) expect(coverSvg(id)).not.toContain('<text')
  })

  it('offers at least six compositions and uses all of them and every palette', () => {
    expect(COMPOSITIONS.length).toBeGreaterThanOrEqual(6)
    expect(new Set(ids.map((id) => coverChoice(id).composition)).size).toBe(COMPOSITIONS.length)
    expect(new Set(ids.map((id) => coverChoice(id).palette)).size).toBe(PALETTES.length)
  })

  it('keeps the products of the demo catalogue visibly different from each other', () => {
    const demo = [
      '504a1b9e-e845-4edb-9727-685e1e23aa75',
      'cb3a325b-0de3-4c1b-83b3-2c1e7970394c',
      'cca0c1d8-cd39-4827-838a-93925951e56b',
      'p1',
      ...['morral', 'audifonos', 'cafe', 'curso', 'tenis', 'ebook', 'lampara', 'plantilla'].map((s) => `muestra-${s}`),
    ]
    const pairs = demo.map((id) => `${coverChoice(id).composition}/${coverChoice(id).palette}`)
    expect(new Set(pairs).size).toBe(demo.length)
  })

  it('does not tie every composition to the same palette', () => {
    const palettesOfFirst = new Set(ids.filter((id) => coverChoice(id).composition === 0).map((id) => coverChoice(id).palette))
    expect(palettesOfFirst.size).toBeGreaterThan(4)
  })

  it('encodes as a data URI an <img> can show', () => {
    expect(coverDataUri('abc')).toMatch(/^data:image\/svg\+xml;charset=utf-8,%3Csvg/)
  })
})

describe('sticker tone', () => {
  it('never blends into the poster it sits on', () => {
    const own: Record<string, string> = { coral: 'coral', durazno: 'coral', cobalto: 'cobalt', lima: 'lime', 'limón claro': 'lime' }
    for (const id of ids) {
      const palette = PALETTES[coverChoice(id).palette]?.name ?? ''
      expect(stickerTone(id)).not.toBe(own[palette])
    }
  })

  it('is stable for a product', () => {
    expect(stickerTone('abc')).toBe(stickerTone('abc'))
  })
})
