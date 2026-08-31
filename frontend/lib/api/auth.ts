import { apiClient, refreshAccessToken } from '@/lib/api/client'
import { clearAccessToken, setAccessToken } from '@/lib/auth/token-store'
import type {
  AuthResponse,
  AuthUser,
  LoginPayload,
  RegisterPayload,
} from '@/types/auth'

export const authApi = {
  async register(payload: RegisterPayload): Promise<AuthResponse> {
    const data = await apiClient.post<AuthResponse>('/auth/register', payload)
    setAccessToken(data.accessToken, data.expiresIn)
    return data
  },

  async login(payload: LoginPayload): Promise<AuthResponse> {
    const data = await apiClient.post<AuthResponse>('/auth/login', payload)
    setAccessToken(data.accessToken, data.expiresIn)
    return data
  },

  /** Trades the httpOnly refresh cookie for a new access token. */
  refresh(): Promise<AuthResponse | null> {
    return refreshAccessToken()
  },

  /**
   * Revokes the session server-side and drops the in-memory token.
   * The local token is cleared even if the network call fails, so the UI
   * can never be left showing a signed-in state after an explicit sign-out.
   */
  async logout(): Promise<void> {
    try {
      await apiClient.post<void>('/auth/logout')
    } finally {
      clearAccessToken()
    }
  },

  me(): Promise<AuthUser> {
    return apiClient.get<AuthUser>('/auth/me')
  },
}
