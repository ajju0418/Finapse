'use client'

import { useEffect, useState } from 'react'
import { transactionsApi, categoriesApi } from '@/lib/api/transactions'
import { accountsApi } from '@/lib/api/accounts'
import { cardsApi } from '@/lib/api/cards'
import type { Account } from '@/types/account'
import type { Card } from '@/types/card'
import type { Category, Transaction, TransactionType } from '@/types/transaction'
import { X } from 'lucide-react'

/** Types a person would realistically enter by hand. */
const TYPES: { value: TransactionType; label: string; direction: 'DEBIT' | 'CREDIT' }[] = [
  { value: 'EXPENSE', label: 'Expense', direction: 'DEBIT' },
  { value: 'INCOME', label: 'Income', direction: 'CREDIT' },
  { value: 'REFUND', label: 'Refund received', direction: 'CREDIT' },
  { value: 'CASHBACK', label: 'Cashback', direction: 'CREDIT' },
  { value: 'FEE', label: 'Fee or charge', direction: 'DEBIT' },
  { value: 'TRANSFER', label: 'Transfer', direction: 'DEBIT' },
]

const today = () => new Date().toISOString().slice(0, 10)

interface Props {
  onCreated: (transaction: Transaction) => void
  onCancel: () => void
}

export function AddTransactionForm({ onCreated, onCancel }: Props) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [categories, setCategories] = useState<Category[]>([])

  const [source, setSource] = useState('')
  const [date, setDate] = useState(today())
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('')
  const [type, setType] = useState<TransactionType>('EXPENSE')
  const [categoryId, setCategoryId] = useState('')

  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    Promise.all([accountsApi.getAll(), cardsApi.getAll(), categoriesApi.getAll()])
      .then(([a, c, cat]) => {
        setAccounts(a)
        setCards(c)
        setCategories(cat)
        const first = a[0] ?? c[0]
        if (first) setSource(a[0] ? `account:${a[0].id}` : `card:${c[0].id}`)
      })
      .catch(() => setError('Could not load your accounts and cards.'))
  }, [])

  const numericAmount = Number(amount)
  const valid =
    source !== '' &&
    description.trim() !== '' &&
    Number.isFinite(numericAmount) &&
    numericAmount > 0 &&
    date !== '' &&
    date <= today()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!valid || saving) return

    const [kind, id] = source.split(':')
    const direction = TYPES.find((t) => t.value === type)?.direction ?? 'DEBIT'

    setSaving(true)
    setError(null)
    try {
      const created = await transactionsApi.createManual({
        transactionDate: date,
        description: description.trim(),
        amount: numericAmount,
        direction,
        transactionType: type,
        categoryId: categoryId === '' ? null : categoryId,
        accountId: kind === 'account' ? id : null,
        cardId: kind === 'card' ? id : null,
      })
      onCreated(created)
      setDescription('')
      setAmount('')
      setCategoryId('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save the transaction.')
    } finally {
      setSaving(false)
    }
  }

  const noSources = accounts.length === 0 && cards.length === 0

  return (
    <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-border bg-card p-5">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold">Add a transaction</h3>
          <p className="mt-0.5 text-xs text-muted-foreground">
            For cash spending and anything else your statements do not show.
          </p>
        </div>
        <button
          type="button"
          onClick={onCancel}
          aria-label="Close form"
          className="rounded-md p-1 text-muted-foreground hover:bg-accent hover:text-foreground"
        >
          <X className="h-4 w-4" />
        </button>
      </div>

      {noSources ? (
        <p className="text-xs text-yellow-500">
          Add a bank account or a card first — every transaction has to belong to one.
        </p>
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="space-y-1.5 text-sm">
              <span className="font-medium">Source</span>
              <select
                value={source}
                onChange={(e) => setSource(e.target.value)}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                {accounts.map((a) => (
                  <option key={a.id} value={`account:${a.id}`}>
                    {a.name}
                  </option>
                ))}
                {cards.map((c) => (
                  <option key={c.id} value={`card:${c.id}`}>
                    {c.name} (card)
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-1.5 text-sm">
              <span className="font-medium">Date</span>
              <input
                type="date"
                required
                max={today()}
                value={date}
                onChange={(e) => setDate(e.target.value)}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              />
            </label>

            <label className="space-y-1.5 text-sm sm:col-span-2">
              <span className="font-medium">Description</span>
              <input
                type="text"
                required
                maxLength={500}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Auto fare, vegetables from the market…"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              />
            </label>

            <label className="space-y-1.5 text-sm">
              <span className="font-medium">Amount (₹)</span>
              <input
                type="number"
                required
                min="0.01"
                step="0.01"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                placeholder="250"
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              />
            </label>

            <label className="space-y-1.5 text-sm">
              <span className="font-medium">Type</span>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as TransactionType)}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                {TYPES.map((t) => (
                  <option key={t.value} value={t.value}>
                    {t.label}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-1.5 text-sm sm:col-span-2">
              <span className="font-medium">Category (optional)</span>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
              >
                <option value="">Uncategorised</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.displayName}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {error && <p className="text-xs text-destructive">{error}</p>}

          <div className="flex gap-2">
            <button
              type="submit"
              disabled={!valid || saving}
              className="rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground disabled:opacity-50"
            >
              {saving ? 'Saving…' : 'Add transaction'}
            </button>
            <button
              type="button"
              onClick={onCancel}
              className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-accent"
            >
              Cancel
            </button>
          </div>
        </>
      )}
    </form>
  )
}
