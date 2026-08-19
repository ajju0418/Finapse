import { formatCurrency } from '@/lib/utils/format'
import type { CategorySpending } from '@/types/dashboard'

interface Props {
  categories: CategorySpending[]
}

// Distinct palette for categories — cycles if more than 10
const PALETTE = [
  { bar: 'bg-primary',       text: 'text-primary'       },
  { bar: 'bg-violet-500',    text: 'text-violet-400'    },
  { bar: 'bg-orange-500',    text: 'text-orange-400'    },
  { bar: 'bg-sky-500',       text: 'text-sky-400'       },
  { bar: 'bg-rose-500',      text: 'text-rose-400'      },
  { bar: 'bg-yellow-500',    text: 'text-yellow-400'    },
  { bar: 'bg-teal-500',      text: 'text-teal-400'      },
  { bar: 'bg-pink-500',      text: 'text-pink-400'      },
  { bar: 'bg-indigo-500',    text: 'text-indigo-400'    },
  { bar: 'bg-amber-500',     text: 'text-amber-400'     },
]

const CATEGORY_EMOJI: Record<string, string> = {
  FOOD_DINING:    '🍽',
  GROCERIES:      '🛒',
  SHOPPING:       '🛍',
  TRANSPORTATION: '🚗',
  BILLS_UTILITIES:'⚡',
  ENTERTAINMENT:  '🎬',
  HEALTHCARE:     '💊',
  TRAVEL:         '✈️',
  EDUCATION:      '📚',
  SUBSCRIPTIONS:  '🔁',
  OTHER:          '📦',
}

export function SpendingBreakdown({ categories }: Props) {
  if (categories.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <p className="text-3xl mb-3">📊</p>
        <p className="text-sm text-muted-foreground">No expense data for this period.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {categories.map((cat, i) => {
        const palette = PALETTE[i % PALETTE.length]
        const emoji = CATEGORY_EMOJI[cat.categoryName] ?? '📦'
        const label = cat.categoryName.replace(/_/g, ' ')
        return (
          <div key={cat.categoryName}>
            <div className="flex items-center justify-between mb-1.5">
              <div className="flex items-center gap-2">
                <span className="text-sm">{emoji}</span>
                <span className="text-sm font-medium capitalize">{label.toLowerCase().replace(/\b\w/g, c => c.toUpperCase())}</span>
              </div>
              <div className="flex items-center gap-3">
                <span className={`text-xs font-semibold tabular-nums ${palette.text}`}>{cat.percentage}%</span>
                <span className="text-sm font-bold tabular-nums">{formatCurrency(cat.amount)}</span>
              </div>
            </div>
            <div className="h-1.5 w-full rounded-full bg-muted/60 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all duration-700 ${palette.bar}`}
                style={{ width: `${cat.percentage}%` }}
              />
            </div>
          </div>
        )
      })}
    </div>
  )
}
