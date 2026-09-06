'use client'

import { Zap, ArrowRight } from 'lucide-react'

interface Props {
  count: number
}

export function AttentionBanner({ count }: Props) {
  if (count <= 0) return null

  const scrollToReviews = () => {
    const el = document.getElementById('reviews')
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
      el.classList.add('ring-2', 'ring-primary/60', 'transition-all', 'duration-500')
      setTimeout(() => {
        el.classList.remove('ring-2', 'ring-primary/60')
      }, 2000)
    }
  }

  return (
    <div className="relative overflow-hidden rounded-2xl border border-primary/20 bg-gradient-to-r from-primary/[0.08] via-card/80 to-card/60 p-4 sm:p-5 shadow-lg shadow-primary/[0.02] backdrop-blur-md transition-all">
      {/* Subtle ambient decorative glow */}
      <div className="pointer-events-none absolute -right-10 -top-10 h-32 w-32 rounded-full bg-primary/10 blur-2xl" />

      <div className="relative flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-start sm:items-center gap-3.5">
          {/* Live notification indicator */}
          <div className="relative flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-primary/25 bg-primary/15 text-primary shadow-sm">
            <Zap className="h-5 w-5 fill-primary/25 text-primary" />
            <span className="absolute -top-1 -right-1 flex h-3 w-3">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-primary opacity-75"></span>
              <span className="relative inline-flex h-3 w-3 rounded-full border-2 border-background bg-primary"></span>
            </span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <p className="text-sm font-bold tracking-tight text-foreground">
                {count} {count === 1 ? 'transaction needs' : 'transactions need'} review
              </p>
              <span className="hidden sm:inline-flex rounded-full bg-primary/15 px-2 py-0.5 text-[11px] font-bold font-mono text-primary">
                Action Required
              </span>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              Reconcile transfers, card payments, and refunds to ensure money is not counted twice.
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={scrollToReviews}
          className="group flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground shadow-sm hover:bg-primary/90 active:scale-[0.98] transition-all cursor-pointer shrink-0"
        >
          <span>Review Now</span>
          <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5" />
        </button>
      </div>
    </div>
  )
}

