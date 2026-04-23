import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { signup, isSignupError, type SignupError } from '../api/authApi'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'
import { deriveUsernameFromEmail, storeToken } from '../lib/auth'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuCu_FZrOTTzX5XK4WeCT-aVlozipqUREoyXBxA5nWws63YY8rNsVbPz1NCAOtwKduW0_QEzLi8p9XX7znw-1V0XyRhwF4PpxCMY0mVyilcV-4DmBEUYQVd9c3a_IrAMxQ83RzHyA1036Y8NMzU4av-LYfBL_pi5xovfyk1x6TpPvrL0foUy1iHaaHlFU-QSAvd4v1sU6FInP0ZrSWzPhs8QDeSeanyr-Rox6N1SktzKjx5mUexaemlyoJC8OuSHOsbNs1NIRnhHPFU'

const signupSchema = z
  .object({
    firstName: z
      .string()
      .trim()
      .min(1, { message: 'First name is required' })
      .max(50, { message: 'First name must be 50 characters or fewer' }),
    lastName: z
      .string()
      .trim()
      .min(1, { message: 'Last name is required' })
      .max(50, { message: 'Last name must be 50 characters or fewer' }),
    email: z
      .string()
      .trim()
      .min(1, { message: 'Email is required' })
      .email({ message: 'Enter a valid email address' }),
    password: z
      .string()
      .min(8, { message: 'Password must be at least 8 characters' }),
    confirmPassword: z.string().min(1, { message: 'Please confirm your password' }),
  })
  .refine((data) => data.password === data.confirmPassword, {
    path: ['confirmPassword'],
    message: "Passwords don't match",
  })

type SignupFormValues = z.infer<typeof signupSchema>

function messageFor(err: SignupError): string {
  if (err.kind === 'email_taken') {
    return 'An account with this email already exists.'
  }
  if (err.kind === 'invalid') {
    return 'Please check your details and try again.'
  }
  return 'Something went wrong. Please try again.'
}

export function SignupPage() {
  const navigate = useNavigate()
  const [formError, setFormError] = useState<SignupError | null>(null)
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
      storeToken(response.token)
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
            alt="Abstract minimalist painting in muted earth tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Begin your collection.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Join our community of digital curators and discover exceptional artworks.
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
          <FormField label="First Name" htmlFor="firstName" error={errors.firstName?.message}>
            <Input
              id="firstName"
              type="text"
              autoComplete="given-name"
              placeholder="E.g. Elena"
              disabled={isSubmitting}
              invalid={!!errors.firstName}
              {...register('firstName')}
            />
          </FormField>

          <FormField label="Last Name" htmlFor="lastName" error={errors.lastName?.message}>
            <Input
              id="lastName"
              type="text"
              autoComplete="family-name"
              placeholder="E.g. Rossi"
              disabled={isSubmitting}
              invalid={!!errors.lastName}
              {...register('lastName')}
            />
          </FormField>

          <FormField label="Email Address" htmlFor="email" error={errors.email?.message}>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              placeholder="name@curator.com"
              disabled={isSubmitting}
              invalid={!!errors.email}
              {...register('email')}
            />
          </FormField>

          <FormField label="Password" htmlFor="password" error={errors.password?.message}>
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
            label="Confirm Password"
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
                  Sign in instead?
                </Link>
              ) : null}
            </div>
          ) : null}

          <div className="pt-2">
            <Button type="submit" variant="primary" fullWidth loading={isSubmitting}>
              Create Your Profile
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/login"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            Already have an account? <span className="border-b border-on-surface pb-1">Sign In</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
