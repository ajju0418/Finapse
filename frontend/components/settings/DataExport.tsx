'use client'

import { useState } from 'react'
import { exportApi } from '@/lib/api/export'
import { Download } from 'lucide-react'

/** Your data should never be trapped in the product. */
export function DataExport() {
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const partialRange = (from === '') !== (to === '')
  const invalidRange = from !== '' && to !== '' && from > to

  async function handleDownload() {
    if (busy || partialRange || invalidRange) return
    setBusy(true)
    setError(null)
    try {
      await exportApi.downloadTransactionsCsv(
        from !== '' && to !== '' ? { from, to } : undefined
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'The export failed. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="mt-10 border-t border-border pt-8">
      <h2 className="mb-4 text-base font-semibold">Export your data</h2>

      <div className="space-y-4 rounded-xl border border-border bg-card p-5">
        <p className="text-xs text-muted-foreground">
          Downloads every transaction as CSV, including category, merchant and why Finapse
          classified it the way it did. Leave the dates empty to export everything.
        </p>

        <div className="flex flex-wrap items-end gap-3">
          <label className="space-y-1 text-xs">
            <span className="text-muted-foreground">From</span>
            <input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(e) => setFrom(e.target.value)}
              className="block rounded-md border border-border bg-background px-3 py-2 text-sm"
            />
          </label>
          <label className="space-y-1 text-xs">
            <span className="text-muted-foreground">To</span>
            <input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(e) => setTo(e.target.value)}
              className="block rounded-md border border-border bg-background px-3 py-2 text-sm"
            />
          </label>
          <button
            onClick={handleDownload}
            disabled={busy || partialRange || invalidRange}
            className="flex items-center gap-1.5 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
          >
            <Download className="h-3.5 w-3.5" />
            {busy ? 'Preparing…' : 'Download CSV'}
          </button>
        </div>

        {partialRange && (
          <p className="text-xs text-yellow-500">Set both dates, or clear both to export everything.</p>
        )}
        {invalidRange && (
          <p className="text-xs text-destructive">The start date must come before the end date.</p>
        )}
        {error && <p className="text-xs text-destructive">{error}</p>}
      </div>
    </section>
  )
}
