'use client'

import { useEffect, useState } from 'react'
import { reconciliationApi } from '@/lib/api/reconciliation'
import type { ReconciliationReview, ReviewType } from '@/types/reconciliation'
import { formatCurrency } from '@/lib/utils/format'

const REVIEW_LABELS: Record<ReviewType, string> = {
  POSSIBLE_DUPLICATE: 'Possible Duplicate',
  POSSIBLE_CARD_PAYMENT: 'Possible Card Payment',
  POSSIBLE_TRANSFER: 'Possible Transfer',
  POSSIBLE_REFUND: 'Possible Refund',
  POSSIBLE_CASHBACK: 'Possible Cashback',
}

const REVIEW_COLORS: Record<ReviewType, string> = {
  POSSIBLE_DUPLICATE: 'bg-orange-50 border-orange-200',
  POSSIBLE_CARD_PAYMENT: 'bg-purple-50 border-purple-200',
  POSSIBLE_TRANSFER: 'bg-blue-50 border-blue-200',
  POSSIBLE_REFUND: 'bg-teal-50 border-teal-200',
  POSSIBLE_CASHBACK: 'bg-emerald-50 border-emerald-200',
}

export function ReconciliationReviewPanel() {
  const [reviews, setReviews] = useState<ReconciliationReview[]>([])
  const [loading, setLoading] = useState(true)
  const [deciding, setDeciding] = useState<string | null>(null)

  const load = () => {
    setLoading(true)
    reconciliationApi.getPending()
      .then(setReviews)
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [])

  const decide = async (id: string, approved: boolean) => {
    setDeciding(id)
    try {
      await reconciliationApi.decide(id, approved)
      setReviews(prev => prev.filter(r => r.id !== id))
    } finally {
      setDeciding(null)
    }
  }

  if (loading) return <p className="text-sm text-gray-500">Loading reviews…</p>

  if (reviews.length === 0) {
    return (
      <div className="rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">
        No pending reconciliation reviews. All clear!
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {reviews.map(review => (
        <div
          key={review.id}
          className={`rounded-lg border p-4 ${REVIEW_COLORS[review.reviewType]}`}
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <span className="text-sm font-semibold text-gray-800">
                  {REVIEW_LABELS[review.reviewType]}
                </span>
                <span className="text-xs text-gray-500">
                  {Math.round(review.confidenceScore * 100)}% confidence
                </span>
              </div>
              <p className="text-xs text-gray-600 leading-relaxed mb-3">{review.systemReason}</p>
              
              <div className="grid grid-cols-2 gap-4 mt-2">
                <div className="rounded border bg-white p-2">
                  <div className="text-[10px] uppercase font-bold text-gray-400 mb-1">Source Transaction</div>
                  <div className="text-sm font-medium">{review.sourceTransaction.merchantName || review.sourceTransaction.description}</div>
                  <div className="text-xs text-gray-500">{new Date(review.sourceTransaction.transactionDate).toLocaleDateString()}</div>
                  <div className={`text-sm font-bold ${review.sourceTransaction.direction === 'CREDIT' ? 'text-green-600' : 'text-red-600'}`}>
                    {review.sourceTransaction.direction === 'CREDIT' ? '+' : '-'}{formatCurrency(review.sourceTransaction.amount)}
                  </div>
                </div>
                <div className="rounded border bg-white p-2">
                  <div className="text-[10px] uppercase font-bold text-gray-400 mb-1">Target Transaction</div>
                  <div className="text-sm font-medium">{review.targetTransaction.merchantName || review.targetTransaction.description}</div>
                  <div className="text-xs text-gray-500">{new Date(review.targetTransaction.transactionDate).toLocaleDateString()}</div>
                  <div className={`text-sm font-bold ${review.targetTransaction.direction === 'CREDIT' ? 'text-green-600' : 'text-red-600'}`}>
                    {review.targetTransaction.direction === 'CREDIT' ? '+' : '-'}{formatCurrency(review.targetTransaction.amount)}
                  </div>
                </div>
              </div>
            </div>
            <div className="flex gap-2 shrink-0">
              <button
                onClick={() => decide(review.id, true)}
                disabled={deciding === review.id}
                className="rounded-md bg-white border border-green-400 px-3 py-1.5 text-xs font-medium text-green-700 hover:bg-green-50 disabled:opacity-50 transition-colors"
              >
                Confirm
              </button>
              <button
                onClick={() => decide(review.id, false)}
                disabled={deciding === review.id}
                className="rounded-md bg-white border border-red-300 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
              >
                Reject
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
