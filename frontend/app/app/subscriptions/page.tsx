'use client'

import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import {
  AlertTriangle,
  CalendarClock,
  MoonStar,
  Repeat,
  TrendingUp,
  Wallet,
} from 'lucide-react'
import { subscriptionsApi } from '@/lib/api/subscriptions'
import type { Subscription, SubscriptionSummary } from '@/types/subscription'
import { formatCurrency, formatDate } from '@/lib/utils/format'
import { Skeleton } from '@/components/ui/skeleton'
import { staggerChild, staggerParent } from '@/lib/motion'
import { cn } from '@/lib/utils'

export default function SubscriptionsPage() {
  const [data, setData] = useState<SubscriptionSummary | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    subscriptionsApi
      .getAll()
      .then(setData)
      .catch(() => setError('Could not load subscriptions. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <div className="p-8">
        <Skeleton className="mb-2 h-8 w-56" />
        <Skeleton className="mb-8 h-4 w-80" />
        <div className="grid gap-4 sm:grid-cols-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return <p className="p-8 text-sm text-destructive">{error}</p>
  }

  const subscriptions = data?.subscriptions ?? []
  const active = subscriptions.filter((s) => !s.possiblyInactive)
  const dormant = subscriptions.filter((s) => s.possiblyInactive)

  return (
    <div className="p-8">
      <header className="mb-8">
        <h1 className="font-heading text-2xl font-bold">Subscriptions</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Recurring charges detected automatically from your statements.
        </p>
      </header>

      {subscriptions.length === 0 ? (
        <EmptyState />
      ) : (
        <>
          <motion.div
            variants={staggerParent}
            initial="hidden"
            animate="show"
            className="mb-10 grid gap-4 sm:grid-cols-3"
          >
            <StatCard
              icon={Repeat}
              label="Active subscriptions"
              value={String(data?.activeCount ?? 0)}
            />
            <StatCard
              icon={Wallet}
              label="Per month"
              value={formatCurrency(data?.monthlyTotal ?? 0)}
              accent
            />
            <StatCard
              icon={CalendarClock}
              label="Per year"
              value={formatCurrency(data?.annualisedTotal ?? 0)}
            />
          </motion.div>

          {active.length > 0 && (
            <section className="mb-10">
              <h2 className="mb-3 text-sm font-semibold">Active</h2>
              <div className="flex flex-col gap-2">
                {active.map((s, i) => (
                  <SubscriptionRow key={s.recurringGroupId} subscription={s} index={i} />
                ))}
              </div>
            </section>
          )}

          {dormant.length > 0 && (
            <section>
              <h2 className="mb-1 flex items-center gap-2 text-sm font-semibold">
                <MoonStar className="h-4 w-4 text-muted-foreground" />
                Possibly cancelled
              </h2>
              <p className="mb-3 text-xs text-muted-foreground">
                No charge for well over the usual interval.
              </p>
              <div className="flex flex-col gap-2">
                {dormant.map((s, i) => (
                  <SubscriptionRow key={s.recurringGroupId} subscription={s} index={i} dimmed />
                ))}
              </div>
            </section>
          )}
        </>
      )}
    </div>
  )
}

function StatCard({
  icon: Icon,
  label,
  value,
  accent,
}: {
  icon: React.ElementType
  label: string
  value: string
  accent?: boolean
}) {
  return (
    <motion.div
      variants={staggerChild}
      className={cn(
        'rounded-2xl border p-5',
        accent ? 'border-primary/30 bg-primary/5' : 'border-border bg-card'
      )}
    >
      <span className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
        <Icon className="h-3.5 w-3.5" />
        {label}
      </span>
      <p
        className={cn(
          'mt-2 font-mono text-2xl font-bold tabular-nums',
          accent && 'text-primary'
        )}
      >
        {value}
      </p>
    </motion.div>
  )
}

function SubscriptionRow({
  subscription,
  index,
  dimmed,
}: {
  subscription: Subscription
  index: number
  dimmed?: boolean
}) {
  const s = subscription

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: dimmed ? 0.65 : 1, y: 0 }}
      transition={{ delay: Math.min(index * 0.04, 0.3), duration: 0.3 }}
      className="flex items-center justify-between gap-4 rounded-xl border border-border bg-card px-4 py-3 transition-colors hover:border-primary/30"
    >
      <div className="flex min-w-0 flex-col gap-1">
        <span className="flex items-center gap-2 truncate text-sm font-semibold">
          {s.merchantName}
          {s.priceIncreased && (
            <span
              className="inline-flex items-center gap-1 rounded-full bg-amber-500/15 px-1.5 py-0.5 text-[0.6rem] font-bold text-amber-600"
              title={`Was ${formatCurrency(s.averageAmount)} on average`}
            >
              <TrendingUp className="h-2.5 w-2.5" />
              Price up
            </span>
          )}
        </span>
        <span className="flex flex-wrap items-center gap-x-2 gap-y-0.5 text-xs text-muted-foreground">
          <span>{s.cadence}</span>
          <span aria-hidden>·</span>
          <span>{s.occurrences} charges</span>
          {s.categoryName && (
            <>
              <span aria-hidden>·</span>
              <span>{s.categoryName}</span>
            </>
          )}
          <span aria-hidden>·</span>
          <span>{formatCurrency(s.totalSpent)} total</span>
        </span>
      </div>

      <div className="flex shrink-0 flex-col items-end gap-0.5">
        <span className="font-mono text-sm font-bold tabular-nums">
          {formatCurrency(s.amount)}
        </span>
        {s.possiblyInactive ? (
          <span className="flex items-center gap-1 text-[0.7rem] text-muted-foreground">
            <AlertTriangle className="h-3 w-3" />
            Last {formatDate(s.lastCharge)}
          </span>
        ) : (
          s.estimatedNextCharge && (
            <span className="text-[0.7rem] text-muted-foreground">
              Next ~{formatDate(s.estimatedNextCharge)}
            </span>
          )
        )}
      </div>
    </motion.div>
  )
}

function EmptyState() {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border py-20 text-center">
      <Repeat className="mb-4 h-8 w-8 text-muted-foreground" />
      <h2 className="text-sm font-semibold">No recurring charges yet</h2>
      <p className="mt-1 max-w-sm text-xs leading-relaxed text-muted-foreground">
        Finapse spots subscriptions once it sees the same merchant charge you at a
        regular interval. Import a few months of statements and they will show up here.
      </p>
    </div>
  )
}
