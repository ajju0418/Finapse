import { formatCurrency } from '@/lib/utils/format'
import type { DashboardData } from '@/types/dashboard'

interface Props {
  data: DashboardData
}

export function SummaryCards({ data }: Props) {
  const cards = [
    {
      label: 'Income',
      value: data.income,
      color: 'text-green-600',
      bg: 'bg-green-50',
    },
    {
      label: 'Actual Spending',
      value: data.actualSpending,
      color: 'text-red-600',
      bg: 'bg-red-50',
      sub: data.refunds > 0 ? `${formatCurrency(data.refunds)} refunded` : undefined,
    },
    {
      label: 'Net Cash Flow',
      value: data.netCashFlow,
      color: data.netCashFlow >= 0 ? 'text-green-600' : 'text-red-600',
      bg: data.netCashFlow >= 0 ? 'bg-green-50' : 'bg-red-50',
    },
    {
      label: 'Cashback',
      value: data.cashback,
      color: 'text-emerald-600',
      bg: 'bg-emerald-50',
    },
  ]

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map(card => (
        <div key={card.label} className={`rounded-xl border border-border p-5 ${card.bg}`}>
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wider mb-2">
            {card.label}
          </p>
          <p className={`text-2xl font-bold ${card.color}`}>
            {formatCurrency(card.value)}
          </p>
          {card.sub && (
            <p className="text-xs text-muted-foreground mt-1">{card.sub}</p>
          )}
        </div>
      ))}
    </div>
  )
}
