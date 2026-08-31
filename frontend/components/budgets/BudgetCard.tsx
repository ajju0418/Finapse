'use client'

import type { Budget } from '@/types/budget'
import { formatCurrency, formatShortDate } from '@/lib/utils/format'
import { AlertTriangle, Pencil, Trash2, TrendingUp } from 'lucide-react'

const STATUS_STYLES: Record<Budget['status'], { bar: string; text: string; label: string }> = {
  ON_TRACK: { bar: 'bg-primary', text: 'text-primary', label: 'On track' },
  AT_RISK: { bar: 'bg-yellow-500', text: 'text-yellow-500', label: 'Close to limit' },
  EXCEEDED: { bar: 'bg-destructive', text: 'text-destructive', label: 'Over budget' },
}

interface Props {
  budget: Budget
  onEdit: (budget: Budget) => void
  onDelete: (budget: Budget) => void
}

export function BudgetCard({ budget, onEdit, onDelete }: Props) {
  const style = STATUS_STYLES[budget.status]
  const width = Math.min(budget.percentUsed, 100)
  const title = budget.categoryName ?? 'All spending'

  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold">{title}</h3>
          <p className="text-xs text-muted-foreground mt-0.5">
            {formatShortDate(budget.periodStart)} → {formatShortDate(budget.periodEnd)}
          </p>
        </div>
        <div className="flex gap-1">
          <button
            onClick={() => onEdit(budget)}
            aria-label={`Edit ${title} budget`}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-accent hover:text-foreground"
          >
            <Pencil className="h-3.5 w-3.5" />
          </button>
          <button
            onClick={() => onDelete(budget)}
            aria-label={`Delete ${title} budget`}
            className="rounded-md p-1.5 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
          >
            <Trash2 className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      <div className="mt-4 flex items-baseline gap-2">
        <span className={`text-2xl font-black tracking-tight ${style.text}`}>
          {formatCurrency(budget.spent)}
        </span>
        <span className="text-xs text-muted-foreground">
          of {formatCurrency(budget.limitAmount)}
        </span>
      </div>

      <div className="mt-3 h-2 overflow-hidden rounded-full bg-muted">
        <div
          className={`h-full rounded-full transition-all ${style.bar}`}
          style={{ width: `${width}%` }}
        />
      </div>

      <div className="mt-2 flex items-center justify-between text-xs">
        <span className={`font-semibold ${style.text}`}>
          {budget.percentUsed.toFixed(0)}% · {style.label}
        </span>
        <span className="text-muted-foreground">
          {budget.remaining >= 0
            ? `${formatCurrency(budget.remaining)} left`
            : `${formatCurrency(Math.abs(budget.remaining))} over`}
        </span>
      </div>

      {budget.projectedToExceed && budget.status !== 'EXCEEDED' && (
        <p className="mt-3 flex items-start gap-1.5 rounded-lg bg-yellow-500/10 p-2 text-xs text-yellow-500">
          <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0" />
          <span>
            At this pace you&apos;ll spend about {formatCurrency(budget.projectedSpend)} by{' '}
            {formatShortDate(budget.periodEnd)}.
          </span>
        </p>
      )}

      {budget.status === 'ON_TRACK' && !budget.projectedToExceed && budget.spent > 0 && (
        <p className="mt-3 flex items-center gap-1.5 text-xs text-muted-foreground">
          <TrendingUp className="h-3.5 w-3.5" />
          Projected {formatCurrency(budget.projectedSpend)} by period end.
        </p>
      )}
    </div>
  )
}
