import { describe, expect, it } from 'vitest'
import { imageFrame } from './imageFrame'

describe('image frame', () => {
  it('shows the whole lamp on a background taken from the photo', () => {
    expect(imageFrame('/productos/lampara.jpg')).toEqual({ position: 'center', fit: 'contain', background: expect.stringContaining('#363332') })
  })

  it('moves the crop down for the sneakers and the photography course', () => {
    expect(imageFrame('/productos/tenis.jpg')).toEqual({ position: '50% 58%', fit: 'cover' })
    expect(imageFrame('/productos/curso.jpg')).toEqual({ position: '50% 58%', fit: 'cover' })
  })

  it('keeps the centred crop for any other image, a URL or no image', () => {
    expect(imageFrame('/productos/morral.jpg')).toEqual({ position: 'center', fit: 'cover' })
    expect(imageFrame('https://images.example.test/x.jpg')).toEqual({ position: 'center', fit: 'cover' })
    expect(imageFrame(null)).toEqual({ position: 'center', fit: 'cover' })
  })
})
