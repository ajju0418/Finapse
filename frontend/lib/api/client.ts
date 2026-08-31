import { API_BASE_URL } from '@/lib/constants'
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '@/lib/auth/token-store'
import type { AuthResponse } from '@/types/auth'

/** Endpoints that must never trigger the refresh-and-retry cycle. */
const AUTH_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout']

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(message: string, status: number, code: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
  }
}

let onUnauthorized: (() => void) | null = null

/** Registered by the auth provider so an unrecoverable 401 can bounce to /login. */
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler
}

/**
 * Concurrent 401s must not each fire their own refresh — that would rotate the
 * token several times and trip the backend's reuse detection. All callers share
 * one in-flight promise.
 */
let refreshPromise: Promise<AuthResponse | null> | null = null

export function refreshAccessToken(): Promise<AuthResponse | null> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: 'POST',
          credentials: 'include',
          headers: { Accept: 'application/json' },
        })
        if (!res.ok) {
          clearAccessToken()
          return null
        }
        const data = (await res.json()) as AuthResponse
        setAccessToken(data.accessToken, data.expiresIn)
        return data
      } catch {
        clearAccessToken()
        return null
      } finally {
        // Released on the next tick so late callers still observe this result.
        setTimeout(() => {
          refreshPromise = null
        }, 0)
      }
    })()
  }
  return refreshPromise
}

function buildHeaders(options: RequestInit | undefined, token: string | null): HeadersInit {
  const headers: Record<string, string> = { Accept: 'application/json' }

  // FormData bodies must keep the browser-generated multipart boundary.
  if (options?.body !== undefined && !(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  return { ...headers, ...(options?.headers as Record<string, string> | undefined) }
}

async function toApiError(res: Response): Promise<ApiError> {
  const body = await res.json().catch(() => null)
  return new ApiError(
    body?.message ?? 'Something went wrong. Please try again.',
    res.status,
    body?.code ?? 'UNKNOWN'
  )
}

async function request<T>(path: string, options?: RequestInit, isRetry = false): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    // Always send the refresh cookie so rotation works across origins.
    credentials: 'include',
    headers: buildHeaders(options, getAccessToken()),
  })

  const isAuthEndpoint = AUTH_ENDPOINTS.some((endpoint) => path.startsWith(endpoint))

  if (res.status === 401 && !isRetry && !isAuthEndpoint) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      return request<T>(path, options, true)
    }
    onUnauthorized?.()
    throw await toApiError(res)
  }

  if (!res.ok) {
    throw await toApiError(res)
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return {} as T
  }
  return (await res.json()) as T
}

export const apiClient = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
  postForm: <T>(path: string, form: FormData) =>
    request<T>(path, { method: 'POST', body: form }),
  patch: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'PATCH',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T = void>(path: string) => request<T>(path, { method: 'DELETE' }),
}
