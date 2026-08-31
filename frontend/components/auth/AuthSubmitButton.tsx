'use client'

import { motion } from 'framer-motion'
import { ArrowRight, Loader2 } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AuthSubmitButtonProps {
  loading: boolean
  disabled?: boolean
  children: React.ReactNode
  loadingLabel?: string
}

export function AuthSubmitButton({
  loading,
  disabled,
  children,
  loadingLabel = 'Just a moment…',
}: AuthSubmitButtonProps) {
  const inert = loading || disabled

  return (
    <motion.button
      type="submit"
      disabled={inert}
      whileHover={inert ? undefined : { y: -1 }}
      whileTap={inert ? undefined : { y: 0, scale: 0.995 }}
      transition={{ type: 'spring', stiffness: 400, damping: 28 }}
      className={cn(
        'premium-edge group relative flex h-12 w-full items-center justify-center gap-2 overflow-hidden rounded-xl',
        'text-[0.9rem] font-semibold tracking-wide text-white',
        'bg-gradient-to-b from-[oklch(0.66_0.19_295)] to-[oklch(0.54_0.19_295)]',
        'border border-[oklch(0.72_0.16_295)]/40',
        'transition-shadow duration-200',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--violet)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--ink)]',
        inert
          ? 'cursor-not-allowed opacity-55'
          : 'shadow-[0_8px_24px_-10px_oklch(0.64_0.19_295_/_70%)] hover:shadow-[0_12px_32px_-10px_oklch(0.64_0.19_295_/_85%)]',
        loading && 'auth-shimmer'
      )}
    >
      {loading ? (
        <>
          <Loader2 className="h-4 w-4 animate-spin" />
          {loadingLabel}
        </>
      ) : (
        <>
          {children}
          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
        </>
      )}
    </motion.button>
  )
}
