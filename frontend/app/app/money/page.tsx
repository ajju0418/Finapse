'use client'

import { useEffect, useState } from 'react'
import { dashboardApi } from '@/lib/api/dashboard'
import type { DashboardData, DashboardPeriod } from '@/types/dashboard'
import { SummaryCards } from '@/components/dashboard/SummaryCards'
import { SpendingBreakdown } from '@/components/dashboard/SpendingBreakdown'
import { TopMerchants } from '@/components/dashboard/TopMerchants'
import { RecentTransactions } from '@/components/dashboard/RecentTransactions'
import { AttentionBanner } from '@/components/dashboard/AttentionBanner'
import { ReconciliationReviewPanel } from '@/components/reconciliation/ReconciliationReviewPanel'
import { Skeleton } from '@/components/ui/skeleton'
import { formatDate } from '@/lib/utils/format'

const PERIODS: { value: DashboardPeriod; label: string }[] = [
  { value: 'THIS_MONTH', label: 'This Month' },
  { value: '7_DAYS', label: '7 Days' },
  { value: '30_DAYS', label: '30 Days' },
  { value: '3_MONTHS', label: '3 Months' },
  { value: '6_MONTHS', label: '6 Months' },
  { value: '1_YEAR', label: '1 Year' },
]

export default function MoneyPage() {
  const [period, setPeriod] = useState<DashboardPeriod>('THIS_MONTH')
  const [data, setData] = useState<DashboardData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setLoading(true)
    setError(null)
    dashboardApi.get(period)
      .then(setData)
      .catch(() => setError('Could not load dashboard. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [period])

  return (
    <div className="p-8">
        {/* Header */}
        <div className="mb-6 flex items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold">Money</h1>
            <p className="text-sm text-muted-foreground mt-1">
              {data ? `${formatDate(data.periodStart)} — ${formatDate(data.periodEnd)}` : 'Your financial overview'}
            </p>
          </div>
          <div className="flex gap-1 rounded-lg border border-border bg-muted/40 p-1">
            {PERIODS.map(p => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value)}
                className={`rounded-md px-3 py-1.5 text-xs font-medium transition-colors ${
                  period === p.value
                    ? 'bg-background shadow-sm text-foreground'
                    : 'text-muted-foreground hover:text-foreground'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>

        {/* Error */}
        {error && (
          <div className="mb-6 rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
            {error}
          </div>
        )}

        {/* Loading skeleton */}
        {loading && (
          <div className="space-y-6">
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
              {[1, 2, 3, 4].map(i => <Skeleton key={i} className="h-24 rounded-xl" />)}
            </div>
            <Skeleton className="h-16 rounded-xl" />
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <Skeleton className="h-64 rounded-xl" />
              <Skeleton className="h-64 rounded-xl" />
            </div>
          </div>
        )}

        {/* Dashboard content */}
        {!loading && data && (
          <div className="space-y-6">
            {/* Summary metrics */}
            <SummaryCards data={data} />

            {/* Attention banner */}
            {data.pendingReviewCount > 0 && (
              <AttentionBanner count={data.pendingReviewCount} />
            )}

            {/* Middle row: category breakdown + top merchants */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              <div className="rounded-xl border border-border bg-card p-6">
                <h2 className="text-sm font-semibold mb-4">Spending by Category</h2>
                <SpendingBreakdown categories={data.categoryBreakdown} />
              </div>
              <div className="rounded-xl border border-border bg-card p-6">
                <h2 className="text-sm font-semibold mb-4">Top Merchants</h2>
                <TopMerchants merchants={data.topMerchants} />
              </div>
            </div>

            {/* Recent transactions */}
            <div className="rounded-xl border border-border bg-card p-6">
              <h2 className="text-sm font-semibold mb-4">Recent Transactions</h2>
              <RecentTransactions transactions={data.recentTransactions} />
            </div>

            {/* Reconciliation reviews */}
            {data.pendingReviewCount > 0 && (
              <div id="reviews" className="rounded-xl border border-border bg-card p-6">
                <h2 className="text-sm font-semibold mb-4">Needs Your Attention</h2>
                <ReconciliationReviewPanel />
              </div>
            )}
          </div>
        )}

        {/* Empty state — no data at all */}
        {!loading && !error && data && data.recentTransactions.length === 0 && (
          <div className="mt-6 rounded-xl border border-dashed border-border p-12 text-center">
            <p className="text-muted-foreground font-medium">No transactions yet.</p>
            <p className="text-sm text-muted-foreground mt-1">
              Upload your first bank or credit-card statement to start understanding your spending.
            </p>
            <a
              href="/app/statements"
              className="mt-4 inline-block rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              Upload Statement
            </a>
          </div>
        )}
      </div>
  )
}
