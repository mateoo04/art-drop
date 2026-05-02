import { zodResolver } from '@hookform/resolvers/zod'
import { X } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
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

const schema = z.object({
  displayName: z
    .string()
    .trim()
    .min(2, { message: 'Display name must be at least 2 characters' })
    .max(100, { message: 'Display name must be 100 characters or fewer' }),
  bio: z
    .string()
    .max(1000, { message: 'Bio must be 1000 characters or fewer' })
    .optional()
    .or(z.literal('')),
  avatarUrl: z
    .string()
    .trim()
    .max(1000)
    .url({ message: 'Must be a valid URL' })
    .optional()
    .or(z.literal('')),
})

type FormValues = z.infer<typeof schema>

export function ProfileEditSidebar({ open, user, onClose, onSaved }: ProfileEditSidebarProps) {
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
        aria-label="Edit profile"
        className={`absolute top-0 right-0 h-full w-full max-w-md bg-surface shadow-2xl transition-transform duration-300 ease-out overflow-y-auto ${
          open ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between px-8 py-6 border-b border-outline-variant/15">
          <h2 className="font-headline text-2xl text-on-surface">Edit profile</h2>
          <button
            type="button"
            aria-label="Close edit profile"
            onClick={onClose}
            className="text-on-surface-variant hover:text-on-surface"
          >
            <X size={20} />
          </button>
        </div>

        <form className="px-8 py-8 space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label="Display Name" htmlFor="displayName" error={errors.displayName?.message}>
            <Input
              id="displayName"
              type="text"
              disabled={isSubmitting}
              invalid={!!errors.displayName}
              {...register('displayName')}
            />
          </FormField>

          <FormField label="Avatar URL" htmlFor="avatarUrl" error={errors.avatarUrl?.message}>
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
              Bio
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
              Cancel
            </Button>
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              Save
            </Button>
          </div>
        </form>
      </aside>
    </div>
  )
}
