import { type FormEvent, type ReactNode, useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronDown } from 'lucide-react'
import { useQueryClient } from '@tanstack/react-query'
import { qk } from '../lib/queryKeys'
import { PendingOrderConflictModal } from '../components/checkout/PendingOrderConflictModal'
import { fetchArtworkById } from '../api/artworksApi'
import { fetchMyAddresses } from '../api/addressesApi'
import { CheckoutError, createCheckoutSession, type PendingOrderConflict } from '../api/checkoutApi'
import { cancelOrder } from '../api/ordersApi'
import type { Artwork } from '../types/artwork'
import type { ShippingAddress } from '../types/address'
import { Spinner } from '../components/ui/Spinner'
import { cloudinaryUrl } from '../lib/cloudinary'
import { listCountries } from '../lib/countries'

type AddressForm = {
  recipientName: string
  line1: string
  line2: string
  city: string
  postalCode: string
  country: string
  phone: string
}

const EMPTY_FORM: AddressForm = {
  recipientName: '',
  line1: '',
  line2: '',
  city: '',
  postalCode: '',
  country: '',
  phone: '',
}

function formatCurrency(amount: number, currency = 'EUR'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function CheckoutPage() {
  const { t } = useTranslation()
  const { artworkId: idParam } = useParams<{ artworkId: string }>()
  const artworkId = idParam ? Number.parseInt(idParam, 10) : Number.NaN
  const [searchParams] = useSearchParams()
  const initialQty = Number.parseInt(searchParams.get('qty') ?? '1', 10) || 1
  const navigate = useNavigate()

  const [artwork, setArtwork] = useState<Artwork | null>(null)
  const [addresses, setAddresses] = useState<ShippingAddress[]>([])
  const [selectedAddressId, setSelectedAddressId] = useState<number | 'new'>('new')
  const [form, setForm] = useState<AddressForm>(EMPTY_FORM)
  const [quantity, setQuantity] = useState(initialQty)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const qc = useQueryClient()
  const [pendingConflict, setPendingConflict] = useState<PendingOrderConflict | null>(null)
  const [conflictResolving, setConflictResolving] = useState(false)

  useEffect(() => {
    let cancelled = false
    Promise.all([fetchArtworkById(artworkId), fetchMyAddresses()])
      .then(([a, addrs]) => {
        if (cancelled) return
        setArtwork(a)
        setAddresses(addrs)
        if (addrs.length > 0) setSelectedAddressId(addrs[0].id)
      })
      .catch((err: unknown) => {
        if (!cancelled) setError(err instanceof Error ? err.message : 'Failed to load checkout')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [artworkId])

  function setFormField(field: keyof AddressForm, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (artwork == null) return
    setError(null)
    setSubmitting(true)
    const useExisting = selectedAddressId !== 'new'
    if (!useExisting) {
      if (!form.recipientName || !form.line1 || !form.city || !form.postalCode || !form.country) {
        setError(t('checkout.errors.address_incomplete'))
        setSubmitting(false)
        return
      }
    }
    await runCheckout()
  }

  async function runCheckout() {
    if (artwork == null) return
    const useExisting = selectedAddressId !== 'new'
    try {
      const { checkoutUrl } = await createCheckoutSession({
        artworkId: artwork.id,
        quantity,
        addressId: useExisting ? (selectedAddressId as number) : null,
        inlineAddress: useExisting
          ? null
          : {
              recipientName: form.recipientName.trim(),
              line1: form.line1.trim(),
              line2: form.line2.trim() || null,
              city: form.city.trim(),
              postalCode: form.postalCode.trim(),
              country: form.country,
              phone: form.phone.trim() || null,
            },
      })
      qc.invalidateQueries({ queryKey: qk.reservations.mine })
      window.location.assign(checkoutUrl)
    } catch (err) {
      if (err instanceof CheckoutError) {
        if (err.kind === 'PENDING_ORDER_EXISTS' && err.existingOrder) {
          setPendingConflict(err.existingOrder)
          setSubmitting(false)
          return
        }
        if (err.kind === 'SELF_PURCHASE') setError(t('checkout.errors.self_purchase'))
        else if (err.kind === 'INVENTORY_UNAVAILABLE') setError(t('checkout.errors.unavailable'))
        else setError(err.message)
      } else {
        setError(err instanceof Error ? err.message : 'Checkout failed')
      }
      setSubmitting(false)
    }
  }

  if (loading) {
    return <div className="py-24 flex justify-center"><Spinner /></div>
  }
  if (artwork == null) {
    return (
      <main className="w-full max-w-[800px] mx-auto px-6 py-16">
        <p className="text-error" role="alert">{error ?? t('checkout.errors.artwork_not_found')}</p>
      </main>
    )
  }

  const unitPrice = artwork.price ?? 0
  const isEdition = artwork.saleType === 'EDITION'
  const remaining = artwork.editionRemaining ?? Infinity

  return (
    <main className="w-full max-w-[900px] mx-auto px-6 py-16">
      <h1 className="font-display text-4xl md:text-5xl text-on-surface mb-12 tracking-tight">
        {t('checkout.title')}
      </h1>

      <div className="grid grid-cols-1 md:grid-cols-[1fr_320px] gap-12">
        <form className="space-y-10" onSubmit={(e) => void handleSubmit(e)}>
          <section>
            <h2 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-4">
              {t('checkout.shipping_address')}
            </h2>
            {addresses.length > 0 ? (
              <div className="space-y-2 mb-4">
                {addresses.map((a) => (
                  <label
                    key={a.id}
                    className="flex items-start gap-3 p-3 border border-outline-variant/30 cursor-pointer"
                  >
                    <input
                      type="radio"
                      name="address"
                      checked={selectedAddressId === a.id}
                      onChange={() => setSelectedAddressId(a.id)}
                    />
                    <span className="text-sm">
                      {a.recipientName}, {a.line1}
                      {a.line2 ? `, ${a.line2}` : ''}, {a.postalCode} {a.city}, {a.country}
                    </span>
                  </label>
                ))}
                <label className="flex items-center gap-3 p-3 border border-outline-variant/30 cursor-pointer">
                  <input
                    type="radio"
                    name="address"
                    checked={selectedAddressId === 'new'}
                    onChange={() => setSelectedAddressId('new')}
                  />
                  <span className="text-sm">{t('checkout.use_new_address')}</span>
                </label>
              </div>
            ) : null}

            {selectedAddressId === 'new' ? (
              <div className="space-y-4">
                <Field label={t('checkout.fields.recipient_name')}>
                  <input className={inputCls} value={form.recipientName} onChange={(e) => setFormField('recipientName', e.target.value)} />
                </Field>
                <Field label={t('checkout.fields.line1')}>
                  <input className={inputCls} value={form.line1} onChange={(e) => setFormField('line1', e.target.value)} />
                </Field>
                <Field label={t('checkout.fields.line2')}>
                  <input className={inputCls} value={form.line2} onChange={(e) => setFormField('line2', e.target.value)} />
                </Field>
                <div className="grid grid-cols-2 gap-3">
                  <Field label={t('checkout.fields.city')}>
                    <input className={inputCls} value={form.city} onChange={(e) => setFormField('city', e.target.value)} />
                  </Field>
                  <Field label={t('checkout.fields.postal_code')}>
                    <input className={inputCls} value={form.postalCode} onChange={(e) => setFormField('postalCode', e.target.value)} />
                  </Field>
                </div>
                <Field label={t('checkout.fields.country')}>
                  <div className="relative">
                    <select
                      className={`${inputCls} appearance-none pr-10`}
                      value={form.country}
                      onChange={(e) => setFormField('country', e.target.value)}
                    >
                      <option value="">{t('checkout.fields.country_placeholder')}</option>
                      {listCountries().map((c) => (
                        <option key={c.code} value={c.code}>{c.name}</option>
                      ))}
                    </select>
                    <ChevronDown className="pointer-events-none absolute right-4 top-1/2 -translate-y-1/2 size-4 text-on-surface-variant" />
                  </div>
                </Field>
                <Field label={t('checkout.fields.phone')}>
                  <input className={inputCls} value={form.phone} onChange={(e) => setFormField('phone', e.target.value)} />
                </Field>
              </div>
            ) : null}
          </section>

          {isEdition ? (
            <section>
              <h2 className="font-label text-xs uppercase tracking-[0.15em] text-on-surface-variant mb-4">
                {t('checkout.quantity')}
              </h2>
              <input
                type="number"
                min={1}
                max={Number.isFinite(remaining) ? remaining : undefined}
                value={quantity}
                onChange={(e) => setQuantity(Math.max(1, Number.parseInt(e.target.value, 10) || 1))}
                className={`${inputCls} w-32`}
              />
            </section>
          ) : null}

          {error ? <p className="text-error text-sm" role="alert">{error}</p> : null}

          <div className="flex gap-3">
            <button
              type="button"
              onClick={() => navigate(-1)}
              className="border border-outline-variant/40 px-6 py-3 font-label text-xs uppercase tracking-widest"
            >
              {t('common.cancel')}
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 bg-on-surface text-surface px-6 py-3 font-label text-sm uppercase tracking-widest disabled:opacity-60"
            >
              {submitting ? t('checkout.redirecting') : t('checkout.continue')}
            </button>
          </div>
        </form>

        <aside className="bg-surface-container-low p-6 h-fit">
          {artwork.coverPublicId ? (
            <img
              src={cloudinaryUrl(artwork.coverPublicId, { width: 400 })}
              alt={artwork.imageAlt}
              className="w-full mb-4"
            />
          ) : null}
          <p className="font-display text-lg text-on-surface mb-1">{artwork.title}</p>
          <p className="font-body text-sm text-on-surface-variant mb-6">
            {artwork.artist?.displayName ?? ''}
          </p>
          <dl className="space-y-2 text-sm border-t border-outline-variant/20 pt-4">
            <div className="flex justify-between">
              <dt>{t('checkout.summary.subtotal', { qty: quantity })}</dt>
              <dd>{formatCurrency(unitPrice * quantity)}</dd>
            </div>
            <div className="flex justify-between">
              <dt>{t('checkout.summary.shipping')}</dt>
              <dd>{formatCurrency(10)}</dd>
            </div>
            <div className="flex justify-between font-display text-lg pt-2 border-t border-outline-variant/20">
              <dt>{t('checkout.summary.total')}</dt>
              <dd>{formatCurrency(unitPrice * quantity + 10)}</dd>
            </div>
          </dl>
        </aside>
      </div>
      {pendingConflict ? (
        <PendingOrderConflictModal
          existingTitle={pendingConflict.artworkTitle}
          incomingTitle={artwork.title}
          sameArtwork={pendingConflict.artworkId === artwork.id}
          isPending={conflictResolving}
          onCancel={() => setPendingConflict(null)}
          onConfirm={async () => {
            setConflictResolving(true)
            setSubmitting(true)
            try {
              await cancelOrder(pendingConflict.id, null)
              qc.invalidateQueries({ queryKey: qk.reservations.mine })
              setPendingConflict(null)
              await runCheckout()
            } catch (err) {
              setError(err instanceof Error ? err.message : 'Checkout failed')
              setSubmitting(false)
              setPendingConflict(null)
            } finally {
              setConflictResolving(false)
            }
          }}
        />
      ) : null}
    </main>
  )
}

const inputCls =
  'w-full border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-sm text-on-surface focus:outline-none focus:border-primary'

function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant mb-1.5">
        {label}
      </span>
      {children}
      {hint ? <span className="block text-[11px] text-on-surface-variant mt-1">{hint}</span> : null}
    </label>
  )
}
