import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { z } from 'zod'
import { isLoginError, login, type LoginError } from '../api/authApi'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'
import { storeToken } from '../lib/auth'
import type { TFunction } from 'i18next'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuC7J-zoC-6ecgH_YsQEcGf7NqrVj1tZSo8dlgSiiXmfotRFTMK1xoZPM1vEa9bmhzn26gL_fgiVqOM6nkxxipdoCxbap_10tE-aIgdLD-OgGdoYXDWMwvq6C7BPqCyJ33kxVeEdnzigM3xPmRYmV9NN1rNs0QaC1jnbBllNzeWSvDcUoOQOh41IiFNgO-K2iHpXfzyiNNsuQhtvA3jmNy1bOjRMlg9E23PDZ7JX7ijJdv7jq1RpvDIQhEutYgxG2xdd8SCmrSAVAKo'

const makeLoginSchema = (t: TFunction) =>
  z.object({
    email: z
      .string()
      .trim()
      .min(1, { message: t('auth.validation.email.required') })
      .email({ message: t('auth.validation.email.invalid') }),
    password: z.string().min(1, { message: t('auth.validation.password.required') }),
  })

type LoginFormValues = {
  email: string
  password: string
}

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [formError, setFormError] = useState<LoginError | null>(null)

  const loginSchema = useMemo(() => makeLoginSchema(t), [t])

  function messageFor(err: LoginError): string {
    if (err.kind === 'bad_credentials') {
      return t('auth.login.errorBadCredentials')
    }
    if (err.kind === 'invalid') {
      return t('auth.login.errorInvalid')
    }
    return t('auth.login.errorNetwork')
  }

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    mode: 'onTouched',
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = handleSubmit(async (values) => {
    setFormError(null)
    try {
      const response = await login({
        email: values.email.trim(),
        password: values.password,
      })
      storeToken(response.accessToken)
      navigate('/')
    } catch (error) {
      if (isLoginError(error)) {
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
            alt={t('auth.login.heroAlt')}
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            {t('auth.login.heading')}
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            {t('auth.login.subheading')}
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label={t('auth.login.email')} htmlFor="email" error={errors.email?.message}>
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

          <FormField label={t('auth.login.password')} htmlFor="password" error={errors.password?.message}>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              disabled={isSubmitting}
              invalid={!!errors.password}
              {...register('password')}
            />
          </FormField>

          <div className="flex justify-end pt-1">
            <a
              href="#"
              className="font-label text-[10px] uppercase tracking-widest text-outline hover:text-on-surface transition-colors"
            >
              {t('auth.login.forgotPassword')}
            </a>
          </div>

          {formError ? (
            <div
              role="alert"
              className="font-body text-sm text-on-error-container bg-error-container/30 border border-error/30 p-3"
            >
              {messageFor(formError)}
            </div>
          ) : null}

          <div className="pt-2">
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              {t('auth.login.submit')}
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/signup"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            {t('auth.login.noAccount')}{' '}
            <span className="border-b border-on-surface pb-1">{t('auth.login.signUpLink')}</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
