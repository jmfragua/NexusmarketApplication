export const DEFAULT_CURRENCY = 'COP'

const ZERO_DECIMAL = new Set(['COP', 'CLP', 'JPY', 'PYG'])

export function formatMoney(amount: number, currency: string = DEFAULT_CURRENCY): string {
  const code = currency.toUpperCase()
  const digits = ZERO_DECIMAL.has(code) ? 0 : 2
  try {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: code,
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    }).format(amount)
  } catch {
    return `${amount.toFixed(digits)} ${code}`
  }
}

/**
 * Reads a price typed by a person or stored as a variant attribute. Accepts `12000`, `12.000`,
 * `12,000.50` and `12.000,50`; returns null for anything that is not a non negative number.
 */
export function parsePrice(raw: string | undefined): number | null {
  if (raw === undefined) return null
  const cleaned = raw.trim().replace(/[^\d.,-]/g, '')
  if (cleaned === '' || cleaned.includes('-')) return null
  const lastComma = cleaned.lastIndexOf(',')
  const lastDot = cleaned.lastIndexOf('.')
  let normalized: string
  if (lastComma > -1 && lastDot > -1) {
    const decimalSep = lastComma > lastDot ? ',' : '.'
    const thousandSep = decimalSep === ',' ? '.' : ','
    normalized = cleaned.split(thousandSep).join('').replace(decimalSep, '.')
  } else if (lastComma > -1 || lastDot > -1) {
    const sep = lastComma > -1 ? ',' : '.'
    const parts = cleaned.split(sep)
    const tail = parts[parts.length - 1] ?? ''
    // A single separator followed by exactly three digits is a thousands mark: 12.000 is twelve thousand.
    normalized = parts.length === 2 && tail.length !== 3 ? parts.join('.') : parts.join('')
  } else {
    normalized = cleaned
  }
  const value = Number(normalized)
  return Number.isFinite(value) && value >= 0 ? value : null
}

/** Rounds to cents so a sum of lines never drifts from what a person would compute by hand. */
export function roundMoney(amount: number): number {
  return Math.round(amount * 100) / 100
}
