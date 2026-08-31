'use client'

import { useCallback, useEffect, useState } from 'react'
import { Plus, Wallet } from 'lucide-react'
import { budgetsApi } from '@/lib/api/budgets'
import { categoriesApi } from '@/lib/api/transactions'
import type { Budget, BudgetInput } from '@/types/budget'
import type { Category } from '@/types/transaction'
import { BudgetCard } from '@/components/budgets/BudgetCard'
import { BudgetForm } from '@/components/budgets/BudgetForm'
import { Skeleton } from '@/components/ui/skeleton'
import { formatCurrency } from '@/lib/utils/format'

export default function BudgetsPage() {
  const [budgets, setBudgets] = useState<Budget[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Budget | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    Promise.all([budgetsApi.getAll(), categoriesApi.getAll()])
      .then(([b, c]) => {
        setBudgets(b)
        setCategories(c)
        setError(null)
      })
      .catch(() => setError('Could not load budgets. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [])

  useEffect(load, [load])

  async function handleSubmit(input: BudgetInput) {
    setSaving(true)
    setFormError(null)
    try {
      const saved = editing
        ? await budgetsApi.update(editing.id, input)
        : await budgetsApi.create(input)

      setBudgets((prev) =>
        editing ? prev.map((b) => (b.id === saved.id ? saved : b)) : [...prev, saved]
      )
      setShowForm(false)
      setEditing(null)
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Could not save the budget.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(budget: Budget) {
    const label = budget.categoryName ?? 'All spending'
    if (!window.confirm(`Delete the budget for ${label}? Your transactions are not affected.`)) {
      return
    }
    const previous = budgets
    setBudgets((prev) => prev.filter((b) => b.id !== budget.id))
    try {
      await budgetsApi.delete(budget.id)
    } catch {
      setBudgets(previous)
      setError('Could not delete that budget. Please try again.')
    }
  }

  const totalLimit = budgets.reduce((sum, b) => sum + b.limitAmount, 0)
  const totalSpent = budgets.reduce((sum, b) => sum + b.spent, 0)
  const overBudget = budgets.filter((b) => b.status === 'EXCEEDED').length

  return (
    <div className="min-h-screen space-y-6 p-6 md:p-8">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Budgets</h1>
          <p className="mt-0.5 text-xs text-muted-foreground">
            Caps you set for yourself. Spending is measured as expenses minus refunds.
          </p>
        </div>
        {!showForm && (
          <button
            onClick={() => {
              setEditing(null)
              setFormError(null)
              setShowForm(true)
            }}
            className="flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
          >
            <Plus className="h-4 w-4" />
            New budget
          </button>
        )}
      </div>

      {error && (
        <div className="rounded-xl border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {showForm && (
        <BudgetForm
          categories={categories}
          existing={budgets}
          editing={editing}
          saving={saving}
          error={formError}
          onSubmit={handleSubmit}
          onCancel={() => {
            setShowForm(false)
            setEditing(null)
            setFormError(null)
          }}
        />
      )}

      {loading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-44 rounded-xl" />
          ))}
        </div>
      )}

      {!loading && budgets.length > 0 && (
        <>
          <div className="grid gap-4 sm:grid-cols-3">
            <SummaryTile label="Total budgeted" value={formatCurrency(totalLimit)} />
            <SummaryTile label="Spent so far" value={formatCurrency(totalSpent)} />
            <SummaryTile
              label="Over budget"
              value={`${overBudget} of ${budgets.length}`}
              tone={overBudget > 0 ? 'text-destructive' : undefined}
            />
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {budgets.map((budget) => (
              <BudgetCard
                key={budget.id}
                budget={budget}
                onEdit={(b) => {
                  setEditing(b)
                  setFormError(null)
                  setShowForm(true)
                }}
                onDelete={handleDelete}
              />
            ))}
          </div>
        </>
      )}

      {!loading && budgets.length === 0 && !showForm && (
        <div className="flex flex-col items-center rounded-xl border border-dashed border-border p-12 text-center">
          <Wallet className="h-8 w-8 text-muted-foreground" />
          <h2 className="mt-3 text-sm font-semibold">No budgets yet</h2>
          <p className="mt-1 max-w-sm text-xs text-muted-foreground">
            Set a monthly cap for a category — groceries or dining are a good start — and Finapse
            will track it against your imported transactions.
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
          >
            Create your first budget
          </button>
        </div>
      )}
    </div>
  )
}

function SummaryTile({ label, value, tone }: { label: string; value: string; tone?: string }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <p className="text-xs font-semibold uppercase tracking-widest text-muted-foreground">
        {label}
      </p>
      <p className={`mt-1 text-xl font-bold tracking-tight ${tone ?? ''}`}>{value}</p>
    </div>
  )
}
