import { useCallback, useEffect, useRef, useState } from 'react'
import type { ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { currentUser } from '../api/auth'
import { TOKEN_KEY, errorMessage } from '../api/client'
import type { Session, User } from '../types'
import { AuthContext } from './auth'
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => !!localStorage.getItem(TOKEN_KEY))
  const [sessionError, setSessionError] = useState('')
  const generation = useRef(0)
  const controller = useRef<AbortController | null>(null)
  const cache = useQueryClient()
  const logout = useCallback(() => {
    generation.current++; controller.current?.abort()
    localStorage.removeItem(TOKEN_KEY)
    setUser(null); setLoading(false); setSessionError(''); cache.clear()
  }, [cache])
  useEffect(() => {
    const version = generation.current
    let active = true
    const request = new AbortController()
    controller.current = request
    const expired = () => { logout(); setSessionError('Votre session a expiré. Connectez-vous pour retrouver votre carnet.') }
    window.addEventListener('merlette:session-expired', expired)
    if (localStorage.getItem(TOKEN_KEY)) {
      currentUser(request.signal).then(value => { if (active && version === generation.current) setUser(value) })
        .catch(error => { if (active && version === generation.current && !axios.isCancel(error)) setSessionError(errorMessage(error)) })
        .finally(() => { if (active && version === generation.current) setLoading(false) })
    }
    return () => { active = false; request.abort(); window.removeEventListener('merlette:session-expired', expired) }
  }, [logout])
  function accept(session: Session) {
    generation.current++; controller.current?.abort()
    cache.clear(); localStorage.setItem(TOKEN_KEY, session.token)
    setSessionError(''); setLoading(false); setUser(session.user)
  }
  return <AuthContext.Provider value={{ user, loading, sessionError, accept, logout }}>{children}</AuthContext.Provider>
}
