'use client'

import { useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { useAuth } from '@/components/auth/AuthProvider'
import { FinapseLogo } from '@/components/branding/FinapseLogo'

/**
 * Second guard layer, inside the client.
 *
 * The middleware only knows a cookie exists; this waits for the refresh call to
 * actually succeed before rendering anything user-scoped, so a revoked or expired
 * session never flashes real data on screen.
 */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { status } = useAuth()
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    if (status === 'unauthenticated') {
      const next = encodeURIComponent(pathname)
      router.replace(`/login?next=${next}`)
    }
  }, [status, router, pathname])

  if (status === 'authenticated') {
    return <>{children}</>
  }

  return <SessionSplash />
}

function SessionSplash() {
  return (
    <div className="flex h-screen w-full flex-col items-center justify-center gap-8 bg-background">
      <div className="relative flex items-center justify-center">
        {[0, 1, 2].map((ring) => (
          <motion.span
            key={ring}
            className="absolute rounded-2xl border border-primary/30"
            initial={{ width: 64, height: 64, opacity: 0.5 }}
            animate={{ width: 168, height: 168, opacity: 0 }}
            transition={{
              duration: 2.4,
              repeat: Infinity,
              ease: 'easeOut',
              delay: ring * 0.8,
            }}
          />
        ))}
        <motion.div
          animate={{ scale: [1, 1.06, 1] }}
          transition={{ duration: 2.4, repeat: Infinity, ease: 'easeInOut' }}
          className="relative rounded-2xl shadow-2xl shadow-primary/30"
        >
          <FinapseLogo size={56} showText={false} />
        </motion.div>
      </div>

      <div className="flex flex-col items-center gap-3">
        <p className="font-heading text-sm font-semibold tracking-[0.3em] text-muted-foreground uppercase">
          Restoring session
        </p>
        <div className="h-px w-40 overflow-hidden bg-border">
          <motion.div
            className="h-full w-1/3 bg-primary"
            animate={{ x: ['-100%', '300%'] }}
            transition={{ duration: 1.4, repeat: Infinity, ease: 'easeInOut' }}
          />
        </div>
      </div>
    </div>
  )
}
