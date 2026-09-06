'use client'

import { useState } from 'react'
import { cardsApi } from '@/lib/api/cards'
import type { Card } from '@/types/card'

interface Props {
  initialData?: Card | null
  onCreated: (card: Card) => void
  onCancel: () => void
}

export function AddCardForm({ initialData, onCreated, onCancel }: Props) {
  const [name, setName] = useState(initialData?.name || '')
  const [issuer, setIssuer] = useState(initialData?.issuer || '')
  const [lastFourDigits, setLastFourDigits] = useState(initialData?.lastFourDigits || '')
  const [creditLimit, setCreditLimit] = useState(initialData?.creditLimit ? String(initialData.creditLimit) : '')
  const [billingCycleDay, setBillingCycleDay] = useState(initialData?.billingCycleDay ? String(initialData.billingCycleDay) : '')
  const [paymentDueDay, setPaymentDueDay] = useState(initialData?.paymentDueDay ? String(initialData.paymentDueDay) : '')
  const [statementPassword, setStatementPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const payload: Omit<Card, 'id' | 'userId' | 'isActive' | 'createdAt'> = {
        name,
        issuer: issuer || null,
        lastFourDigits: lastFourDigits || null,
        creditLimit: creditLimit ? parseFloat(creditLimit) : null,
        billingCycleDay: billingCycleDay ? parseInt(billingCycleDay) : null,
        paymentDueDay: paymentDueDay ? parseInt(paymentDueDay) : null,
        ...(statementPassword ? { statementPassword } : {}),
      }
      
      let card;
      if (initialData) {
        card = await cardsApi.update(initialData.id, payload)
      } else {
        card = await cardsApi.create(payload)
      }
      onCreated(card)
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${initialData ? 'update' : 'create'} card`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium mb-1">Card Name *</label>
        <input
          required
          value={name}
          onChange={e => setName(e.target.value)}
          placeholder="SBI Cashback Card"
          className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-1">Issuer</label>
          <input
            value={issuer}
            onChange={e => setIssuer(e.target.value)}
            placeholder="SBI"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Last 4 Digits</label>
          <input
            value={lastFourDigits}
            onChange={e => setLastFourDigits(e.target.value)}
            placeholder="4821"
            maxLength={4}
            pattern="\d{4}"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
      </div>

      <div>
        <label className="block text-sm font-medium mb-1">Credit Limit (₹)</label>
        <input
          type="number"
          min="0"
          step="0.01"
          value={creditLimit}
          onChange={e => setCreditLimit(e.target.value)}
          placeholder="100000"
          className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium mb-1">Billing Cycle Day</label>
          <input
            type="number"
            min="1"
            max="31"
            value={billingCycleDay}
            onChange={e => setBillingCycleDay(e.target.value)}
            placeholder="5"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">Payment Due Day</label>
          <input
            type="number"
            min="1"
            max="31"
            value={paymentDueDay}
            onChange={e => setPaymentDueDay(e.target.value)}
            placeholder="15"
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between mb-1">
          <label className="block text-sm font-medium">Statement Password (Optional)</label>
          {initialData?.hasStatementPassword && (
            <span className="text-[11px] font-semibold text-emerald-400">Password Saved</span>
          )}
        </div>
        <input
          type="password"
          value={statementPassword}
          onChange={e => setStatementPassword(e.target.value)}
          placeholder={initialData?.hasStatementPassword ? '•••••••• (leave blank to keep)' : 'e.g. DOB or PAN for auto-unlocking PDFs'}
          className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
        />
        <p className="text-[11px] text-muted-foreground mt-1">
          Auto-unlocks password-protected credit card statements uploaded for this card.
        </p>
      </div>

      {error && (
        <p className="text-sm text-destructive">{error}</p>
      )}

      <div className="flex justify-end gap-3 pt-2">
        <button
          type="button"
          onClick={onCancel}
          className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={loading}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
        >
          {loading ? (initialData ? 'Updating…' : 'Adding…') : (initialData ? 'Update Card' : 'Add Card')}
        </button>
      </div>
    </form>
  )
}
