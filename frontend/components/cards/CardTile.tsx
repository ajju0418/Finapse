'use client'

import { useEffect, useState } from 'react'
import type { Card, CardAnalytics } from '@/types/card'
import { cardsApi } from '@/lib/api/cards'
import { TransactionList } from '@/components/transactions/TransactionList'
import { formatCurrency, formatDate } from '@/lib/utils/format'
import { CreditCard, ChevronDown, ChevronUp } from 'lucide-react'

interface Props {
  card: Card
}

const UTILIZATION_STYLES: Record<string, { bar: string; text: string; label: string }> = {
  LOW:      { bar: 'bg-primary',     text: 'text-primary',     label: 'Healthy' },
  MODERATE: { bar: 'bg-yellow-500',  text: 'text-yellow-500',  label: 'Moderate' },
  HIGH:     { bar: 'bg-destructive', text: 'text-destructive', label: 'High — may affect your credit score' },
}

export function CardTile({ card }: Props) {
  const [analytics, setAnalytics] = useState<CardAnalytics | null>(null)
  const [expanded, setExpanded] = useState(false)

  useEffect(() => {
    cardsApi.getAnalytics(card.id)
      .then(setAnalytics)
      .catch(() => {/* no transactions yet */})
  }, [card.id])

  const utilization = analytics?.utilizationPercent ?? null
  const usagePercent = utilization != null ? Math.min(100, utilization) : null
  const band = analytics?.utilizationBand ? UTILIZATION_STYLES[analytics.utilizationBand] : null
  const daysUntilDue = analytics?.daysUntilDue ?? null

  return (
    <div className="rounded-xl border border-border bg-card overflow-hidden">
      {/* Card header */}
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div>
            <p className="font-semibold text-base">{card.name}</p>
            {card.issuer && (
              <p className="text-xs text-muted-foreground mt-0.5">{card.issuer}</p>
            )}
          </div>
          <CreditCard className="h-5 w-5 text-muted-foreground" />
        </div>

        {card.lastFourDigits && (
          <p className="text-sm font-mono tracking-widest text-muted-foreground mb-4">
            •••• •••• •••• {card.lastFourDigits}
          </p>
        )}

        {/* Limit + usage */}
        {card.creditLimit != null && (
          <div className="mb-4">
            <div className="flex justify-between text-xs text-muted-foreground mb-1">
              <span>Credit used</span>
              <span>
                {analytics
                  ? `${formatCurrency(analytics.outstanding)} / ${formatCurrency(card.creditLimit)}`
                  : formatCurrency(card.creditLimit) + ' limit'}
              </span>
            </div>
            <div className="h-1.5 w-full rounded-full bg-muted overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${band?.bar ?? 'bg-primary'}`}
                style={{ width: `${usagePercent ?? 0}%` }}
              />
            </div>
            {utilization != null && band && (
              <p className={`mt-1 text-xs font-medium ${band.text}`}>
                {utilization.toFixed(0)}% utilisation · {band.label}
              </p>
            )}
          </div>
        )}

        {/* Spending — always visible, prominent */}
        <div className="flex items-end justify-between mb-4">
          <div>
            <p className="text-xs text-muted-foreground mb-0.5">Total Spending</p>
            <p className="text-2xl font-bold text-red-500">
              {analytics ? formatCurrency(analytics.totalSpending) : '—'}
            </p>
          </div>
          {analytics && analytics.totalCashback > 0 && (
            <div className="text-right">
              <p className="text-xs text-muted-foreground mb-0.5">Cashback earned</p>
              <p className="text-sm font-semibold text-emerald-500">+{formatCurrency(analytics.totalCashback)}</p>
            </div>
          )}
        </div>

        {/* Secondary stats — payments + transaction count */}
        {analytics && (
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="rounded-lg bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Payments Made</p>
              <p className="font-semibold text-purple-500">{formatCurrency(analytics.totalPayments)}</p>
            </div>
            <div className="rounded-lg bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Transactions</p>
              <p className="font-semibold">{analytics.transactionCount}</p>
            </div>
          </div>
        )}

        {/* Billing cycle */}
        {analytics?.currentCycleStart && analytics.currentCycleEnd && (
          <div className="mt-3 rounded-lg bg-muted/50 p-3 text-xs">
            <div className="flex justify-between">
              <span className="text-muted-foreground">This cycle</span>
              <span className="font-medium">
                {formatDate(analytics.currentCycleStart)} → {formatDate(analytics.currentCycleEnd)}
              </span>
            </div>
            <div className="mt-1 flex justify-between">
              <span className="text-muted-foreground">Spent this cycle</span>
              <span className="font-semibold">{formatCurrency(analytics.currentCycleSpend)}</span>
            </div>
          </div>
        )}

        {analytics?.nextDueDate ? (
          <p
            className={`mt-3 text-xs font-medium ${
              daysUntilDue != null && daysUntilDue <= 5 ? 'text-yellow-500' : 'text-muted-foreground'
            }`}
          >
            Payment due {formatDate(analytics.nextDueDate)}
            {daysUntilDue != null && daysUntilDue >= 0 && ` · in ${daysUntilDue} day${daysUntilDue === 1 ? '' : 's'}`}
          </p>
        ) : card.paymentDueDay ? (
          <p className="text-xs text-muted-foreground mt-3">
            Payment due: day {card.paymentDueDay} of each month
          </p>
        ) : null}
      </div>

      {/* Expand transactions */}
      <button
        onClick={() => setExpanded(v => !v)}
        className="flex w-full items-center justify-between border-t border-border px-6 py-3 text-xs font-medium text-muted-foreground hover:bg-muted/30 transition-colors"
      >
        <span>View Transactions</span>
        {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
      </button>

      {expanded && (
        <div className="border-t border-border px-6 py-4">
          <TransactionList cardId={card.id} />
        </div>
      )}
    </div>
  )
}
