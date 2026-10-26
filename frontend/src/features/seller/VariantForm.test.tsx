import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { VariantForm } from './VariantForm'

function setup() {
  const onSubmit = vi.fn<(attributes: Record<string, string>) => void>()
  render(<VariantForm submitLabel="Guardar" pending={false} onSubmit={onSubmit} defaults={{ name: 'Morral urbano', price: '189000' }} />)
  return { onSubmit, image: screen.getByLabelText(/Imagen/) }
}

describe('variant form image field', () => {
  it('explains that a local path is accepted', () => {
    setup()
    expect(screen.getByText('URL pública o ruta local como /productos/foto.jpg')).toBeInTheDocument()
  })

  it('accepts a local path and saves it as the image attribute', async () => {
    const { onSubmit, image } = setup()
    await userEvent.type(image, '/productos/morral.jpg')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ image: '/productos/morral.jpg' }))
  })

  it('still accepts an https URL', async () => {
    const { onSubmit, image } = setup()
    await userEvent.type(image, 'https://images.unsplash.com/photo-1.jpg')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(onSubmit).toHaveBeenCalledWith(expect.objectContaining({ image: 'https://images.unsplash.com/photo-1.jpg' }))
  })

  it.each(['/productos/../x.jpg', 'javascript:alert(1)'])('rejects %s and does not submit', async (value) => {
    const { onSubmit, image } = setup()
    await userEvent.type(image, value)
    await userEvent.click(screen.getByRole('button', { name: 'Guardar' }))
    expect(await screen.findByText('usa una dirección https:// o una ruta local como /productos/foto.jpg')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })
})
