import { formatCurrency } from '@/lib/utils/format'
import type { DashboardData } from '@/types/dashboard'
import { TrendingUp, TrendingDown, ArrowDownLeft, Sparkles } from 'lucide-react'

interface Props {
  data: DashboardData
}

export function SummaryCards({ data }: Props) {
  const cards = [
    {
      label: 'Income',
      value: data.income,
      valueClass: 'text-primary',
      icon: TrendingUp,
      iconClass: 'text-primary',
      glow: 'shadow-primary/10',
      sub: null,
    },
    {
      label: 'Gross Expenses',
      value: data.grossExpenses,
      valueClass: 'text-destructive',
      icon: TrendingDown,
      iconClass: 'text-destructive',
      glow: 'shadow-destructive/10',
      sub: data.refunds > 0 ? `−${formatCurrency(data.refunds)} refunded` : null,
    },
    {
      label: 'Actual Spending',
      value: data.actualSpending,
      valueClass: 'text-orange-400',
      icon: ArrowDownLeft,
      iconClass: 'text-orange-400',
      glow: 'shadow-orange-500/10',
      sub: 'after refunds',
    },
    {
      label: 'Cashback Earned',
      value: data.cashback,
      valueClass: 'text-emerald-400',
      icon: Sparkles,
      iconClass: 'text-emerald-400',
      glow: 'shadow-emerald-500/10',
      sub: null,
    },
  ]

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map(card => {
        const Icon = card.icon
        return (
          <div
            key={card.label}
            className={`relative overflow-hidden rounded-xl border border-border bg-card p-5 shadow-lg ${card.glow}`}
          >
            <div className="flex items-start justify-between mb-3">
              <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
                {card.label}
              </p>
              <Icon className={`h-3.5 w-3.5 ${card.iconClass} opacity-70`} />
            </div>
            <p className={`text-2xl font-black tracking-tight ${card.valueClass}`}>
              {formatCurrency(card.value)}
            </p>
            {card.sub && (
              <p className="text-xs text-muted-foreground mt-1.5">{card.sub}</p>
            )}
          </div>
        )
      })}
    </div>
  )
}
