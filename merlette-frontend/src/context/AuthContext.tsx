// Contexte d'authentification : utilisateur courant, token, login(), logout()
// TODO

import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { currentUser } from '../api/auth'
import { TOKEN_KEY, errorMessage } from '../api/client'
import type { Session, User } from '../types'
import { AuthContext } from './auth'
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => !!localStorage.getItem(TOKEN_KEY))
  const [sessionError, setSessionError] = useState('')
  const cache = useQueryClient()
  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY); setUser(null); cache.clear()
  }, [cache])
  useEffect(() => {
    let active = true
    window.addEventListener('merlette:session-expired', logout)
    if (localStorage.getItem(TOKEN_KEY)) {
      currentUser().then(value => { if (active) setUser(value) })
        .catch(error => { if (active) setSessionError(errorMessage(error)) })
        .finally(() => { if (active) setLoading(false) })
    }
    return () => { active = false; window.removeEventListener('merlette:session-expired', logout) }
  }, [logout])
  function accept(session: Session) {
    cache.clear(); localStorage.setItem(TOKEN_KEY, session.token)
    setSessionError(''); setUser(session.user)
  }
  return <AuthContext.Provider value={{ user, loading, sessionError, accept, logout }}>{children}</AuthContext.Provider>
}
