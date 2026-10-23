import { describe, expect, it } from 'vitest'
import { productTone } from './productTone'

describe('product tone', () => {
  it('is coral for physical products and lime for digital ones', () => {
    expect(productTone('PHYSICAL')).toBe('coral')
    expect(productTone('DIGITAL')).toBe('lime')
  })
})
