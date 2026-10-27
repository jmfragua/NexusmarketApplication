import { describe, expect, it } from 'vitest'
import { parsePrice } from '@/lib/money'
import { buildAttributes, readProduct, readVariant, safeImageSource, variantDetail } from './variant'

describe('parsePrice', () => {
  it.each([
    ['12000', 12000],
    ['12.000', 12000],
    ['1.250.000', 1250000],
    ['12,000.50', 12000.5],
    ['12.000,50', 12000.5],
    ['19.99', 19.99],
    ['$ 45.000', 45000],
  ])('reads %s as %d', (raw, expected) => {
    expect(parsePrice(raw)).toBe(expected)
  })

  it.each(['', 'gratis', '-5', undefined])('rejects %s', (raw) => {
    expect(parsePrice(raw)).toBeNull()
  })
})

describe('variant attribute convention', () => {
  it('separates the reserved keys from the choosable options', () => {
    const view = readVariant({
      id: 'v1',
      attributes: { Name: 'Tenis', price: '159000', brand: 'Malecón', Talla: '40', Color: 'Azul', image: 'javascript:alert(1)' },
    })
    expect(view.name).toBe('Tenis')
    expect(view.price).toBe(159000)
    expect(view.currency).toBe('COP')
    expect(view.brand).toBe('Malecón')
    expect(view.options).toEqual([
      ['Color', 'Azul'],
      ['Talla', '40'],
    ])
    expect(view.image).toBeNull()
  })

  it('advertises the cheapest variant and flags a price range', () => {
    const view = readProduct({
      id: 'abc123def',
      sellerId: 's',
      productType: 'PHYSICAL',
      status: 'PUBLISHED',
      visibleInCatalog: true,
      variants: [
        { id: 'a', attributes: { name: 'Morral', price: '199000' } },
        { id: 'b', attributes: { price: '189000' } },
      ],
    })
    expect(view.title).toBe('Morral')
    expect(view.priceFrom).toBe(189000)
    expect(view.hasPriceRange).toBe(true)
  })

  it('falls back to a readable title when no variant has a name', () => {
    const view = readProduct({
      id: 'p1',
      sellerId: 's',
      productType: 'DIGITAL',
      status: 'PUBLISHED',
      visibleInCatalog: true,
      variants: [{ id: 'v', attributes: { color: 'azul' } }],
    })
    expect(view.title).toBe('Producto P1')
    expect(view.priceFrom).toBeNull()
  })

  it('builds attributes from the seller form, ignoring blanks and reserved option keys', () => {
    expect(
      buildAttributes({
        name: '  Termo  ',
        price: '68.500',
        currency: 'cop',
        description: '',
        brand: 'Páramo',
        options: [
          { key: 'Color', value: 'Coral' },
          { key: 'price', value: '1' },
          { key: ' ', value: 'x' },
        ],
      }),
    ).toEqual({ name: 'Termo', price: '68500', currency: 'COP', brand: 'Páramo', Color: 'Coral' })
  })
})

describe('safeImageSource', () => {
  it('keeps a local path served by the frontend', () => {
    expect(safeImageSource('/productos/morral.jpg')).toBe('/productos/morral.jpg')
    expect(safeImageSource('  /productos/camiseta-basica_2.jpg ')).toBe('/productos/camiseta-basica_2.jpg')
  })

  it('keeps an https URL', () => {
    expect(safeImageSource('https://images.unsplash.com/photo-1?w=800&q=80')).toBe('https://images.unsplash.com/photo-1?w=800&q=80')
  })

  it.each([
    ['a path that climbs out with ..', '/productos/../secretos.jpg'],
    ['a path made only of ..', '/..'],
    ['a protocol-relative URL', '//evil.example/x.jpg'],
    ['a javascript: URL', 'javascript:alert(1)'],
    ['a data: URL', 'data:image/svg+xml,<svg/>'],
    ['a relative path without the leading slash', 'productos/morral.jpg'],
    ['a path with a backslash', '/productos\\morral.jpg'],
    ['a path with spaces', '/productos/mi foto.jpg'],
  ])('rejects %s', (_, value) => {
    expect(safeImageSource(value)).toBeNull()
  })

  it('is what the catalogue reads from a variant', () => {
    expect(readVariant({ id: 'v', attributes: { name: 'Morral', image: '/productos/morral.jpg' } }).image).toBe('/productos/morral.jpg')
    expect(readVariant({ id: 'v', attributes: { name: 'Morral', image: '/../etc/passwd' } }).image).toBeNull()
  })
})

describe('variantDetail', () => {
  const variant = (attributes: Record<string, string>) => readVariant({ id: 'v', attributes })

  it('says nothing when the variant name only repeats the product name', () => {
    expect(variantDetail(variant({ name: 'Mouse inalámbrico Pulso' }), 'Mouse inalámbrico Pulso')).toBe('')
    expect(variantDetail(variant({ name: 'mouse inalambrico  pulso' }), 'Mouse inalámbrico Pulso')).toBe('')
  })

  it('keeps what tells the variant apart', () => {
    expect(variantDetail(variant({ name: 'Camiseta', Talla: 'M' }), 'Camiseta')).toBe('M')
    expect(variantDetail(variant({ name: 'Morral', Color: 'Arena' }), 'Morral')).toBe('Arena')
    expect(variantDetail(variant({ name: 'Edición de lujo' }), 'Libro')).toBe('Edición de lujo')
  })
})
