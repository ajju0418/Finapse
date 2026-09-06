'use client'

import { useEffect, useState } from 'react'
import {
  CreditCard,
  ArrowRightLeft,
  Copy,
  RotateCcw,
  Sparkles,
  Check,
  X,
  CheckCircle2,
  Calendar,
  Loader2,
  HelpCircle,
} from 'lucide-react'
import { reconciliationApi } from '@/lib/api/reconciliation'
import type { ReconciliationReview, ReviewType } from '@/types/reconciliation'
import { formatCurrency } from '@/lib/utils/format'
import { emitReconciliationUpdated } from '@/lib/events/reconciliation'

interface Props {
  onCountChange?: (count: number) => void
}

interface ReviewTypeConfig {
  label: string
  sublabel: string
  icon: React.ComponentType<{ className?: string }>
  badgeClass: string
  borderHover: string
}

const REVIEW_CONFIG: Record<ReviewType, ReviewTypeConfig> = {
  POSSIBLE_CARD_PAYMENT: {
    label: 'Credit Card Bill Payment',
    sublabel: 'Settles existing card spending (Rule D2)',
    icon: CreditCard,
    badgeClass: 'bg-primary/15 text-primary border-primary/25',
    borderHover: 'hover:border-primary/40',
  },
  POSSIBLE_TRANSFER: {
    label: 'Account Transfer',
    sublabel: 'Money moving between own accounts (Rule D3)',
    icon: ArrowRightLeft,
    badgeClass: 'bg-sky-500/15 text-sky-400 border-sky-500/25',
    borderHover: 'hover:border-sky-500/40',
  },
  POSSIBLE_DUPLICATE: {
    label: 'Duplicate Transaction',
    sublabel: 'Appears in multiple statements (Rule D5)',
    icon: Copy,
    badgeClass: 'bg-amber-500/15 text-amber-400 border-amber-500/25',
    borderHover: 'hover:border-amber-500/40',
  },
  POSSIBLE_REFUND: {
    label: 'Refund Match',
    sublabel: 'Offsets prior expense (Rule D1)',
    icon: RotateCcw,
    badgeClass: 'bg-teal-500/15 text-teal-400 border-teal-500/25',
    borderHover: 'hover:border-teal-500/40',
  },
  POSSIBLE_CASHBACK: {
    label: 'Reward Cashback',
    sublabel: 'Tracked separately from spending (Rule D4)',
    icon: Sparkles,
    badgeClass: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/25',
    borderHover: 'hover:border-emerald-500/40',
  },
}

