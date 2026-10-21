import type { BuyerResponse } from '@/api/types'
import { readJson, writeJson } from '@/lib/storage'

// The API has no "read my profile" endpoint, so the addresses the backend last confirmed for a
// buyer are remembered locally to offer them at checkout. The backend stays the authority: the
// chosen address is registered again (idempotently) before the order is placed.

export interface AddressBook {
  primary: string
  additional: string[]
}

const key = (userId: string) => `nexusmarket.addresses.${userId}`

function isAddressBook(value: unknown): value is AddressBook {
  if (typeof value !== 'object' || value === null) return false
  const v = value as Record<string, unknown>
  return typeof v.primary === 'string' && Array.isArray(v.additional) && v.additional.every((a) => typeof a === 'string')
}

export function readAddressBook(userId: string): AddressBook | null {
  return readJson(key(userId), isAddressBook)
}

export function rememberPrimaryAddress(userId: string, primary: string): void {
  const current = readAddressBook(userId)
  writeJson(key(userId), { primary, additional: current?.additional ?? [] })
}

export function rememberBuyer(buyer: BuyerResponse): AddressBook {
  const book = { primary: buyer.primaryAddress, additional: buyer.additionalAddresses }
  writeJson(key(buyer.id), book)
  return book
}

export function knownAddresses(book: AddressBook | null): string[] {
  if (!book) return []
  return [book.primary, ...book.additional.filter((a) => a !== book.primary)].filter((a) => a.trim() !== '')
}
