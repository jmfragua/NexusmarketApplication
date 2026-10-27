/**
 * The minimum shape of a delivery address: long enough, with a number and a city after a comma,
 * like "Calle 10 # 5-20, Bogotá". It only checks what a shopper types now; addresses already saved
 * are never re-checked.
 */
export const ADDRESS_EXAMPLE = 'Calle 10 # 5-20, Bogotá'
export const ADDRESS_HINT = `Con número y ciudad, por ejemplo: ${ADDRESS_EXAMPLE}.`

/** Why the address is not usable yet, in plain Spanish, or null when it is fine. */
export function addressProblem(raw: string): string | null {
  const address = raw.trim()
  if (address === '') return 'escribe la dirección de entrega'
  if (address.length < 8) return `la dirección es muy corta: escribe calle, número y ciudad, por ejemplo: ${ADDRESS_EXAMPLE}`
  if (!/\d/.test(address)) return `falta el número de la dirección, por ejemplo: ${ADDRESS_EXAMPLE}`
  const city = address.slice(address.lastIndexOf(',') + 1).trim()
  if (!address.includes(',') || !/\p{L}{2,}/u.test(city) || address.lastIndexOf(',') === 0) {
    return `separa la ciudad con una coma, por ejemplo: ${ADDRESS_EXAMPLE}`
  }
  return null
}
