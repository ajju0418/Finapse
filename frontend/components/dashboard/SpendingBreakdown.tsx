import { formatCurrency } from '@/lib/utils/format'
import type { CategorySpending } from '@/types/dashboard'

interface Props {
  categories: CategorySpending[]
}

export function SpendingBreakdown({ categories }: Props) {
  if (categories.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">
        No expense data for this period.
      </p>
    )
  }

  return (
    <div className="space-y-3">
      {categories.map(cat => (
        <div key={cat.categoryName}>
          <div className="flex items-center justify-between mb-1">
            <span className="text-sm font-medium">{cat.categoryName}</span>
            <div className="flex items-center gap-3">
              <span className="text-xs text-muted-foreground">{cat.percentage}%</span>
              <span className="text-sm font-semibold">{formatCurrency(cat.amount)}</span>
            </div>
          </div>
          <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
            <div
              className="h-full rounded-full bg-primary/70"
              style={{ width: `${cat.percentage}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  )
}
