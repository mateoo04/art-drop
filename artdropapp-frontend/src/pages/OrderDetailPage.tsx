import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { cancelOrder, fetchOrder } from '../api/ordersApi'
import type { Order, OrderStatus } from '../types/order'
import { Spinner } from '../components/ui/Spinner'
import { cloudinaryUrl } from '../lib/cloudinary'

const POLL_INTERVAL_MS = 1500
const POLL_MAX_ATTEMPTS = 5

function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(amount)
}

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

export function OrderDetailPage() {
  const { t } = useTranslation()
  const { id: idParam } = useParams<{ id: string }>()
  const id = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const [searchParams] = useSearchParams()
  const sessionStatus = searchParams.get('status')

  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [cancelling, setCancelling] = useState(false)
  const [polling, setPolling] = useState(false)

  const reload = useCallback(async (): Promise<Order | null> => {
    if (!Number.isFinite(id)) return null
    try {
      const o = await fetchOrder(id)
      if (o == null) {
        setError(t('orders.detail.not_found'))
        return null
      }
      setOrder(o)
      return o
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load order')
      return null
    } finally {
      setLoading(false)
    }
  }, [id, t])

  useEffect(() => {
    void reload()
  }, [reload])

  // On redirect-back with status=success, poll until webhook flips order to PAID.
  useEffect(() => {
    if (sessionStatus !== 'success') return
    if (order == null || order.status !== 'PENDING_PAYMENT') return
    setPolling(true)
    let attempts = 0
    const timer = window.setInterval(() => {
      attempts++
      void reload().then((updated) => {
        if (updated && updated.status !== 'PENDING_PAYMENT') {
          window.clearInterval(timer)
          setPolling(false)
        } else if (attempts >= POLL_MAX_ATTEMPTS) {
          window.clearInterval(timer)
          setPolling(false)
        }
      })
    }, POLL_INTERVAL_MS)
    return () => {
      window.clearInterval(timer)
      setPolling(false)
    }
  }, [sessionStatus, order, reload])

  async function handleCancel() {
    if (order == null) return
    setCancelling(true)
    try {
      const updated = await cancelOrder(order.id, null)
      setOrder(updated)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Cancel failed')
    } finally {
      setCancelling(false)
    }
  }

  if (loading) {
    return <div className="py-24 flex justify-center"><Spinner /></div>
  }
  if (error) {
    return (
      <main className="w-full max-w-[800px] mx-auto px-6 py-16">
        <p className="text-error" role="alert">{error}</p>
      </main>
    )
  }
  if (!order) return null

  return (
    <main className="w-full max-w-[800px] mx-auto px-6 py-16">
      {sessionStatus === 'cancelled' && order.status === 'PENDING_PAYMENT' ? (
        <div className="mb-8 border border-outline-variant/30 bg-surface-container-low p-4 text-sm text-on-surface-variant">
          {t('orders.detail.cancelled_at_stripe')}
        </div>
      ) : null}
      {polling ? (
        <div className="mb-8 border border-outline-variant/30 bg-surface-container-low p-4 text-sm text-on-surface-variant flex items-center gap-3">
          <Spinner />
          <span>{t('orders.detail.confirming')}</span>
        </div>
      ) : null}

      <header className="mb-10">
        <p className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-2">
          {t('orders.detail.order_id', { id: order.id })}
        </p>
        <h1 className="font-display text-4xl text-on-surface tracking-tight mb-3">
          {order.artworkTitle ?? '—'}
        </h1>
        <span className={`${badge(order.status)} inline-block px-3 py-1 text-[10px] font-label uppercase tracking-widest`}>
          {t(`orders.status.${order.status}`)}
        </span>
      </header>

      <div className="grid grid-cols-1 md:grid-cols-[160px_1fr] gap-6 mb-12">
        {order.artworkCoverPublicId ? (
          <Link
            to={`/details/${order.artworkId}`}
            aria-label={t('orders.detail.view_artwork')}
            className="block w-40 h-40 overflow-hidden bg-surface-container-low hover:opacity-90 transition-opacity"
          >
            <img
              src={cloudinaryUrl(order.artworkCoverPublicId, { width: 320 })}
              alt=""
              className="w-full h-full object-cover"
            />
          </Link>
        ) : (
          <div className="w-40 h-40 bg-surface-container-low" />
        )}
        <dl className="space-y-2 text-sm">
          <div className="flex justify-between">
            <dt className="text-on-surface-variant">{t('orders.detail.subtotal')}</dt>
            <dd>{formatCurrency(order.subtotal, order.currency)}</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-on-surface-variant">{t('orders.detail.shipping')}</dt>
            <dd>{formatCurrency(order.shippingFee, order.currency)}</dd>
          </div>
          <div className="flex justify-between font-display text-lg pt-2 border-t border-outline-variant/20">
            <dt>{t('orders.detail.total')}</dt>
            <dd>{formatCurrency(order.total, order.currency)}</dd>
          </div>
        </dl>
      </div>

      <section className="mb-12">
        <h2 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-3">
          {t('orders.detail.shipping_to')}
        </h2>
        <p className="font-body text-sm text-on-surface leading-relaxed">
          {order.shippingAddress.recipientName}<br />
          {order.shippingAddress.line1}<br />
          {order.shippingAddress.line2 ? <>{order.shippingAddress.line2}<br /></> : null}
          {order.shippingAddress.postalCode} {order.shippingAddress.city}<br />
          {order.shippingAddress.country}
          {order.shippingAddress.phone ? <><br />{order.shippingAddress.phone}</> : null}
        </p>
      </section>

      {order.trackingNumber ? (
        <section className="mb-12">
          <h2 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-3">
            {t('orders.detail.tracking')}
          </h2>
          <p className="font-body text-sm text-on-surface">
            {order.shippingCarrier ?? ''} <span className="font-mono">{order.trackingNumber}</span>
          </p>
        </section>
      ) : null}

      {order.status === 'PAID' ? (
        <button
          type="button"
          disabled={cancelling}
          onClick={() => void handleCancel()}
          className="bg-error text-on-error px-6 py-3 font-label text-xs uppercase tracking-widest disabled:opacity-60"
        >
          {cancelling ? t('orders.detail.cancelling') : t('orders.detail.cancel_button')}
        </button>
      ) : null}
    </main>
  )
}
