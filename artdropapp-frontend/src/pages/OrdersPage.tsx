import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { fetchMyOrders } from '../api/ordersApi'
import type { Order, OrderStatus } from '../types/order'
import { Spinner } from '../components/ui/Spinner'
import { cloudinaryUrl } from '../lib/cloudinary'

function statusBadgeClass(status: OrderStatus): string {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'bg-surface-container text-on-surface-variant'
    case 'PAID':
      return 'bg-tertiary text-on-tertiary'
    case 'SHIPPED':
      return 'bg-primary text-on-primary'
    case 'DELIVERED':
      return 'bg-secondary text-on-secondary'
    case 'CANCELLED':
    case 'REFUNDED':
      return 'bg-inverse-surface text-inverse-on-surface'
  }
}

function formatCurrency(amount: number, currency: string): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(amount)
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleDateString()
  } catch {
    return iso
  }
}

export function OrdersPage() {
  const { t } = useTranslation()
  const [orders, setOrders] = useState<Order[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    fetchMyOrders()
      .then((data) => {
        if (!cancelled) setOrders(data)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load orders')
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main className="w-full max-w-[1100px] mx-auto px-6 md:px-12 py-16">
      <h1 className="font-display text-4xl md:text-5xl text-on-surface mb-12 tracking-tight">
        {t('orders.title')}
      </h1>

      {orders == null && error == null ? (
        <div className="py-12 flex justify-center"><Spinner /></div>
      ) : null}
      {error ? <p className="text-error" role="alert">{error}</p> : null}
      {orders != null && orders.length === 0 ? (
        <p className="font-body text-on-surface-variant">{t('orders.empty')}</p>
      ) : null}

      {orders != null && orders.length > 0 ? (
        <ul className="divide-y divide-outline-variant/20">
          {orders.map((o) => (
            <li key={o.id}>
              <Link
                to={`/orders/${o.id}`}
                className="grid grid-cols-[64px_1fr_auto] gap-4 items-center py-4 hover:bg-surface-container-lowest transition-colors"
              >
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
                    {formatDate(o.createdAt)} · {formatCurrency(o.total, o.currency)}
                  </p>
                </div>
                <span
                  className={`${statusBadgeClass(o.status)} px-3 py-1 text-[10px] font-label uppercase tracking-widest`}
                >
                  {t(`orders.status.${o.status}`)}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </main>
  )
}
