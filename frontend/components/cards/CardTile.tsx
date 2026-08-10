'use client'

import { useEffect, useState } from 'react'
import type { Card, CardAnalytics } from '@/types/card'
import { cardsApi } from '@/lib/api/cards'
import { TransactionList } from '@/components/transactions/TransactionList'
import { formatCurrency } from '@/lib/utils/format'
import { CreditCard, ChevronDown, ChevronUp } from 'lucide-react'

interface Props {
  card: Card
}

export function CardTile({ card }: Props) {
  const [analytics, setAnalytics] = useState<CardAnalytics | null>(null)
  const [expanded, setExpanded] = useState(false)

  useEffect(() => {
    cardsApi.getAnalytics(card.id)
      .then(setAnalytics)
      .catch(() => {/* no transactions yet */})
  }, [card.id])

  const usagePercent =
    analytics?.availableCredit != null && card.creditLimit
      ? Math.min(100, (analytics.outstanding / card.creditLimit) * 100)
      : null

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
                className="h-full rounded-full bg-primary transition-all"
                style={{ width: `${usagePercent ?? 0}%` }}
              />
            </div>
          </div>
        )}

        {/* Analytics grid */}
        {analytics && (
          <div className="grid grid-cols-3 gap-3 text-sm">
            <div className="rounded-lg bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Spending</p>
              <p className="font-semibold text-red-600">{formatCurrency(analytics.totalSpending)}</p>
            </div>
            <div className="rounded-lg bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Cashback</p>
              <p className="font-semibold text-emerald-600">{formatCurrency(analytics.totalCashback)}</p>
            </div>
            <div className="rounded-lg bg-muted/50 p-3">
              <p className="text-xs text-muted-foreground mb-1">Payments</p>
              <p className="font-semibold text-purple-600">{formatCurrency(analytics.totalPayments)}</p>
            </div>
          </div>
        )}

        {card.paymentDueDay && (
          <p className="text-xs text-muted-foreground mt-3">
            Payment due: day {card.paymentDueDay} of each month
          </p>
        )}
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
