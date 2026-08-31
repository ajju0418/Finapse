'use client'

import { useState } from 'react'
import { authApi } from '@/lib/api/auth'
import { KeyRound } from 'lucide-react'

/**
 * Changing the password revokes every other session server-side, so the user is
 * told that explicitly rather than being surprised by a sign-out elsewhere.
 */
export function ChangePasswordForm() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  const strongEnough =
    newPassword.length >= 10 &&
    /[a-z]/.test(newPassword) &&
    /[A-Z]/.test(newPassword) &&
    /\d/.test(newPassword)
  const matches = newPassword !== '' && newPassword === confirmPassword
  const valid = currentPassword !== '' && strongEnough && matches

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!valid || saving) return

    setSaving(true)
    setError(null)
    setDone(false)
    try {
      await authApi.changePassword({ currentPassword, newPassword })
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setDone(true)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not change your password.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="mt-10 border-t border-border pt-8">
      <h2 className="mb-4 text-base font-semibold">Password</h2>

      <form onSubmit={handleSubmit} className="space-y-4 rounded-xl border border-border bg-card p-5">
        <p className="flex items-start gap-2 text-xs text-muted-foreground">
          <KeyRound className="mt-0.5 h-3.5 w-3.5 shrink-0" />
          Changing your password signs you out everywhere else. This device stays signed in.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <label className="space-y-1.5 text-sm sm:col-span-2">
            <span className="text-xs font-medium">Current password</span>
            <input
              type="password"
              required
              autoComplete="current-password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </label>

          <label className="space-y-1.5 text-sm">
            <span className="text-xs font-medium">New password</span>
            <input
              type="password"
              required
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </label>

          <label className="space-y-1.5 text-sm">
            <span className="text-xs font-medium">Confirm new password</span>
            <input
              type="password"
              required
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            />
          </label>
        </div>

        {newPassword !== '' && !strongEnough && (
          <p className="text-xs text-yellow-500">
            Use at least 10 characters with an uppercase letter, a lowercase letter and a number.
          </p>
        )}
        {confirmPassword !== '' && !matches && (
          <p className="text-xs text-yellow-500">The two new passwords do not match.</p>
        )}
        {error && <p className="text-xs text-destructive">{error}</p>}
        {done && <p className="text-xs text-primary">Password updated. Other sessions were signed out.</p>}

        <button
          type="submit"
          disabled={!valid || saving}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          {saving ? 'Updating…' : 'Change password'}
        </button>
      </form>
    </section>
  )
}
