import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { isLoginError, login, type LoginError } from '../api/authApi'
import { AuthHeader } from '../components/layout/AuthHeader'
import { Button } from '../components/ui/Button'
import { FormField } from '../components/ui/FormField'
import { Input } from '../components/ui/Input'
import { storeToken } from '../lib/auth'

const HERO_IMAGE_URL =
  'https://lh3.googleusercontent.com/aida-public/AB6AXuC7J-zoC-6ecgH_YsQEcGf7NqrVj1tZSo8dlgSiiXmfotRFTMK1xoZPM1vEa9bmhzn26gL_fgiVqOM6nkxxipdoCxbap_10tE-aIgdLD-OgGdoYXDWMwvq6C7BPqCyJ33kxVeEdnzigM3xPmRYmV9NN1rNs0QaC1jnbBllNzeWSvDcUoOQOh41IiFNgO-K2iHpXfzyiNNsuQhtvA3jmNy1bOjRMlg9E23PDZ7JX7ijJdv7jq1RpvDIQhEutYgxG2xdd8SCmrSAVAKo'

const loginSchema = z.object({
  email: z
    .string()
    .trim()
    .min(1, { message: 'Email is required' })
    .email({ message: 'Enter a valid email address' }),
  password: z.string().min(1, { message: 'Password is required' }),
})

type LoginFormValues = z.infer<typeof loginSchema>

function messageFor(err: LoginError): string {
  if (err.kind === 'bad_credentials') {
    return 'Incorrect email or password.'
  }
  if (err.kind === 'invalid') {
    return 'Please check your details and try again.'
  }
  return 'Something went wrong. Please try again.'
}

export function LoginPage() {
  const navigate = useNavigate()
  const [formError, setFormError] = useState<LoginError | null>(null)
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
            alt="Abstract minimalist digital painting in muted charcoal tones"
          />
        </div>

        <section className="mb-8">
          <h2 className="font-headline text-4xl font-light mb-2 tracking-tight">
            Welcome back.
          </h2>
          <p className="font-body text-sm text-on-surface-variant leading-relaxed max-w-[280px]">
            Continue your journey through the world of digital art.
          </p>
        </section>

        <form className="space-y-6" onSubmit={onSubmit} noValidate>
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
              Forgot Password?
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
              Log In to Your Profile
            </Button>
          </div>
        </form>

        <footer className="mt-auto pt-16 pb-10 text-center">
          <Link
            to="/signup"
            className="font-label text-[11px] uppercase tracking-[0.15em] text-on-surface hover:text-outline transition-colors duration-300"
          >
            Don't have an account? <span className="border-b border-on-surface pb-1">Sign Up</span>
          </Link>
        </footer>
      </main>
    </div>
  )
}
