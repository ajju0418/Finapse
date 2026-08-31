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
      whileHover={inert ? undefined : { y: -2 }}
      whileTap={inert ? undefined : { y: 0, scale: 0.99 }}
      transition={{ type: 'spring', stiffness: 400, damping: 28 }}
      className={cn(
        'group relative flex h-12 w-full items-center justify-center gap-2 overflow-hidden rounded-xl',
        'bg-primary text-sm font-bold text-primary-foreground',
        'shadow-lg shadow-primary/25 transition-shadow',
        'focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-primary/50',
        inert ? 'cursor-not-allowed opacity-60' : 'hover:shadow-xl hover:shadow-primary/35',
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
