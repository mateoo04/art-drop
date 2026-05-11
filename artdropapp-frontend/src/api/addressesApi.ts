import { authFetch } from '../lib/authFetch'
import type { ShippingAddress, ShippingAddressInput } from '../types/address'

function mapAddress(raw: Record<string, unknown>): ShippingAddress {
  return {
    id: Number(raw.id),
    recipientName: String(raw.recipientName ?? ''),
    line1: String(raw.line1 ?? ''),
    line2: raw.line2 == null ? null : String(raw.line2),
    city: String(raw.city ?? ''),
    postalCode: String(raw.postalCode ?? ''),
    country: String(raw.country ?? ''),
    phone: raw.phone == null ? null : String(raw.phone),
  }
}

export async function fetchMyAddresses(): Promise<ShippingAddress[]> {
  const res = await authFetch('/api/addresses')
  if (!res.ok) throw new Error(`Failed to load addresses (${res.status})`)
  const json: unknown = await res.json()
  if (!Array.isArray(json)) throw new Error('Unexpected server response')
  return json.map((el) => mapAddress(el as Record<string, unknown>))
}

export async function createAddress(input: ShippingAddressInput): Promise<ShippingAddress> {
  const res = await authFetch('/api/addresses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  })
  if (!res.ok) throw new Error(`Failed to create address (${res.status})`)
  return mapAddress((await res.json()) as Record<string, unknown>)
}

export async function deleteAddress(id: number): Promise<boolean> {
  const res = await authFetch(`/api/addresses/${id}`, { method: 'DELETE' })
  return res.status === 204
}
