import { fireEvent, render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ProductCover } from './ProductCover'

const photo = 'https://cdn.example.test/morral.jpg'

function image(container: HTMLElement): HTMLImageElement {
  const found = container.querySelector('img')
  if (!found) throw new Error('no <img> rendered')
  return found
}

describe('ProductCover', () => {
  it('draws the generated poster, as decoration, when there is no photo', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral" image={null} />)
    expect(image(container).src).toMatch(/^data:image\/svg\+xml/)
    expect(image(container).getAttribute('alt')).toBe('')
  })

  it('always uses the photo when there is one, describing it with the product name', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral urbano" image={photo} />)
    expect(image(container).src).toBe(photo)
    expect(image(container).getAttribute('alt')).toBe('Morral urbano')
    expect(image(container).className).toContain('object-cover')
  })

  it('shows a skeleton while the photo loads and removes it once it has', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral" image={photo} />)
    expect(container.querySelector('.skeleton')).not.toBeNull()
    expect(image(container).className).toContain('opacity-0')

    fireEvent.load(image(container))

    expect(container.querySelector('.skeleton')).toBeNull()
    expect(image(container).className).toContain('opacity-100')
  })

  it('falls back to the generated poster when the photo fails', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral" image={photo} />)

    fireEvent.error(image(container))

    expect(image(container).src).toMatch(/^data:image\/svg\+xml/)
    expect(container.querySelector('.skeleton')).toBeNull()
  })

  it('starts over with its own skeleton when another photo is chosen', () => {
    const { container, rerender } = render(<ProductCover productId="p-1" title="Morral" image={photo} />)
    fireEvent.load(image(container))
    expect(container.querySelector('.skeleton')).toBeNull()

    rerender(<ProductCover productId="p-1" title="Morral" image="https://cdn.example.test/otro.jpg" />)

    expect(container.querySelector('.skeleton')).not.toBeNull()
  })

  it('lets the caller override the alternative text', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral" image={photo} alt="" />)
    expect(image(container).getAttribute('alt')).toBe('')
  })

  it('shows the whole lamp, contained on a background matching the photo', () => {
    const { container } = render(<ProductCover productId="p-1" title="Lámpara" image="/productos/lampara.jpg" />)
    expect(image(container).className).toContain('object-contain')
    expect(image(container).className).not.toContain('object-cover')
    expect((image(container).parentElement as HTMLElement).style.background).toContain('linear-gradient')
  })

  it('crops the sneakers lower than the centre', () => {
    const { container } = render(<ProductCover productId="p-1" title="Tenis" image="/productos/tenis.jpg" />)
    expect(image(container).style.objectPosition).toBe('50% 58%')
    expect(image(container).className).toContain('object-cover')
  })

  it('keeps the centred crop for a photo that is not in the map', () => {
    const { container } = render(<ProductCover productId="p-1" title="Morral" image={photo} />)
    expect(image(container).style.objectPosition).toBe('center')
  })

  it('still falls back to the generated poster when a framed photo fails', () => {
    const { container } = render(<ProductCover productId="p-1" title="Lámpara" image="/productos/lampara.jpg" />)
    fireEvent.error(image(container))
    expect(image(container).src).toMatch(/^data:image\/svg\+xml/)
  })
})
