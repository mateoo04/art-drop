import { createContext } from 'react'

export type AuthPromptContextValue = {
  promptToAuth: (action: string) => void
  requireAuth: (action: string, run: () => void | Promise<void>) => void
}

export const AuthPromptContext = createContext<AuthPromptContextValue | null>(null)
