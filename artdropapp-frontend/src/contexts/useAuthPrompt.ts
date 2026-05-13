import { useContext } from 'react'
import { AuthPromptContext, type AuthPromptContextValue } from './authPromptContext'

export function useAuthPrompt(): AuthPromptContextValue {
  const ctx = useContext(AuthPromptContext)
  if (!ctx) throw new Error('useAuthPrompt must be used within AuthPromptProvider')
  return ctx
}
