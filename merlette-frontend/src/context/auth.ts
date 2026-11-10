import { createContext } from 'react'
import type { Session, User } from '../types'
export const AuthContext = createContext<{
  user: User | null; loading: boolean; sessionError: string; accept: (session: Session) => void; logout: () => void
} | null>(null)
