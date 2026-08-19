import { Zap } from 'lucide-react'

interface Props {
  count: number
}

export function AttentionBanner({ count }: Props) {
  if (count === 0) return null

  return (
    <div className="flex items-center justify-between rounded-2xl border border-amber-500/30 bg-amber-500/10 px-5 py-4">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-amber-500/20">
          <Zap className="h-4 w-4 text-amber-400" />
        </div>
        <div>
          <p className="text-sm font-semibold text-amber-300">
            {count} {count === 1 ? 'transaction needs' : 'transactions need'} review
          </p>
          <p className="text-xs text-amber-400/70 mt-0.5">
            Possible duplicates, card payments, or refunds detected.
          </p>
        </div>
      </div>
      <a
        href="/app/money#reviews"
        className="shrink-0 rounded-lg bg-amber-500/20 border border-amber-500/30 px-4 py-1.5 text-xs font-semibold text-amber-300 hover:bg-amber-500/30 transition-colors"
      >
        Review →
      </a>
    </div>
  )
}
