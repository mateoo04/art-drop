export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REFUNDED'

export interface OrderShippingAddressSnapshot {
  recipientName: string
  line1: string
  line2: string | null
  city: string
  postalCode: string
  country: string
  phone: string | null
}

export interface Order {
  id: number
  buyerUserId: number
  artistUserId: number
  artworkId: number
  artworkTitle: string | null
  artworkCoverPublicId: string | null
  quantity: number
  status: OrderStatus
  subtotal: number
  shippingFee: number
  platformFee: number
  total: number
  currency: string
  shippingAddress: OrderShippingAddressSnapshot
  trackingNumber: string | null
  shippingCarrier: string | null
  cancellationReason: string | null
  paidAt: string | null
  shippedAt: string | null
  deliveredAt: string | null
  cancelledAt: string | null
  refundedAt: string | null
  createdAt: string
}
