import { authFetch } from '../lib/authFetch'
import type { ShippingAddressInput } from '../types/address'

export type CheckoutSessionRequest = {
  artworkId: number
  quantity: number
  addressId: number | null
  inlineAddress: ShippingAddressInput | null
  replaceExistingReservation?: boolean
}

export type CheckoutSessionResponse = {
  orderId: number
  checkoutUrl: string
}

export type CheckoutErrorKind =
  | 'SELF_PURCHASE'
  | 'INVENTORY_UNAVAILABLE'
  | 'RESERVATION_CONFLICT'
  | 'BAD_REQUEST'
  | 'UNKNOWN'

export type ReservationConflictExistingArtwork = { id: number; title: string }

export class CheckoutError extends Error {
  kind: CheckoutErrorKind
  existingArtwork?: ReservationConflictExistingArtwork

  constructor(
    kind: CheckoutErrorKind,
    message: string,
    existingArtwork?: ReservationConflictExistingArtwork,
  ) {
    super(message)
    this.name = 'CheckoutError'
    this.kind = kind
    this.existingArtwork = existingArtwork
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
    let body: { error?: string; message?: string; existingArtwork?: ReservationConflictExistingArtwork } = {}
    try {
      body = (await res.json()) as typeof body
    } catch {
      // ignore
    }
    const kind: CheckoutErrorKind =
      body.error === 'SELF_PURCHASE' ? 'SELF_PURCHASE'
      : body.error === 'INVENTORY_UNAVAILABLE' ? 'INVENTORY_UNAVAILABLE'
      : body.error === 'RESERVATION_CONFLICT' ? 'RESERVATION_CONFLICT'
      : body.error === 'BAD_REQUEST' ? 'BAD_REQUEST'
      : 'UNKNOWN'
    throw new CheckoutError(
      kind,
      body.message ?? `Checkout failed (${res.status})`,
      body.existingArtwork,
    )
  }
  return (await res.json()) as CheckoutSessionResponse
}
