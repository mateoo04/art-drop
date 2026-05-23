import { zodResolver } from '@hookform/resolvers/zod'
import { Trash2, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { z } from 'zod'
import { updateMe, type UpdateProfilePayload } from '../../api/usersApi'
import { openCloudinaryUpload } from '../../lib/cloudinary'
import type { UserProfile } from '../../types/user'
import { Button } from '../ui/Button'
import { FormField } from '../ui/FormField'
import { Input } from '../ui/Input'

type ProfileEditSidebarProps = {
  open: boolean
  user: UserProfile
  onClose: () => void
  onSaved: (updated: UserProfile) => void
  focusAvatar?: boolean
}

const makeSchema = (t: TFunction) =>
  z.object({
    displayName: z
      .string()
      .trim()
      .min(2, { message: t('profile.validation.displayName.tooShort') })
      .max(100, { message: t('profile.validation.displayName.tooLong') }),
    bio: z
      .string()
      .max(1000, { message: t('profile.validation.bio.tooLong') })
      .optional()
      .or(z.literal('')),
  })

type FormValues = z.infer<ReturnType<typeof makeSchema>>

export function ProfileEditSidebar({
  open,
  user,
  onClose,
  onSaved,
  focusAvatar,
}: ProfileEditSidebarProps) {
  const { t } = useTranslation()
  const schema = useMemo(() => makeSchema(t), [t])
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [avatarUrl, setAvatarUrl] = useState<string | null>(user.avatarUrl ?? null)
  const [avatarUploading, setAvatarUploading] = useState(false)
  const [avatarError, setAvatarError] = useState<string | null>(null)
  const avatarBlockRef = useRef<HTMLDivElement>(null)
  const {
    register,
    handleSubmit,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    mode: 'onTouched',
    resolver: zodResolver(schema),
    defaultValues: {
      displayName: user.displayName,
      bio: user.bio ?? '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        displayName: user.displayName,
        bio: user.bio ?? '',
      })
      setAvatarUrl(user.avatarUrl ?? null)
      setSubmitError(null)
      setAvatarError(null)
    }
  }, [open, user, reset])

  useEffect(() => {
    if (open && focusAvatar) {
      avatarBlockRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, [open, focusAvatar])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const bioValue = watch('bio') ?? ''

  const onChangePhoto = async () => {
    setAvatarError(null)
    setAvatarUploading(true)
    try {
      const results = await openCloudinaryUpload({ multiple: false })
      const first = results[0]
      if (first?.url) {
        setAvatarUrl(first.url)
      }
    } catch (e) {
      setAvatarError(e instanceof Error ? e.message : t('profile.editProfile.uploadError'))
    } finally {
      setAvatarUploading(false)
    }
  }

  const onRemovePhoto = () => {
    setAvatarError(null)
    setAvatarUrl(null)
  }

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    const payload: UpdateProfilePayload = {
      displayName: values.displayName.trim(),
      bio: values.bio && values.bio.length > 0 ? values.bio : null,
      avatarUrl: avatarUrl ?? '',
    }
    try {
      const updated = await updateMe(payload)
      onSaved(updated)
      onClose()
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : 'Update failed')
    }
  })

  const initial = (user.displayName || '?').slice(0, 1).toUpperCase()

  return (
    <div
      className={`fixed inset-0 z-[60] ${open ? '' : 'pointer-events-none'}`}
      aria-hidden={!open}
    >
      <div
        className={`absolute inset-0 bg-on-surface/30 transition-opacity duration-300 ${
          open ? 'opacity-100' : 'opacity-0'
        }`}
        onClick={onClose}
      />
      <aside
        role="dialog"
        aria-modal="true"
        aria-label={t('profile.editProfile.ariaLabel')}
        className={`absolute top-0 right-0 h-full w-full max-w-md bg-surface transition-[transform,box-shadow] duration-300 ease-out overflow-y-auto ${
          open ? 'translate-x-0' : 'translate-x-full'
        } ${open ? 'shadow-none sm:shadow-2xl' : 'shadow-none'}`}
      >
        <div className="flex items-center justify-between px-8 py-6 border-b border-outline-variant/15">
          <h2 className="font-headline text-2xl text-on-surface">{t('profile.editProfile.title')}</h2>
          <button
            type="button"
            aria-label={t('profile.editProfile.closeLabel')}
            onClick={onClose}
            className="text-on-surface-variant hover:text-on-surface"
          >
            <X size={20} />
          </button>
        </div>

        <form className="px-8 py-8 space-y-6" onSubmit={onSubmit} noValidate>
          <div ref={avatarBlockRef} className="space-y-3">
            <label className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant">
              {t('profile.editProfile.avatar')}
            </label>
            <div className="flex items-center gap-4">
              {avatarUrl ? (
                <img
                  src={avatarUrl}
                  alt={user.displayName}
                  className="w-20 h-20 rounded-full object-cover bg-surface-container-low"
                />
              ) : (
                <div className="w-20 h-20 rounded-full bg-surface-container-low flex items-center justify-center font-headline text-2xl text-on-surface">
                  {initial}
                </div>
              )}
              <div className="flex flex-col gap-2">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={onChangePhoto}
                  disabled={isSubmitting || avatarUploading}
                  loading={avatarUploading}
                >
                  {t('profile.editProfile.changePhoto')}
                </Button>
                {avatarUrl ? (
                  <button
                    type="button"
                    onClick={onRemovePhoto}
                    disabled={isSubmitting || avatarUploading}
                    className="inline-flex items-center gap-1.5 font-label text-[11px] uppercase tracking-[0.15em] text-on-surface-variant hover:text-error transition-colors disabled:opacity-60 self-start"
                  >
                    <Trash2 size={14} />
                    {t('profile.editProfile.removePhoto')}
                  </button>
                ) : null}
              </div>
            </div>
            {avatarError ? (
              <p role="alert" className="font-body text-xs text-error">
                {avatarError}
              </p>
            ) : null}
          </div>

          <FormField label={t('profile.editProfile.displayName')} htmlFor="displayName" error={errors.displayName?.message}>
            <Input
              id="displayName"
              type="text"
              disabled={isSubmitting}
              invalid={!!errors.displayName}
              {...register('displayName')}
            />
          </FormField>

          <div className="space-y-1.5">
            <label
              htmlFor="bio"
              className="block font-label text-[10px] uppercase tracking-[0.15em] text-on-surface-variant"
            >
              {t('profile.editProfile.bio')}
            </label>
            <textarea
              id="bio"
              rows={6}
              disabled={isSubmitting}
              aria-invalid={errors.bio ? true : undefined}
              className={`w-full bg-surface-container-lowest p-4 font-body text-sm rounded-none transition-all duration-300 focus:outline-none disabled:opacity-60 border ${
                errors.bio
                  ? 'border-error focus:border-error'
                  : 'border-outline-variant/15 focus:border-on-surface'
              }`}
              {...register('bio')}
            />
            <div className="flex justify-between items-center">
              {errors.bio ? (
                <p role="alert" className="font-body text-xs text-error">
                  {errors.bio.message}
                </p>
              ) : (
                <span />
              )}
              <span className="font-label text-[10px] text-on-surface-variant">
                {bioValue.length}/1000
              </span>
            </div>
          </div>

          {submitError ? (
            <div
              role="alert"
              className="font-body text-sm text-on-error-container bg-error-container/30 border border-error/30 p-3"
            >
              {submitError}
            </div>
          ) : null}

          <div className="flex gap-3 pt-2">
            <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              {t('common.save')}
            </Button>
          </div>
        </form>
      </aside>
    </div>
  )
}
