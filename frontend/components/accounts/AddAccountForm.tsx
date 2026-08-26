'use client'

import { useState } from 'react'
import { accountsApi } from '@/lib/api/accounts'
import type { Account } from '@/types/account'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

interface Props {
  initialData?: Account | null
  onCreated: (account: Account) => void
  onCancel: () => void
}

export function AddAccountForm({ initialData, onCreated, onCancel }: Props) {
  const [loading, setLoading] = useState(false)
  const [formData, setFormData] = useState({
    name: initialData?.name || '',
    institutionName: initialData?.institutionName || '',
    lastFourDigits: initialData?.lastFourDigits || '',
    currency: initialData?.currency || 'INR'
  })

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setLoading(true)
    try {
      let account;
      if (initialData) {
        account = await accountsApi.update(initialData.id, formData)
      } else {
        account = await accountsApi.create(formData)
      }
      onCreated(account)
    } catch (err) {
      alert(`Failed to ${initialData ? 'update' : 'create'} account. Please try again.`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="name">Account Nickname</Label>
        <Input
          id="name"
          placeholder="e.g. Primary Savings"
          value={formData.name}
          onChange={e => setFormData({ ...formData, name: e.target.value })}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="institution">Bank Name</Label>
        <Input
          id="institution"
          placeholder="e.g. HDFC Bank"
          value={formData.institutionName}
          onChange={e => setFormData({ ...formData, institutionName: e.target.value })}
          required
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="digits">Last 4 Digits</Label>
        <Input
          id="digits"
          placeholder="1234"
          maxLength={4}
          value={formData.lastFourDigits}
          onChange={e => setFormData({ ...formData, lastFourDigits: e.target.value })}
        />
      </div>

      <div className="flex gap-3 pt-4">
        <Button type="button" variant="outline" className="flex-1" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" className="flex-1" disabled={loading}>
          {loading ? (initialData ? 'Updating...' : 'Creating...') : (initialData ? 'Update Account' : 'Create Account')}
        </Button>
      </div>
    </form>
  )
}
