import { authFetch } from '../lib/authFetch'
import type { Order, OrderShippingAddressSnapshot, OrderStatus } from '../types/order'

const ORDER_STATUSES: OrderStatus[] = [
  'PENDING_PAYMENT',
  'PAID',
  'SHIPPED',
  'DELIVERED',
  'CANCELLED',
  'REFUNDED',
]

function normalizeTimestamp(value: unknown): string | null {
  if (value == null) return null
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, mo, d, h = 0, mi = 0, s = 0] = value as number[]
    const date = new Date(y, mo - 1, d, h, mi, s)
    return Number.isNaN(date.getTime()) ? String(value) : date.toISOString()
  }
  return String(value)
}

function parseStatus(value: unknown): OrderStatus {
  if (typeof value === 'string' && (ORDER_STATUSES as string[]).includes(value)) {
    return value as OrderStatus
  }
  throw new Error(`Unexpected order status: ${String(value)}`)
}

function mapAddress(raw: unknown): OrderShippingAddressSnapshot {
  const r = (raw ?? {}) as Record<string, unknown>
  return {
    recipientName: String(r.recipientName ?? ''),
    line1: String(r.line1 ?? ''),
    line2: r.line2 == null ? null : String(r.line2),
    city: String(r.city ?? ''),
    postalCode: String(r.postalCode ?? ''),
    country: String(r.country ?? ''),
    phone: r.phone == null ? null : String(r.phone),
  }
}

function mapApiOrder(raw: Record<string, unknown>): Order {
  return {
    id: Number(raw.id),
    buyerUserId: Number(raw.buyerUserId),
    artistUserId: Number(raw.artistUserId),
    artworkId: Number(raw.artworkId),
    artworkTitle: raw.artworkTitle == null ? null : String(raw.artworkTitle),
    artworkCoverPublicId: raw.artworkCoverPublicId == null ? null : String(raw.artworkCoverPublicId),
    quantity: Number(raw.quantity ?? 1),
    status: parseStatus(raw.status),
    subtotal: Number(raw.subtotal),
    shippingFee: Number(raw.shippingFee),
    platformFee: Number(raw.platformFee),
    total: Number(raw.total),
    currency: String(raw.currency ?? 'EUR'),
    shippingAddress: mapAddress(raw.shippingAddress),
    trackingNumber: raw.trackingNumber == null ? null : String(raw.trackingNumber),
    shippingCarrier: raw.shippingCarrier == null ? null : String(raw.shippingCarrier),
    cancellationReason: raw.cancellationReason == null ? null : String(raw.cancellationReason),
    paidAt: normalizeTimestamp(raw.paidAt),
    shippedAt: normalizeTimestamp(raw.shippedAt),
    deliveredAt: normalizeTimestamp(raw.deliveredAt),
    cancelledAt: normalizeTimestamp(raw.cancelledAt),
    refundedAt: normalizeTimestamp(raw.refundedAt),
    createdAt: normalizeTimestamp(raw.createdAt) ?? '',
  }
}

export async function fetchMyOrders(): Promise<Order[]> {
  const res = await authFetch('/api/orders')
  if (!res.ok) throw new Error(`Failed to load orders (${res.status})`)
  const json: unknown = await res.json()
  if (!Array.isArray(json)) throw new Error('Unexpected server response')
  return json.map((el) => mapApiOrder(el as Record<string, unknown>))
}

export async function fetchOrder(id: number): Promise<Order | null> {
  const res = await authFetch(`/api/orders/${id}`)
  if (res.status === 404 || res.status === 403) return null
  if (!res.ok) throw new Error(`Failed to load order (${res.status})`)
  return mapApiOrder((await res.json()) as Record<string, unknown>)
}

export async function cancelOrder(id: number, reason: string | null): Promise<Order> {
  const res = await authFetch(`/api/orders/${id}/cancel`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(reason ? { reason } : {}),
  })
  if (!res.ok) throw new Error(`Failed to cancel order (${res.status})`)
  return mapApiOrder((await res.json()) as Record<string, unknown>)
}

export async function fetchMySales(): Promise<Order[]> {
  const res = await authFetch('/api/sales')
  if (res.status === 403) return []
  if (!res.ok) throw new Error(`Failed to load sales (${res.status})`)
  const json: unknown = await res.json()
  if (!Array.isArray(json)) throw new Error('Unexpected server response')
  return json.map((el) => mapApiOrder(el as Record<string, unknown>))
}

export async function shipOrder(
  id: number,
  trackingNumber: string | null,
  carrier: string | null,
): Promise<Order> {
  const res = await authFetch(`/api/orders/${id}/ship`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ trackingNumber, carrier }),
  })
  if (!res.ok) throw new Error(`Failed to ship order (${res.status})`)
  return mapApiOrder((await res.json()) as Record<string, unknown>)
}

export async function deliverOrder(id: number): Promise<Order> {
  const res = await authFetch(`/api/orders/${id}/deliver`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{}',
  })
  if (!res.ok) throw new Error(`Failed to mark delivered (${res.status})`)
  return mapApiOrder((await res.json()) as Record<string, unknown>)
}
