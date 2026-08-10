import { formatCurrency } from '@/lib/utils/format'
import type { MerchantSpending } from '@/types/dashboard'

interface Props {
  merchants: MerchantSpending[]
}

export function TopMerchants({ merchants }: Props) {
  if (merchants.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">
        No merchant data for this period.
      </p>
    )
  }

  return (
    <div className="space-y-2">
      {merchants.map((m, i) => (
        <div key={m.merchantName} className="flex items-center justify-between py-1.5">
          <div className="flex items-center gap-3">
            <span className="flex h-6 w-6 items-center justify-center rounded-full bg-muted text-xs font-medium text-muted-foreground">
              {i + 1}
            </span>
            <div>
              <p className="text-sm font-medium">{m.merchantName}</p>
              <p className="text-xs text-muted-foreground">
                {m.transactionCount} {m.transactionCount === 1 ? 'transaction' : 'transactions'}
              </p>
            </div>
          </div>
          <span className="text-sm font-semibold text-red-600">
            {formatCurrency(m.amount)}
          </span>
        </div>
      ))}
    </div>
  )
}
