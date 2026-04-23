import { AuthHeader } from '../components/layout/AuthHeader'

export function SignupPage() {
  return (
    <div className="min-h-screen flex flex-col bg-surface text-on-surface">
      <AuthHeader />
      <main className="w-full max-w-md mx-auto px-8 py-4 flex flex-col flex-grow">
        <p className="font-body text-sm text-on-surface-variant">Signup coming in the next task.</p>
      </main>
    </div>
  )
}
