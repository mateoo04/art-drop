import { AuthHeader } from '../components/layout/AuthHeader'

export function LoginPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-12 flex flex-col flex-grow">
        <h2 className="font-headline text-4xl font-light tracking-tight mb-2">Log in</h2>
        <p className="font-body text-sm text-on-surface-variant">Login coming soon.</p>
      </main>
    </div>
  )
}