export function ReconciliationReviewPanel({ onCountChange }: Props) {
  const [reviews, setReviews] = useState<ReconciliationReview[]>([])
  const [loading, setLoading] = useState(true)
  const [decidingId, setDecidingId] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    reconciliationApi.getPending()
      .then((data) => {
        setReviews(data)
        queueMicrotask(() => {
          onCountChange?.(data.length)
          emitReconciliationUpdated(data.length)
        })
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const decide = async (id: string, approved: boolean) => {
    setDecidingId(id)
    try {
      await reconciliationApi.decide(id, approved)
      const next = reviews.filter((r) => r.id !== id)
      setReviews(next)
      queueMicrotask(() => {
        onCountChange?.(next.length)
        emitReconciliationUpdated(next.length)
      })
    } catch {
      alert('Failed to submit decision. Please try again.')
    } finally {
      setDecidingId(null)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12 text-muted-foreground gap-2.5">
        <Loader2 className="h-5 w-5 animate-spin text-primary" />
        <span className="text-sm font-medium">Checking pending reviews…</span>
      </div>
    )
  }

  if (reviews.length === 0) {
    return (
      <div className="relative overflow-hidden rounded-2xl border border-emerald-500/20 bg-gradient-to-b from-emerald-500/[0.06] to-transparent p-8 text-center backdrop-blur-sm">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 shadow-lg shadow-emerald-500/10 mb-3.5">
          <CheckCircle2 className="h-6 w-6" />
        </div>
        <h3 className="font-heading text-base font-bold text-foreground">All Clear!</h3>
        <p className="text-xs text-muted-foreground mt-1 max-w-md mx-auto leading-relaxed">
          Zero pending reconciliation items. Every bank and credit card transaction has been cleanly categorized and linked.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {reviews.map((review) => {
        const config = REVIEW_CONFIG[review.reviewType] || {
          label: review.reviewType,
          sublabel: 'Reconciliation candidate',
          icon: HelpCircle,
          badgeClass: 'bg-muted text-muted-foreground border-border',
          borderHover: 'hover:border-border',
        }
        const Icon = config.icon
        const isDeciding = decidingId === review.id
        const confidencePct = Math.round(review.confidenceScore * 100)

        const source = review.sourceTransaction
        const target = review.targetTransaction

        return (
          <div
            key={review.id}
            className={`group relative overflow-hidden rounded-2xl border border-border/80 bg-card/60 p-5 backdrop-blur-md transition-all shadow-sm ${config.borderHover}`}
          >
            {/* Top Bar: Type, Confidence & Actions */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3.5 border-b border-border/50">
              <div className="flex flex-wrap items-center gap-2.5">
                <span className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-semibold ${config.badgeClass}`}>
                  <Icon className="h-3.5 w-3.5" />
                  {config.label}
                </span>

                <span className="rounded-full border border-border/60 bg-muted/40 px-2.5 py-0.5 text-[11px] font-mono font-medium text-muted-foreground">
                  {confidencePct}% Confidence
                </span>

                <span className="hidden md:inline-block text-xs text-muted-foreground/80">
                  {config.sublabel}
                </span>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2 self-end sm:self-auto shrink-0">
                <button
                  type="button"
                  onClick={() => decide(review.id, false)}
                  disabled={isDeciding}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-border/80 bg-muted/40 px-3.5 py-1.5 text-xs font-medium text-muted-foreground hover:bg-destructive/10 hover:text-destructive hover:border-destructive/30 active:scale-[0.98] disabled:opacity-50 transition-all cursor-pointer"
                  title="Reject match — treat as separate transactions"
                >
                  <X className="h-3.5 w-3.5" />
                  <span>Reject</span>
                </button>

                <button
                  type="button"
                  onClick={() => decide(review.id, true)}
                  disabled={isDeciding}
                  className="inline-flex items-center gap-1.5 rounded-xl border border-emerald-500/30 bg-emerald-500/15 px-4 py-1.5 text-xs font-semibold text-emerald-400 hover:bg-emerald-500/25 hover:border-emerald-500/50 active:scale-[0.98] disabled:opacity-50 transition-all cursor-pointer shadow-sm"
                  title="Confirm link — avoid double counting"
                >
                  {isDeciding ? (
                    <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  ) : (
                    <Check className="h-3.5 w-3.5" />
                  )}
                  <span>Confirm Link</span>
                </button>
              </div>
            </div>

            {/* Explanation Reason */}
            {review.systemReason && (
              <p className="text-xs text-muted-foreground/90 mt-3 leading-relaxed">
                <span className="font-semibold text-foreground/80">Engine Reason: </span>
                {review.systemReason}
              </p>
            )}

            {/* Comparison Grid: Source vs Target */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3.5">
              {/* Source Transaction Box */}
              <div className="rounded-xl border border-border/60 bg-muted/20 p-3.5 transition-colors group-hover:bg-muted/30">
                <div className="flex items-center justify-between text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70 mb-1.5">
                  <span>Source Transaction</span>
                  <span className="flex items-center gap-1 text-[11px] font-normal font-mono lowercase opacity-90">
                    <Calendar className="h-3 w-3 opacity-60" />
                    {new Date(source.transactionDate).toLocaleDateString()}
                  </span>
                </div>

                <div className="flex items-baseline justify-between gap-3 mt-1">
                  <p className="text-sm font-semibold text-foreground truncate" title={source.merchantName || source.description}>
                    {source.merchantName || source.description}
                  </p>
                  <span
                    className={`text-sm font-black font-mono tracking-tight shrink-0 ${
                      source.direction === 'CREDIT' ? 'text-emerald-400' : 'text-foreground'
                    }`}
                  >
                    {source.direction === 'CREDIT' ? '+' : '−'}{formatCurrency(source.amount)}
                  </span>
                </div>

                <div className="mt-1 flex items-center gap-2 text-[11px] text-muted-foreground">
                  <span className="capitalize">{source.transactionType?.toLowerCase().replace(/_/g, ' ')}</span>
                  {source.categoryName && (
                    <>
                      <span>•</span>
                      <span className="truncate">{source.categoryName}</span>
                    </>
                  )}
                </div>
              </div>

              {/* Target Transaction Box */}
              <div className="rounded-xl border border-border/60 bg-muted/20 p-3.5 transition-colors group-hover:bg-muted/30">
                <div className="flex items-center justify-between text-[10px] font-bold uppercase tracking-wider text-muted-foreground/70 mb-1.5">
                  <span>Matched Candidate</span>
                  <span className="flex items-center gap-1 text-[11px] font-normal font-mono lowercase opacity-90">
                    <Calendar className="h-3 w-3 opacity-60" />
                    {new Date(target.transactionDate).toLocaleDateString()}
                  </span>
                </div>

                <div className="flex items-baseline justify-between gap-3 mt-1">
                  <p className="text-sm font-semibold text-foreground truncate" title={target.merchantName || target.description}>
                    {target.merchantName || target.description}
                  </p>
                  <span
                    className={`text-sm font-black font-mono tracking-tight shrink-0 ${
                      target.direction === 'CREDIT' ? 'text-emerald-400' : 'text-foreground'
                    }`}
                  >
                    {target.direction === 'CREDIT' ? '+' : '−'}{formatCurrency(target.amount)}
                  </span>
                </div>

                <div className="mt-1 flex items-center gap-2 text-[11px] text-muted-foreground">
                  <span className="capitalize">{target.transactionType?.toLowerCase().replace(/_/g, ' ')}</span>
                  {target.categoryName && (
                    <>
                      <span>•</span>
                      <span className="truncate">{target.categoryName}</span>
                    </>
                  )}
                </div>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

