'use client'

import { useEffect, useState } from 'react'
import { accountsApi } from '@/lib/api/accounts'
import type { Account } from '@/types/account'
import { Building2, Plus, X } from 'lucide-react'
import { Skeleton } from '@/components/ui/skeleton'
import { AppShell } from '@/components/layout/AppShell'

export default function SettingsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Form state
  const [name, setName] = useState('')
  const [institution, setInstitution] = useState('')
  const [lastFour, setLastFour] = useState('')
  const [currency, setCurrency] = useState('INR')
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  useEffect(() => {
    accountsApi.getAll()
      .then(setAccounts)
      .catch(() => setError('Could not load accounts. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSaving(true)
    try {
      const account = await accountsApi.create({ name, institutionName: institution || null, lastFourDigits: lastFour || null, currency })
      setAccounts(prev => [account, ...prev])
      setShowForm(false)
      setName(''); setInstitution(''); setLastFour(''); setCurrency('INR')
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Failed to create account')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="p-8 max-w-2xl">
        <div className="mb-8">
          <h1 className="text-2xl font-bold">Settings</h1>
          <p className="text-sm text-muted-foreground mt-1">Manage your bank accounts and preferences</p>
        </div>

        {/* Bank Accounts section */}
        <section>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-base font-semibold">Bank Accounts</h2>
            <button
              onClick={() => setShowForm(v => !v)}
              className="flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-accent transition-colors"
            >
              <Plus className="h-3.5 w-3.5" />
              Add Account
            </button>
          </div>

          {/* Add account form */}
          {showForm && (
            <form onSubmit={handleAdd} className="mb-4 rounded-xl border border-border bg-card p-5 space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium">New Bank Account</p>
                <button type="button" onClick={() => setShowForm(false)}>
                  <X className="h-4 w-4 text-muted-foreground" />
                </button>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="col-span-2">
                  <label className="block text-xs font-medium mb-1">Account Name *</label>
                  <input required value={name} onChange={e => setName(e.target.value)}
                    placeholder="HDFC Savings"
                    className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="block text-xs font-medium mb-1">Bank / Institution</label>
                  <input value={institution} onChange={e => setInstitution(e.target.value)}
                    placeholder="HDFC Bank"
                    className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="block text-xs font-medium mb-1">Last 4 Digits</label>
                  <input value={lastFour} onChange={e => setLastFour(e.target.value)}
                    placeholder="1234" maxLength={4} pattern="\\d{4}"
                    className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring" />
                </div>
                <div>
                  <label className="block text-xs font-medium mb-1">Currency</label>
                  <select value={currency} onChange={e => setCurrency(e.target.value)}
                    className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring">
                    <option value="INR">INR</option>
                    <option value="USD">USD</option>
                    <option value="EUR">EUR</option>
                    <option value="GBP">GBP</option>
                  </select>
                </div>
              </div>
              {formError && <p className="text-xs text-destructive">{formError}</p>}
              <div className="flex justify-end gap-3">
                <button type="button" onClick={() => setShowForm(false)}
                  className="rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors">
                  Cancel
                </button>
                <button type="submit" disabled={saving}
                  className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50 transition-colors">
                  {saving ? 'Saving…' : 'Add Account'}
                </button>
              </div>
            </form>
          )}

          {/* Error */}
          {error && (
            <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive mb-4">
              {error}
            </div>
          )}

          {/* Loading */}
          {loading && (
            <div className="space-y-2">
              {[1, 2].map(i => <Skeleton key={i} className="h-16 rounded-xl" />)}
            </div>
          )}

          {/* Empty */}
          {!loading && !error && accounts.length === 0 && (
            <div className="rounded-xl border border-dashed border-border p-8 text-center">
              <Building2 className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
              <p className="text-sm text-muted-foreground">No bank accounts added yet.</p>
              <p className="text-xs text-muted-foreground mt-1">Add an account to start importing bank statements.</p>
            </div>
          )}

          {/* Account list */}
          {!loading && accounts.length > 0 && (
            <div className="space-y-2">
              {accounts.map(account => (
                <div key={account.id} className="flex items-center gap-4 rounded-xl border border-border bg-card px-5 py-4">
                  <div className="flex h-9 w-9 items-center justify-center rounded-full bg-muted">
                    <Building2 className="h-4 w-4 text-muted-foreground" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium">{account.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {[account.institutionName, account.lastFourDigits ? `•••• ${account.lastFourDigits}` : null, account.currency]
                        .filter(Boolean).join(' · ')}
                    </p>
                  </div>
                  {!account.isActive && (
                    <span className="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded-full">Inactive</span>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>

        {/* App info */}
        <section className="mt-10 pt-8 border-t border-border">
          <h2 className="text-base font-semibold mb-4">About</h2>
          <div className="rounded-xl border border-border bg-card p-5 space-y-2 text-sm text-muted-foreground">
            <p><span className="font-medium text-foreground">Finapse</span> — Privacy-first personal finance intelligence</p>
            <p>All data is stored locally in your MySQL database. Nothing leaves your laptop.</p>
            <p className="text-xs">Backend: <code className="bg-muted px-1 rounded">http://localhost:8080</code></p>
          </div>
        </section>
      </div>
  )
}
