export type UserRole = 'USER' | 'ADMIN'

export interface AuthUser {
  id: string
  name: string
  email: string
  role: UserRole
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  /** Access-token lifetime in seconds. */
  expiresIn: number
  user: AuthUser
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  name: string
  email: string
  password: string
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export interface ApiError {
  timestamp: string
  status: number
  code: string
  message: string
}
