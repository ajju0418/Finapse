/**
 * In-memory access-token store.
 *
 * The access token is deliberately NOT written to localStorage, sessionStorage or a
 * readable cookie: anything reachable from JavaScript is also reachable from an XSS
 * payload. Durability comes from the httpOnly refresh cookie instead, which the
 * browser replays on `/auth/refresh` to mint a new access token after a reload.
 */

let accessToken: string | null = null
let expiresAt = 0

type Listener = (token: string | null) => void
const listeners = new Set<Listener>()

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null, expiresInSeconds?: number): void {
  accessToken = token
  // Renew slightly early so an in-flight request never races the expiry.
  expiresAt = token && expiresInSeconds ? Date.now() + (expiresInSeconds - 30) * 1000 : 0
  listeners.forEach((listener) => listener(token))
}

export function clearAccessToken(): void {
  setAccessToken(null)
}

export function isAccessTokenExpired(): boolean {
  return !accessToken || Date.now() >= expiresAt
}

export function subscribeToToken(listener: Listener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}
