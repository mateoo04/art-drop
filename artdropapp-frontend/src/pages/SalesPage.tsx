import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { deliverOrder, fetchMySales, shipOrder } from '../api/ordersApi'
import type { Order, OrderStatus } from '../types/order'
import { Spinner } from '../components/ui/Spinner'
import { cloudinaryUrl } from '../lib/cloudinary'

function badge(status: OrderStatus): string {
  switch (status) {
    case 'PENDING_PAYMENT': return 'bg-surface-container text-on-surface-variant'
    case 'PAID':            return 'bg-tertiary text-on-tertiary'
    case 'SHIPPED':         return 'bg-primary text-on-primary'
    case 'DELIVERED':       return 'bg-secondary text-on-secondary'
    case 'CANCELLED':
    case 'REFUNDED':        return 'bg-inverse-surface text-inverse-on-surface'
  }
}

export function SalesPage() {
  const { t } = useTranslation()
  const [sales, setSales] = useState<Order[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [trackInputs, setTrackInputs] = useState<Record<number, { tracking: string; carrier: string }>>({})

  function loadSales() {
    fetchMySales()
      .then(setSales)
      .catch((err: unknown) =>
        setError(err instanceof Error ? err.message : 'Failed to load sales'))
  }

  useEffect(() => {
    loadSales()
  }, [])

  async function handleShip(o: Order) {
    setBusyId(o.id)
    try {
      const input = trackInputs[o.id] ?? { tracking: '', carrier: '' }
      const updated = await shipOrder(o.id, input.tracking || null, input.carrier || null)
      setSales((prev) => prev?.map((x) => (x.id === o.id ? updated : x)) ?? null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Ship failed')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDeliver(o: Order) {
    setBusyId(o.id)
    try {
      const updated = await deliverOrder(o.id)
      setSales((prev) => prev?.map((x) => (x.id === o.id ? updated : x)) ?? null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Deliver failed')
    } finally {
      setBusyId(null)
    }
  }

  function setTrackField(id: number, field: 'tracking' | 'carrier', value: string) {
    setTrackInputs((prev) => ({
      ...prev,
      [id]: { ...(prev[id] ?? { tracking: '', carrier: '' }), [field]: value },
    }))
  }

  return (
    <main className="w-full max-w-[1100px] mx-auto px-6 md:px-12 py-16">
      <h1 className="font-display text-4xl md:text-5xl text-on-surface mb-12 tracking-tight">
        {t('sales.title')}
      </h1>

      {sales == null && error == null ? (
        <div className="py-12 flex justify-center"><Spinner /></div>
      ) : null}
      {error ? <p className="text-error mb-6" role="alert">{error}</p> : null}
      {sales != null && sales.length === 0 ? (
        <p className="font-body text-on-surface-variant">{t('sales.empty')}</p>
      ) : null}

      {sales != null && sales.length > 0 ? (
        <ul className="space-y-6">
          {sales.map((o) => (
            <li key={o.id} className="border border-outline-variant/20 p-5 md:p-6">
              <div className="grid grid-cols-[64px_1fr_auto] gap-4 items-start">
                <div className="w-16 h-16 bg-surface-container-low overflow-hidden">
                  {o.artworkCoverPublicId ? (
                    <img
                      src={cloudinaryUrl(o.artworkCoverPublicId, { width: 200 })}
                      alt=""
                      className="w-full h-full object-cover"
                    />
                  ) : null}
                </div>
                <div className="min-w-0">
                  <p className="font-display text-lg text-on-surface truncate">
                    {o.artworkTitle ?? `#${o.id}`}
                  </p>
                  <p className="font-label text-xs text-on-surface-variant uppercase tracking-widest mt-1">
                    {t('sales.order_id', { id: o.id })} · {t('sales.buyer', { uid: o.buyerUserId })}
                  </p>
                  <p className="font-body text-sm text-on-surface mt-2">
                    {o.shippingAddress.recipientName} — {o.shippingAddress.line1}
                    {o.shippingAddress.line2 ? `, ${o.shippingAddress.line2}` : ''}, {o.shippingAddress.postalCode} {o.shippingAddress.city}, {o.shippingAddress.country}
                  </p>
                </div>
                <span className={`${badge(o.status)} px-3 py-1 text-[10px] font-label uppercase tracking-widest`}>
                  {t(`orders.status.${o.status}`)}
                </span>
              </div>

              {o.status === 'PAID' ? (
                <div className="mt-4 flex flex-wrap gap-3 items-end">
                  <label className="flex flex-col text-xs text-on-surface-variant">
                    {t('sales.tracking_number')}
                    <input
                      type="text"
                      value={trackInputs[o.id]?.tracking ?? ''}
                      onChange={(ev) => setTrackField(o.id, 'tracking', ev.target.value)}
                      className="border border-outline-variant/30 px-3 py-2 text-sm text-on-surface w-48 mt-1"
                    />
                  </label>
                  <label className="flex flex-col text-xs text-on-surface-variant">
                    {t('sales.carrier')}
                    <input
                      type="text"
                      value={trackInputs[o.id]?.carrier ?? ''}
                      onChange={(ev) => setTrackField(o.id, 'carrier', ev.target.value)}
                      className="border border-outline-variant/30 px-3 py-2 text-sm text-on-surface w-32 mt-1"
                    />
                  </label>
                  <button
                    type="button"
                    disabled={busyId === o.id}
                    onClick={() => void handleShip(o)}
                    className="bg-on-surface text-surface px-5 py-2 font-label text-xs uppercase tracking-widest disabled:opacity-60"
                  >
                    {t('sales.mark_shipped')}
                  </button>
                </div>
              ) : null}
              {o.status === 'SHIPPED' ? (
                <div className="mt-4 flex flex-wrap gap-3 items-center">
                  {o.trackingNumber ? (
                    <span className="font-label text-xs text-on-surface-variant uppercase tracking-widest">
                      {o.shippingCarrier ?? ''} {o.trackingNumber}
                    </span>
                  ) : null}
                  <button
                    type="button"
                    disabled={busyId === o.id}
                    onClick={() => void handleDeliver(o)}
                    className="bg-on-surface text-surface px-5 py-2 font-label text-xs uppercase tracking-widest disabled:opacity-60"
                  >
                    {t('sales.mark_delivered')}
                  </button>
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  )
}
