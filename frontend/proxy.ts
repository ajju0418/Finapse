import { NextResponse, type NextRequest } from 'next/server'

/**
 * Edge-level route guard (Next.js 16 `proxy` convention, formerly `middleware`).
 *
 * This is the first of two layers. It only inspects whether the httpOnly refresh
 * cookie is present, which is enough to keep unauthenticated visitors from ever
 * downloading protected pages. It deliberately does not verify the token — the
 * signing key stays on the API server, and `<AuthGuard>` plus the API's own
 * authorization checks are the authoritative gate.
 *
 * Requires the API and the app to share a host (same-origin in production, or
 * localhost on different ports in development), since cookies ignore port.
 */

const REFRESH_COOKIE = process.env.NEXT_PUBLIC_REFRESH_COOKIE_NAME ?? 'finapse_refresh'

const PROTECTED_PREFIXES = ['/app']
const AUTH_ROUTES = ['/login', '/register']

export function proxy(request: NextRequest) {
  const { pathname, search } = request.nextUrl
  const hasSession = Boolean(request.cookies.get(REFRESH_COOKIE)?.value)

  const isProtected = PROTECTED_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`)
  )
  const isAuthRoute = AUTH_ROUTES.includes(pathname)

  if (isProtected && !hasSession) {
    const loginUrl = new URL('/login', request.url)
    // Remember where they were headed so sign-in can return them there.
    if (pathname !== '/app') {
      loginUrl.searchParams.set('next', `${pathname}${search}`)
    }
    return NextResponse.redirect(loginUrl)
  }

  if (isAuthRoute && hasSession) {
    return NextResponse.redirect(new URL('/app/money', request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/app/:path*', '/login', '/register'],
}
