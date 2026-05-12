import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { cloudinaryUrl } from '../../lib/cloudinary'
import { qk } from '../../lib/queryKeys'
import { useMyReservation, useReleaseMyReservation } from '../../hooks/useMyReservation'

function formatRemaining(msLeft: number): { minutes: string; seconds: string } {
  const total = Math.max(0, Math.floor(msLeft / 1000))
  const minutes = String(Math.floor(total / 60)).padStart(2, '0')
  const seconds = String(total % 60).padStart(2, '0')
  return { minutes, seconds }
}

export function ReservationHeaderTimer() {
  const { data } = useMyReservation()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const location = useLocation()
  const qc = useQueryClient()
  const release = useReleaseMyReservation()
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    if (!data) return
    const id = window.setInterval(() => setNow(Date.now()), 1000)
    return () => window.clearInterval(id)
  }, [data])

  if (!data) return null
  if (location.pathname.startsWith('/checkout')) return null

  const reservedUntilMs = new Date(data.reservedUntil).getTime()
  const msLeft = reservedUntilMs - now
  if (msLeft <= 0) {
    qc.invalidateQueries({ queryKey: qk.reservations.mine })
    return null
  }
  const { minutes, seconds } = formatRemaining(msLeft)

  const handleResume = () => navigate(`/checkout/${data.artworkId}`)
  const handleRelease = (e: React.MouseEvent) => {
    e.stopPropagation()
    if (window.confirm(t('reservation.header.release_confirm', { title: data.title }))) {
      release.mutate()
    }
  }

  return (
    <button
      type="button"
      onClick={handleResume}
      className="flex items-center gap-2 px-3 py-1.5 border border-outline-variant/40 bg-surface-container-lowest text-sm text-on-surface hover:bg-surface-container-low transition-colors"
    >
      {data.coverPublicId ? (
        <img
          src={cloudinaryUrl(data.coverPublicId, { width: 64 })}
          alt=""
          className="size-6 object-cover"
        />
      ) : null}
      <span className="font-medium truncate max-w-[120px]">{data.title}</span>
      <span className="tabular-nums text-on-surface-variant">{minutes}:{seconds}</span>
      <span
        role="button"
        aria-label="Release reservation"
        onClick={handleRelease}
        className="text-on-surface-variant hover:text-on-surface ml-1"
      >
        <X size={14} />
      </span>
    </button>
  )
}
