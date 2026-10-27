import { describe, expect, it } from 'vitest'
import { ADDRESS_EXAMPLE, addressProblem } from './addressRules'

describe('address rules', () => {
  it.each([ADDRESS_EXAMPLE, 'Carrera 7 # 40-12, apto 301, Medellín', 'Cl 45 sur 39-10, Sabaneta'])('accepts "%s"', (address) => {
    expect(addressProblem(address)).toBeNull()
  })

  it('rejects a lone town name, the case that slipped through', () => {
    expect(addressProblem('sabaneta')).toMatch(/número/)
  })

  it.each([
    ['', /escribe la dirección/],
    ['Cra 7', /muy corta/],
    ['Calle principal, Bogotá', /número/],
    ['Calle 10 # 5-20 Bogotá', /separa la ciudad con una coma/],
    ['Calle 10 # 5-20, 7', /separa la ciudad con una coma/],
  ])('explains what is missing in "%s"', (address, message) => {
    expect(addressProblem(address)).toMatch(message)
  })

  it('always gives the example in the message', () => {
    expect(addressProblem('sabaneta')).toContain(ADDRESS_EXAMPLE)
  })
})
