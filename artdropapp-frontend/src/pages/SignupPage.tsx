import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { signup, isSignupError, type SignupError } from '../api/authApi'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'
import { deriveUsernameFromEmail, storeToken } from '../lib/auth'
import type { TFunction } from 'i18next'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

const makeSignupSchema = (t: TFunction) =>
  z
    .object({
      firstName: z
        .string()
        .trim()
        .min(1, { message: t('auth.validation.firstName.required') })
        .max(50, { message: t('auth.validation.firstName.tooLong') }),
      lastName: z
        .string()
        .trim()
        .min(1, { message: t('auth.validation.lastName.required') })
        .max(50, { message: t('auth.validation.lastName.tooLong') }),
      email: z
        .string()
        .trim()
        .min(1, { message: t('auth.validation.email.required') })
        .email({ message: t('auth.validation.email.invalid') }),
      password: z
        .string()
        .min(8, { message: t('auth.validation.password.tooShort') }),
      confirmPassword: z.string().min(1, { message: t('auth.validation.confirmPassword.required') }),
    })
    .refine((data) => data.password === data.confirmPassword, {
      path: ['confirmPassword'],
      message: t('auth.validation.confirmPassword.mismatch'),
    })

type SignupFormValues = {
  firstName: string
  lastName: string
  email: string
  password: string
  confirmPassword: string
}

export function SignupPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<SignupError | null>(null)

  const signupSchema = useMemo(() => makeSignupSchema(t), [t])

  function messageFor(err: SignupError): string {
    if (err.kind === 'email_taken') {
      return t('auth.signup.errorEmailTaken')
    }
    if (err.kind === 'invalid') {
      return t('auth.signup.errorInvalid')
    }
    return t('auth.signup.errorNetwork')
  }

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({
    mode: 'onTouched',
    resolver: zodResolver(signupSchema),
    defaultValues: {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const response = await signup({
        username: deriveUsernameFromEmail(values.email),
        email: values.email.trim(),
        password: values.password,
        displayName: `${values.firstName.trim()} ${values.lastName.trim()}`,
      })
      storeToken(response.accessToken)
      navigate('/')
    } catch (error) {
      if (isSignupError(error)) {
        setFormError(error)
      } else {
        setFormError({ kind: 'network' })
      }
    }
  })

  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 md:py-12 flex flex-col flex-grow">
        <div className="mb-10 w-full overflow-hidden">
          <img
            className="w-full h-48 md:h-64 object-cover grayscale opacity-90 transition-all duration-700 hover:grayscale-0"
            src={HERO_IMAGE_URL}
            alt={t('auth.signup.heroAlt')}
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            {t('auth.signup.heading')}
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            {t('auth.signup.subheading')}
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label={t('auth.signup.firstName')} htmlFor="firstName" error={errors.firstName?.message}>
            <Input
              id="firstName"
              type="text"
              autoComplete="given-name"
              placeholder={t('auth.signup.firstNamePlaceholder')}
              disabled={isSubmitting}
              invalid={!!errors.firstName}
              {...register('firstName')}
            />
          </FormField>

          <FormField label={t('auth.signup.lastName')} htmlFor="lastName" error={errors.lastName?.message}>
            <Input
              id="lastName"
              type="text"
              autoComplete="family-name"
              placeholder={t('auth.signup.lastNamePlaceholder')}
              disabled={isSubmitting}
              invalid={!!errors.lastName}
              {...register('lastName')}
            />
          </FormField>

          <FormField label={t('auth.signup.email')} htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder={t('auth.login.emailPlaceholder')}
              disabled={isSubmitting}
              invalid={!!errors.email}
              {...register('email')}
            />
          </FormField>

          <FormField label={t('auth.signup.password')} htmlFor="password" error={errors.password?.message}>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.password}
              {...register('password')}
            />
          </FormField>

          <FormField
            label={t('auth.signup.confirmPassword')}
            htmlFor="confirmPassword"
            error={errors.confirmPassword?.message}
          >
            <Input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.confirmPassword}
              {...register('confirmPassword')}
            />
          </FormField>

          {formError ? (
            <div
              role="alert"
              className="font-body text-sm text-on-error-container bg-error-container/30 border border-error/30 p-3"
            >
              {messageFor(formError)}{' '}
              {formError.kind === 'email_taken' ? (
                <Link to="/login" className="underline underline-offset-4 hover:text-on-surface">
                  {t('auth.signup.signInInstead')}
                </Link>
              ) : null}
            </div>
          ) : null}

          <div className="pt-2">
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              {t('auth.signup.submit')}
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/login"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            {t('auth.signup.hasAccount')}{' '}
            <span className="border-b border-on-surface pb-1">{t('auth.signup.signInLink')}</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
