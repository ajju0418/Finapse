'use client'

import { motion } from 'framer-motion'
import { ArrowUpRight } from 'lucide-react'
import Link from 'next/link'
import { fadeUp } from '@/lib/motion'
import { cn } from '@/lib/utils'

interface Props {
  icon: React.ReactNode
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
  actionHref?: string
  className?: string
}

/**
 * Reusable premium empty state.
 * Renders inside a glass surface with a soft glow behind the icon.
 */
export function EmptyState({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  actionHref,
  className,
}: Props) {
  const button = actionLabel && (
    <span className="btn-premium press mt-6 inline-flex items-center gap-1.5 rounded-xl px-5 py-2.5 text-sm font-semibold">
      {actionLabel}
      <ArrowUpRight className="h-4 w-4" />
    </span>
  )

  return (
    <motion.div
      variants={fadeUp}
      initial="hidden"
      animate="show"
      className={cn(
        'surface relative flex flex-col items-center overflow-hidden px-8 py-16 text-center',
        className
      )}
    >
      <div
        aria-hidden
        className="pointer-events-none absolute -top-16 left-1/2 h-40 w-40 -translate-x-1/2 rounded-full bg-primary/15 blur-3xl"
      />
      <div className="relative mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-primary/10 text-primary ring-1 ring-primary/15">
        {icon}
      </div>
      <h3 className="font-display text-2xl tracking-tight text-foreground">{title}</h3>
      <p className="mt-2 max-w-sm text-sm text-muted-foreground">{description}</p>

      {actionLabel && actionHref && (
        <Link href={actionHref}>{button}</Link>
      )}
      {actionLabel && onAction && !actionHref && (
        <button type="button" onClick={onAction}>
          {button}
        </button>
      )}
    </motion.div>
  )
}
