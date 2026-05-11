export interface ShippingAddress {
  id: number
  recipientName: string
  line1: string
  line2: string | null
  city: string
  postalCode: string
  country: string
  phone: string | null
}

export interface ShippingAddressInput {
  recipientName: string
  line1: string
  line2: string | null
  city: string
  postalCode: string
  country: string
  phone: string | null
}
