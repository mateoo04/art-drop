import { useCallback, useMemo, useState, type ReactNode } from 'react'
import { AuthPromptModal } from '../components/auth/AuthPromptModal'
import { getToken } from '../lib/auth'
import { AuthPromptContext, type AuthPromptContextValue } from './authPromptContext'

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
