import { AlertTriangle } from 'lucide-react'

interface Props {
  count: number
}

export function AttentionBanner({ count }: Props) {
  if (count === 0) return null

  return (
    <div className="flex items-center justify-between rounded-xl border border-yellow-300 bg-yellow-50 px-5 py-4">
      <div className="flex items-center gap-3">
        <AlertTriangle className="h-5 w-5 text-yellow-600 shrink-0" />
        <div>
          <p className="text-sm font-semibold text-yellow-800">
            {count} {count === 1 ? 'item needs' : 'items need'} your attention
          </p>
          <p className="text-xs text-yellow-700 mt-0.5">
            Finapse detected possible duplicates, card payments, or refunds that need review.
          </p>
        </div>
      </div>
      <a
        href="/app/money#reviews"
        className="shrink-0 rounded-md bg-yellow-600 px-4 py-2 text-xs font-semibold text-white hover:bg-yellow-700 transition-colors"
      >
        Review
      </a>
    </div>
  )
}
