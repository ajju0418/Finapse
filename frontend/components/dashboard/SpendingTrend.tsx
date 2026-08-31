'use client'

import { useEffect, useState } from 'react'
import { dashboardApi } from '@/lib/api/dashboard'
import type { TrendData } from '@/types/dashboard'
import { formatCurrency } from '@/lib/utils/format'
import { Skeleton } from '@/components/ui/skeleton'
import { TrendingDown, TrendingUp } from 'lucide-react'

interface Props {
  months?: number
}

/**
 * Month-by-month spending vs income. Rendered as plain divs rather than a chart
 * library so it stays dependency-free and readable at small sizes.
 */
export function SpendingTrend({ months = 6 }: Props) {
  const [data, setData] = useState<TrendData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    setLoading(true)
    dashboardApi
      .trends(months)
      .then((d) => {
        setData(d)
        setError(false)
      })
      .catch(() => setError(true))
      .finally(() => setLoading(false))
  }, [months])

  if (loading) return <Skeleton className="h-72 rounded-xl" />
  if (error || !data) return null

  const peak = Math.max(
    1,
    ...data.points.map((p) => Math.max(p.actualSpending, p.income))
  )
  const hasActivity = data.points.some((p) => p.transactionCount > 0)

  const change = data.spendingChangePercent
  const spendingUp = change !== null && change > 0

  return (
    <div className="rounded-xl border border-border bg-card p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h2 className="text-sm font-semibold">Trend</h2>
          <p className="mt-0.5 text-xs text-muted-foreground">
            Last {data.points.length} months · avg spend {formatCurrency(data.averageMonthlySpend)}
          </p>
        </div>
        {change !== null && (
          <div
            className={`flex items-center gap-1 text-xs font-semibold ${
              spendingUp ? 'text-destructive' : 'text-primary'
            }`}
          >
            {spendingUp ? (
              <TrendingUp className="h-3.5 w-3.5" />
            ) : (
              <TrendingDown className="h-3.5 w-3.5" />
            )}
            {Math.abs(change).toFixed(0)}% vs last month
          </div>
        )}
      </div>

      {!hasActivity ? (
        <p className="mt-8 text-center text-xs text-muted-foreground">
          Import a few statements to see how your spending moves over time.
        </p>
      ) : (
        <>
          <div className="mt-6 flex h-40 items-end gap-2">
            {data.points.map((point) => (
              <div key={point.bucket} className="flex flex-1 flex-col items-center gap-1">
                <div className="flex h-full w-full items-end justify-center gap-0.5">
                  <div
                    title={`Income ${formatCurrency(point.income)}`}
                    className="w-1/2 rounded-t bg-primary/70 transition-all"
                    style={{ height: `${(point.income / peak) * 100}%` }}
                  />
                  <div
                    title={`Spending ${formatCurrency(point.actualSpending)}`}
                    className="w-1/2 rounded-t bg-destructive/70 transition-all"
                    style={{ height: `${(point.actualSpending / peak) * 100}%` }}
                  />
                </div>
                <span className="text-[10px] text-muted-foreground">
                  {point.label.split(' ')[0]}
                </span>
              </div>
            ))}
          </div>

          <div className="mt-4 flex gap-4 text-xs text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-sm bg-primary/70" /> Income
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-sm bg-destructive/70" /> Spending
            </span>
          </div>
        </>
      )}
    </div>
  )
}
