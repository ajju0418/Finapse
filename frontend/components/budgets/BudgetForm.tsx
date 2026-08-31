'use client'

import { useMemo, useState } from 'react'
import type { Budget, BudgetInput, BudgetPeriod } from '@/types/budget'
import type { Category } from '@/types/transaction'
import { X } from 'lucide-react'

const PERIODS: BudgetPeriod[] = ['WEEKLY', 'MONTHLY', 'YEARLY']

interface Props {
  categories: Category[]
  /** Categories that already have a budget for the selected period. */
  existing: Budget[]
  editing: Budget | null
  saving: boolean
  error: string | null
  onSubmit: (input: BudgetInput) => void
  onCancel: () => void
}

export function BudgetForm({ categories, existing, editing, saving, error, onSubmit, onCancel }: Props) {
  const [categoryId, setCategoryId] = useState<string>(editing?.categoryId ?? '')
  const [limitAmount, setLimitAmount] = useState<string>(
    editing ? String(editing.limitAmount) : ''
  )
  const [period, setPeriod] = useState<BudgetPeriod>(editing?.period ?? 'MONTHLY')
  const [alertThreshold, setAlertThreshold] = useState<string>(
    String(editing?.alertThreshold ?? 80)
  )

  // Prevent creating a second budget for a category that already has one.
  const takenCategoryIds = useMemo(() => {
    return new Set(
      existing
        .filter((b) => b.period === period && b.id !== editing?.id)
        .map((b) => b.categoryId ?? '')
    )
  }, [existing, period, editing])

  const amount = Number(limitAmount)
  const threshold = Number(alertThreshold)
  const valid =
    Number.isFinite(amount) &&
    amount > 0 &&
    Number.isFinite(threshold) &&
    threshold >= 1 &&
    threshold <= 100 &&
    !takenCategoryIds.has(categoryId)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!valid || saving) return
    onSubmit({
      categoryId: categoryId === '' ? null : categoryId,
      limitAmount: amount,
      period,
      alertThreshold: threshold,
    })
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-xl border border-border bg-card p-5 space-y-4"
    >
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold">
          {editing ? 'Edit budget' : 'New budget'}
        </h3>
        <button
          type="button"
          onClick={onCancel}
          aria-label="Close budget form"
          className="rounded-md p-1 text-muted-foreground hover:bg-accent hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <label className="space-y-1.5 text-sm">
          <span className="font-medium">Applies to</span>
          <select
            value={categoryId}
            onChange={(e) => setCategoryId(e.target.value)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          >
            <option value="">All spending</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id} disabled={takenCategoryIds.has(c.id)}>
                {c.displayName}
                {takenCategoryIds.has(c.id) ? ' — already budgeted' : ''}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-1.5 text-sm">
          <span className="font-medium">Resets</span>
          <select
            value={period}
            onChange={(e) => setPeriod(e.target.value as BudgetPeriod)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          >
            {PERIODS.map((p) => (
              <option key={p} value={p}>
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </option>
            ))}
          </select>
        </label>

        <label className="space-y-1.5 text-sm">
          <span className="font-medium">Limit (₹)</span>
          <input
            type="number"
            min="1"
            step="0.01"
            required
            value={limitAmount}
            onChange={(e) => setLimitAmount(e.target.value)}
            placeholder="15000"
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          />
        </label>

        <label className="space-y-1.5 text-sm">
          <span className="font-medium">Warn me at (%)</span>
          <input
            type="number"
            min="1"
            max="100"
            required
            value={alertThreshold}
            onChange={(e) => setAlertThreshold(e.target.value)}
            className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
          />
        </label>
      </div>

      {takenCategoryIds.has(categoryId) && (
        <p className="text-xs text-yellow-500">
          A {period.toLowerCase()} budget already exists for this category. Edit it instead.
        </p>
      )}

      {error && <p className="text-xs text-destructive">{error}</p>}

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={!valid || saving}
          className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
        >
          {saving ? 'Saving…' : editing ? 'Save changes' : 'Create budget'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-accent"
        >
          Cancel
        </button>
      </div>
    </form>
  )
}
