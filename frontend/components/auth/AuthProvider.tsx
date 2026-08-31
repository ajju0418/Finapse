'use client'

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { useRouter } from 'next/navigation'
import { authApi } from '@/lib/api/auth'
import { setUnauthorizedHandler } from '@/lib/api/client'
import { clearAccessToken } from '@/lib/auth/token-store'
import type { AuthUser, LoginPayload, RegisterPayload } from '@/types/auth'

type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

interface AuthContextValue {
  user: AuthUser | null
  status: AuthStatus
  isAuthenticated: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** Refresh this far ahead of expiry so a request never rides an expired token. */
const REFRESH_LEAD_SECONDS = 60

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const [user, setUser] = useState<AuthUser | null>(null)
  const [status, setStatus] = useState<AuthStatus>('loading')
  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  const cancelScheduledRefresh = useCallback(() => {
    if (refreshTimer.current) {
      clearTimeout(refreshTimer.current)
      refreshTimer.current = null
    }
  }, [])

  /**
   * Keeps the session alive silently. The access token lives only in memory, so
   * without this the user would be bounced out the moment it expires mid-session.
   */
  const scheduleRefresh = useCallback(
    (expiresIn: number) => {
      cancelScheduledRefresh()
      const delay = Math.max(expiresIn - REFRESH_LEAD_SECONDS, 30) * 1000
      refreshTimer.current = setTimeout(async () => {
        const result = await authApi.refresh()
        if (result) {
          setUser(result.user)
          scheduleRefresh(result.expiresIn)
        } else {
          setUser(null)
          setStatus('unauthenticated')
        }
      }, delay)
    },
    [cancelScheduledRefresh]
  )

  // Rehydrate on first paint: a full reload wipes the in-memory token, but the
  // httpOnly refresh cookie survives and can mint a new one.
  useEffect(() => {
    let cancelled = false

    authApi
      .refresh()
      .then((result) => {
        if (cancelled) return
        if (result) {
          setUser(result.user)
          setStatus('authenticated')
          scheduleRefresh(result.expiresIn)
        } else {
          setUser(null)
          setStatus('unauthenticated')
        }
      })
      .catch(() => {
        if (cancelled) return
        setUser(null)
        setStatus('unauthenticated')
      })

    return () => {
      cancelled = true
      cancelScheduledRefresh()
    }
  }, [scheduleRefresh, cancelScheduledRefresh])

  // A 401 the client could not recover from means the session is truly gone.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      cancelScheduledRefresh()
      clearAccessToken()
      setUser(null)
      setStatus('unauthenticated')
      router.replace('/login')
    })
    return () => setUnauthorizedHandler(null)
  }, [router, cancelScheduledRefresh])

  const login = useCallback(
    async (payload: LoginPayload) => {
      const result = await authApi.login(payload)
      setUser(result.user)
      setStatus('authenticated')
      scheduleRefresh(result.expiresIn)
    },
    [scheduleRefresh]
  )

  const register = useCallback(
    async (payload: RegisterPayload) => {
      const result = await authApi.register(payload)
      setUser(result.user)
      setStatus('authenticated')
      scheduleRefresh(result.expiresIn)
    },
    [scheduleRefresh]
  )

  const logout = useCallback(async () => {
    cancelScheduledRefresh()
    try {
      await authApi.logout()
    } finally {
      setUser(null)
      setStatus('unauthenticated')
      router.replace('/login')
    }
  }, [router, cancelScheduledRefresh])

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAuthenticated: status === 'authenticated',
      login,
      register,
      logout,
    }),
    [user, status, login, register, logout]
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an <AuthProvider>')
  }
  return context
}
