import { formatCurrency } from '@/lib/utils/format'
import type { MerchantSpending } from '@/types/dashboard'

interface Props {
  merchants: MerchantSpending[]
}

const AVATAR_COLORS = [
  'bg-primary/20 text-primary',
  'bg-violet-500/20 text-violet-400',
  'bg-orange-500/20 text-orange-400',
  'bg-sky-500/20 text-sky-400',
  'bg-rose-500/20 text-rose-400',
]

export function TopMerchants({ merchants }: Props) {
  if (merchants.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <p className="text-3xl mb-3">🏪</p>
        <p className="text-sm text-muted-foreground">No merchant data for this period.</p>
      </div>
    )
  }

  const max = merchants[0]?.amount ?? 1

  return (
    <div className="space-y-3">
      {merchants.map((m, i) => (
        <div key={m.merchantName} className="group relative">
          {/* Background spend bar */}
          <div
            className="absolute inset-0 rounded-lg bg-muted/30 transition-all"
            style={{ width: `${Math.max(8, (m.amount / max) * 100)}%` }}
          />
          <div className="relative flex items-center justify-between px-3 py-2.5 rounded-lg">
            <div className="flex items-center gap-3">
              <span className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-sm font-bold ${AVATAR_COLORS[i % AVATAR_COLORS.length]}`}>
                {m.merchantName.charAt(0).toUpperCase()}
              </span>
              <div>
                <p className="text-sm font-semibold leading-tight">{m.merchantName}</p>
                <p className="text-xs text-muted-foreground">
                  {m.transactionCount} {m.transactionCount === 1 ? 'txn' : 'txns'}
                </p>
              </div>
            </div>
            <span className="text-sm font-bold tabular-nums text-destructive">
              {formatCurrency(m.amount)}
            </span>
          </div>
        </div>
      ))}
    </div>
  )
}
