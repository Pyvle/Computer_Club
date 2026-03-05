import { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import apiClient from '../utils/apiClient'

export interface ClubMembership {
  clubId: number
  clubName: string
  role: string
}

export interface PendingApplicationBrief {
  applicationId: number
  clubName: string
  status: string
}

export interface UserContext {
  userId: number
  phone: string | null
  email: string | null
  globalRole: string
  clubs: ClubMembership[]
  pendingApplications: PendingApplicationBrief[]
}

interface AuthContextValue {
  user: UserContext | null
  loadContext: () => Promise<UserContext>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserContext | null>(null)

  const loadContext = useCallback(async (): Promise<UserContext> => {
    const { data } = await apiClient.get<UserContext>('/me/context')
    setUser(data)
    return data
  }, [])

  const logout = useCallback(() => {
    const refreshToken = localStorage.getItem('refreshToken')
    if (refreshToken) {
      // fire-and-forget: выход не должен блокировать UI
      apiClient.post('/auth/logout', { refreshToken }).catch(() => {})
    }
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, loadContext, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
