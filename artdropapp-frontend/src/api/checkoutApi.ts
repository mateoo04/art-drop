import { authFetch } from '../lib/authFetch'
import type { ShippingAddressInput } from '../types/address'

export type CheckoutSessionRequest = {
  artworkId: number
  quantity: number
  addressId: number | null
  inlineAddress: ShippingAddressInput | null
}

export type CheckoutSessionResponse = {
  orderId: number
  checkoutUrl: string
}

export type CheckoutErrorKind =
  | 'SELF_PURCHASE'
  | 'INVENTORY_UNAVAILABLE'
  | 'BAD_REQUEST'
  | 'UNKNOWN'

export class CheckoutError extends Error {
  constructor(public kind: CheckoutErrorKind, message: string) {
    super(message)
    this.name = 'CheckoutError'
  }
}

export async function createCheckoutSession(
  req: CheckoutSessionRequest,
): Promise<CheckoutSessionResponse> {
  const res = await authFetch('/api/checkout/session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(req),
  })
  if (!res.ok) {
    let body: { error?: string; message?: string } = {}
    try {
      body = (await res.json()) as typeof body
    } catch {
      // ignore
    }
    const kind: CheckoutErrorKind =
      body.error === 'SELF_PURCHASE' ? 'SELF_PURCHASE'
      : body.error === 'INVENTORY_UNAVAILABLE' ? 'INVENTORY_UNAVAILABLE'
      : body.error === 'BAD_REQUEST' ? 'BAD_REQUEST'
      : 'UNKNOWN'
    throw new CheckoutError(kind, body.message ?? `Checkout failed (${res.status})`)
  }
  return (await res.json()) as CheckoutSessionResponse
}
