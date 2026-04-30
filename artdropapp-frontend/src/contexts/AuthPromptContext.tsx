import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { AuthPromptModal } from '../components/auth/AuthPromptModal'
import { getToken } from '../lib/auth'

type AuthPromptContextValue = {
  promptToAuth: (action: string) => void
  requireAuth: (action: string, run: () => void | Promise<void>) => void
}

const AuthPromptContext = createContext<AuthPromptContextValue | null>(null)

export function AuthPromptProvider({ children }: { children: ReactNode }) {
  const [action, setAction] = useState<string | null>(null)

  const promptToAuth = useCallback((next: string) => {
    setAction(next)
  }, [])

  const requireAuth = useCallback(
    (next: string, run: () => void | Promise<void>) => {
      if (getToken()) {
        void run()
        return
      }
      setAction(next)
    },
    [],
  )

  const close = useCallback(() => setAction(null), [])

  const value = useMemo<AuthPromptContextValue>(
    () => ({ promptToAuth, requireAuth }),
    [promptToAuth, requireAuth],
  )

  return (
    <AuthPromptContext.Provider value={value}>
      {children}
      <AuthPromptModal open={action !== null} action={action ?? ''} onClose={close} />
    </AuthPromptContext.Provider>
  )
}

export function useAuthPrompt(): AuthPromptContextValue {
  const ctx = useContext(AuthPromptContext)
  if (!ctx) throw new Error('useAuthPrompt must be used within AuthPromptProvider')
  return ctx
}
