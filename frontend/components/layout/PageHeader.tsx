'use client'

import { motion } from 'framer-motion'
import { fadeUp } from '@/lib/motion'
import { cn } from '@/lib/utils'

interface Props {
  eyebrow?: string
  title: string
  subtitle?: string
  actions?: React.ReactNode
  className?: string
}

/**
 * Consistent premium page header used across app pages.
 * Serif display title + eyebrow + optional actions on the right.
 */
export function PageHeader({ eyebrow, title, subtitle, actions, className }: Props) {
  return (
    <motion.header
      variants={fadeUp}
      initial="hidden"
      animate="show"
      className={cn(
        'flex flex-wrap items-end justify-between gap-4',
        className
      )}
    >
      <div>
        {eyebrow && (
          <p className="mb-2 text-[10px] font-semibold uppercase tracking-[0.2em] text-primary">
            {eyebrow}
          </p>
        )}
        <h1 className="font-display text-4xl leading-none tracking-tight text-gradient-ink md:text-5xl">
          {title}
        </h1>
        {subtitle && (
          <p className="mt-2 text-sm text-muted-foreground">{subtitle}</p>
        )}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </motion.header>
  )
}
