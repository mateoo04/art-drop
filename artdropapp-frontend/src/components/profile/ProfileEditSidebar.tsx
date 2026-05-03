import { zodResolver } from '@hookform/resolvers/zod'
import { X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import { z } from 'zod'
import { updateMe, type UpdateProfilePayload } from '../../api/usersApi'
import type { UserProfile } from '../../types/user'
import { Button } from '../ui/Button'
import { FormField } from '../ui/FormField'
import { Input } from '../ui/Input'

type ProfileEditSidebarProps = {
  open: boolean
  user: UserProfile
  onClose: () => void
  onSaved: (updated: UserProfile) => void
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
    avatarUrl: z
      .string()
      .trim()
      .max(1000)
      .url({ message: t('profile.validation.avatarUrl.invalid') })
      .optional()
      .or(z.literal('')),
  })

type FormValues = z.infer<ReturnType<typeof makeSchema>>

export function ProfileEditSidebar({ open, user, onClose, onSaved }: ProfileEditSidebarProps) {
  const { t } = useTranslation()
  const schema = useMemo(() => makeSchema(t), [t])
  const [submitError, setSubmitError] = useState<string | null>(null)
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
      avatarUrl: user.avatarUrl ?? '',
    },
  })

  useEffect(() => {
    if (open) {
      reset({
        displayName: user.displayName,
        bio: user.bio ?? '',
        avatarUrl: user.avatarUrl ?? '',
      })
      setSubmitError(null)
    }
  }, [open, user, reset])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  const bioValue = watch('bio') ?? ''

  const onSubmit = handleSubmit(async (values) => {
    setSubmitError(null)
    const payload: UpdateProfilePayload = {
      displayName: values.displayName.trim(),
      bio: values.bio && values.bio.length > 0 ? values.bio : null,
      avatarUrl: values.avatarUrl && values.avatarUrl.length > 0 ? values.avatarUrl : null,
    }
    try {
      const updated = await updateMe(payload)
      onSaved(updated)
      onClose()
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : 'Update failed')
    }
  })

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
        className={`absolute top-0 right-0 h-full w-full max-w-md bg-surface shadow-2xl transition-transform duration-300 ease-out overflow-y-auto ${
          open ? 'translate-x-0' : 'translate-x-full'
        }`}
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
          <FormField label={t('profile.editProfile.displayName')} htmlFor="displayName" error={errors.displayName?.message}>
            <Input
              id="displayName"
              type="text"
              disabled={isSubmitting}
              invalid={!!errors.displayName}
              {...register('displayName')}
            />
          </FormField>

          <FormField label={t('profile.editProfile.avatarUrl')} htmlFor="avatarUrl" error={errors.avatarUrl?.message}>
            <Input
              id="avatarUrl"
              type="url"
              placeholder="https://…"
              disabled={isSubmitting}
              invalid={!!errors.avatarUrl}
              {...register('avatarUrl')}
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
