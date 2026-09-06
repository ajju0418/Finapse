'use client'

import { useEffect, useState } from 'react'
import { dashboardApi } from '@/lib/api/dashboard'
import type { DashboardData, DashboardPeriod, DateRange } from '@/types/dashboard'
import { SummaryCards } from '@/components/dashboard/SummaryCards'
import { SpendingBreakdown } from '@/components/dashboard/SpendingBreakdown'
import { SpendingTrend } from '@/components/dashboard/SpendingTrend'
import { PeriodSelector } from '@/components/dashboard/PeriodSelector'
import { TopMerchants } from '@/components/dashboard/TopMerchants'
import { AttentionBanner } from '@/components/dashboard/AttentionBanner'
import { AddTransactionForm } from '@/components/transactions/AddTransactionForm'
import { ReconciliationReviewPanel } from '@/components/reconciliation/ReconciliationReviewPanel'
import { FinancialSourceCard } from '@/components/financial/FinancialSourceCard'
import { Skeleton } from '@/components/ui/skeleton'
import { formatCurrency } from '@/lib/utils/format'
import { Plus } from 'lucide-react'
import { RECONCILIATION_UPDATED_EVENT, type ReconciliationEventDetail } from '@/lib/events/reconciliation'

export default function MoneyPage() {
  const [period, setPeriod] = useState<DashboardPeriod>('THIS_MONTH')
  const [range, setRange]   = useState<DateRange | undefined>(undefined)
  const [data, setData]     = useState<DashboardData | null>(null)
  const [pendingReviewCount, setPendingReviewCount] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]   = useState<string | null>(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    setLoading(true)
    setError(null)
    dashboardApi.get(period, range)
      .then((res) => {
        setData(res)
        setPendingReviewCount(res.pendingReviewCount)
      })
      .catch(() => setError('Could not reach backend.'))
      .finally(() => setLoading(false))
  }, [period, range, reloadKey])

  useEffect(() => {
    const handleReconciliationEvent = (e: Event) => {
      const customEvent = e as CustomEvent<ReconciliationEventDetail>
      if (customEvent.detail && typeof customEvent.detail.pendingCount === 'number') {
        setPendingReviewCount(customEvent.detail.pendingCount)
      }
    }

    window.addEventListener(RECONCILIATION_UPDATED_EVENT, handleReconciliationEvent)
    return () => window.removeEventListener(RECONCILIATION_UPDATED_EVENT, handleReconciliationEvent)
  }, [])

  const activeReviewCount = pendingReviewCount ?? data?.pendingReviewCount ?? 0

  const savingsRate = data && data.income > 0
    ? Math.round(((data.income - data.actualSpending) / data.income) * 100)
    : null


  return (
    <div className="min-h-screen p-6 md:p-8 space-y-6">

      {/* ── Header ─────────────────────────────────────────── */}
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Financial Overview</h1>
          <p className="text-xs text-muted-foreground mt-0.5">
            {data ? `${data.periodStart} → ${data.periodEnd}` : 'Your money, clearly.'}
          </p>
        </div>
        <PeriodSelector
          period={period}
          range={range}
          onChange={(nextPeriod, nextRange) => {
            setPeriod(nextPeriod)
            setRange(nextRange)
          }}
        />
      </div>

      {/* Cash and other off-statement spending */}
      {showAddForm ? (
        <AddTransactionForm
          onCreated={() => {
            setShowAddForm(false)
            setReloadKey(k => k + 1)
          }}
          onCancel={() => setShowAddForm(false)}
        />
      ) : (
        <button
          onClick={() => setShowAddForm(true)}
          className="flex items-center gap-1.5 rounded-lg border border-dashed border-border px-4 py-2 text-xs font-semibold text-muted-foreground transition-colors hover:border-primary hover:text-foreground"
        >
          <Plus className="h-3.5 w-3.5" />
          Add a cash transaction
        </button>
      )}

      {/* ── Error ──────────────────────────────────────────── */}
      {error && (
        <div className="rounded-xl border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {/* ── Loading skeleton ───────────────────────────────── */}
      {loading && (
        <div className="space-y-6">
          <Skeleton className="h-32 w-full rounded-2xl" />
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[1,2,3,4].map(i => <Skeleton key={i} className="h-24 rounded-xl" />)}
          </div>
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            <Skeleton className="h-72 rounded-xl" />
            <Skeleton className="h-72 rounded-xl" />
          </div>
        </div>
      )}

      {/* ── Dashboard ──────────────────────────────────────── */}
      {!loading && data && (
        <div className="space-y-6">

          {/* Hero pulse card */}
          <div className="relative overflow-hidden rounded-2xl border border-border bg-card p-6 md:p-8">
            {/* Glow */}
            <div className="pointer-events-none absolute -top-16 -right-16 h-48 w-48 rounded-full bg-primary/10 blur-3xl" />
            <div className="pointer-events-none absolute -bottom-16 -left-16 h-48 w-48 rounded-full bg-primary/5 blur-3xl" />

            <div className="relative flex flex-col md:flex-row md:items-center md:justify-between gap-6">
              {/* Net cash flow — hero number */}
              <div>
                <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-1">
                  Net Cash Flow
                </p>
                <p className={`text-5xl font-black tracking-tighter ${
                  data.netCashFlow >= 0 ? 'text-primary' : 'text-destructive'
                }`}>
                  {data.netCashFlow >= 0 ? '+' : ''}{formatCurrency(data.netCashFlow)}
                </p>
                {savingsRate !== null && (
                  <p className="mt-2 text-sm text-muted-foreground">
                    <span className={`font-semibold ${savingsRate >= 20 ? 'text-primary' : savingsRate >= 0 ? 'text-yellow-400' : 'text-destructive'}`}>
                      {savingsRate}% savings rate
                    </span>
                    {' '}this period
                  </p>
                )}
              </div>

              {/* Income vs Spending visual */}
              <div className="flex-1 max-w-sm space-y-3">
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Income</span>
                    <span className="font-semibold text-primary">{formatCurrency(data.income)}</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted overflow-hidden">
                    <div className="h-full rounded-full bg-primary transition-all" style={{ width: '100%' }} />
                  </div>
                </div>
                <div>
                  <div className="flex justify-between text-xs mb-1">
                    <span className="text-muted-foreground">Spending</span>
                    <span className="font-semibold text-destructive">{formatCurrency(data.actualSpending)}</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted overflow-hidden">
                    <div
                      className="h-full rounded-full bg-destructive transition-all"
                      style={{ width: data.income > 0 ? `${Math.min(100, (data.actualSpending / data.income) * 100)}%` : '0%' }}
                    />
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Summary stats row */}
          <SummaryCards data={data} />

          {/* My Financial Sources */}
          <div className="space-y-4">
            <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
              My Financial Sources
            </p>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {data.sourceSummaries?.map(source => (
                <FinancialSourceCard
                  key={source.id}
                  source={{
                    name: source.name,
                    institution: source.institution,
                    currentBalance: formatCurrency(source.currentBalance),
                    totalSpending: formatCurrency(source.totalSpending),
                    isCard: source.isCard
                  }}
                />
              ))}
            </div>
          </div>

          {/* Attention banner */}
          {activeReviewCount > 0 && (
            <AttentionBanner count={activeReviewCount} />
          )}

          {/* Month-over-month trend */}
          <SpendingTrend months={6} />

          {/* Category + Merchants */}
          <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
            <div className="lg:col-span-3 rounded-2xl border border-border bg-card p-6">
              <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-5">
                Spending by Category
              </p>
              <SpendingBreakdown categories={data.categoryBreakdown} />
            </div>
            <div className="lg:col-span-2 rounded-2xl border border-border bg-card p-6">
              <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground mb-5">
                Top Merchants
              </p>
              <TopMerchants merchants={data.topMerchants} />
            </div>
          </div>

          {/* Reconciliation */}
          {(activeReviewCount > 0 || (pendingReviewCount !== null && pendingReviewCount === 0 && (data.pendingReviewCount ?? 0) > 0)) && (
            <div id="reviews" className="rounded-2xl border border-border/80 bg-card/70 backdrop-blur-md p-6 shadow-sm transition-all duration-300 scroll-mt-6">
              <div className="flex items-center justify-between mb-5">
                <div>
                  <div className="flex items-center gap-2.5">
                    <h2 className="font-heading text-lg font-bold tracking-tight">Needs Your Attention</h2>
                    {activeReviewCount > 0 ? (
                      <span className="rounded-full bg-primary/15 border border-primary/25 px-2.5 py-0.5 text-xs font-bold font-mono text-primary">
                        {activeReviewCount} Pending
                      </span>
                    ) : (
                      <span className="rounded-full bg-emerald-500/15 border border-emerald-500/25 px-2.5 py-0.5 text-xs font-semibold text-emerald-400">
                        All Reconciled
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">
                    Review matching transactions across statements to ensure money is not counted twice.
                  </p>
                </div>
              </div>
              <ReconciliationReviewPanel
                onCountChange={(newCount) => {
                  setPendingReviewCount(newCount)
                  if (newCount === 0) {
                    // Silently refresh dashboard stats to reflect the confirmed reconciliation links
                    dashboardApi.get(period, range).then(setData).catch(() => {})
                  }
                }}
              />
            </div>
          )}
        </div>
      )}

      {/* Empty state */}
      {!loading && !error && data && data.categoryBreakdown.length === 0 && data.topMerchants.length === 0 && (
        <div className="rounded-2xl border border-dashed border-border p-16 text-center">
          <p className="text-lg font-semibold">Nothing here yet.</p>
          <p className="text-sm text-muted-foreground mt-2 max-w-xs mx-auto">
            Upload a bank or credit-card statement to see your financial overview.
          </p>
          <a
            href="/app/statements"
            className="mt-6 inline-block rounded-xl bg-primary px-6 py-2.5 text-sm font-semibold text-primary-foreground hover:opacity-90 transition-opacity"
          >
            Upload Statement
          </a>
        </div>
      )}
    </div>
  )
}



