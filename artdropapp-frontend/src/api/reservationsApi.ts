import { authFetch } from '../lib/authFetch'

export type MyReservation = {
  artworkId: number
  title: string
  coverPublicId: string | null
  reservedUntil: string
}

export async function fetchMyReservation(): Promise<MyReservation | null> {
  const res = await authFetch('/api/reservations/me')
  if (res.status === 204) return null
  if (!res.ok) throw new Error(`Failed to fetch reservation (${res.status})`)
  return (await res.json()) as MyReservation
}

export async function releaseMyReservation(): Promise<void> {
  const res = await authFetch('/api/reservations/me', { method: 'DELETE' })
  if (!res.ok) throw new Error(`Failed to release reservation (${res.status})`)
}
