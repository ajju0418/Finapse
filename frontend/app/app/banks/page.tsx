'use client'

import { useEffect, useState } from 'react'
import { accountsApi } from '@/lib/api/accounts'
import type { Account } from '@/types/account'
import { AccountAnalyticsResponse } from '@/types/account' // I'll need to create this type
import { FinancialSourceCard } from '@/components/financial/FinancialSourceCard'
import { AddAccountForm } from '@/components/accounts/AddAccountForm' // I'll need to create this
import { Skeleton } from '@/components/ui/skeleton'
import { X } from 'lucide-react'
import { formatCurrency } from '@/lib/utils/format'

export default function BanksPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [analytics, setAnalytics] = useState<Record<string, AccountAnalyticsResponse>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

  useEffect(() => {
    async function loadData() {
      try {
        const data = await accountsApi.getAll()
        setAccounts(data)

        const analyticsData: Record<string, AccountAnalyticsResponse> = {}
        await Promise.all(
          data.map(async (acc) => {
            const res = await accountsApi.getAnalytics(acc.id)
            analyticsData[acc.id] = res
          })
        )
        setAnalytics(analyticsData)
      } catch (err) {
        setError('Could not load bank accounts. Make sure the backend is running.')
      } finally {
        setLoading(false)
      }
    }
    loadData()
  }, [])

  function handleAccountCreated(account: Account) {
    setAccounts(prev => [account, ...prev])
    setShowForm(false)
  }

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">My Banks</h1>
          <p className="text-sm text-muted-foreground mt-1">Manage your linked bank accounts</p>
        </div>
        <button
          onClick={() => setShowForm(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          + Add Bank Account
        </button>
      </div>

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-md rounded-xl border border-border bg-background p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold">Add Bank Account</h2>
              <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
                <X className="h-4 w-4" />
              </button>
            </div>
            <AddAccountForm onCreated={handleAccountCreated} onCancel={() => setShowForm(false)} />
          </div>
        </div>
      )}

      {loading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map(i => <Skeleton key={i} className="h-48 w-full rounded-xl" />)}
        </div>
      )}

      {error && (
        <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {!loading && !error && accounts.length === 0 && (
        <div className="rounded-xl border border-dashed border-border p-16 text-center">
          <p className="text-muted-foreground font-medium">No bank accounts added yet.</p>
          <p className="text-sm text-muted-foreground mt-1">
            Add a bank account to start tracking your statements and spending.
          </p>
          <button
            onClick={() => setShowForm(true)}
            className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Add Bank Account
          </button>
        </div>
      )}

      {!loading && !error && accounts.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {accounts.map(acc => {
            const analytic = analytics[acc.id]
            return (
              <FinancialSourceCard
                key={acc.id}
                source={{
                  name: acc.name,
                  institution: acc.institutionName || 'Unknown Bank',
                  currentBalance: analytic ? formatCurrency(analytic.netChange) : '---',
                  totalSpending: analytic ? formatCurrency(analytic.totalOutflow) : '---',
                  isCard: false
                }}
              />
            )
          })}
        </div>
      )}
    </div>
  )
}
