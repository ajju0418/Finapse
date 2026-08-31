'use client'

import { useState } from 'react'
import type { DashboardPeriod, DateRange } from '@/types/dashboard'
import { CalendarRange } from 'lucide-react'

const PRESETS: { value: DashboardPeriod; label: string }[] = [
  { value: 'THIS_MONTH', label: 'Month' },
  { value: 'LAST_MONTH', label: 'Last M' },
  { value: '7_DAYS', label: '7D' },
  { value: '30_DAYS', label: '30D' },
  { value: '3_MONTHS', label: '3M' },
  { value: '6_MONTHS', label: '6M' },
  { value: 'YTD', label: 'YTD' },
  { value: '1_YEAR', label: '1Y' },
]

interface Props {
  period: DashboardPeriod
  range: DateRange | undefined
  onChange: (period: DashboardPeriod, range?: DateRange) => void
}

export function PeriodSelector({ period, range, onChange }: Props) {
  const [open, setOpen] = useState(period === 'CUSTOM')
  const [from, setFrom] = useState(range?.from ?? '')
  const [to, setTo] = useState(range?.to ?? '')

  const rangeInvalid = from !== '' && to !== '' && from > to

  function applyCustom() {
    if (from === '' || to === '' || rangeInvalid) return
    onChange('CUSTOM', { from, to })
  }

  return (
    <div className="flex flex-col items-end gap-2">
      <div className="flex flex-wrap gap-1 rounded-xl border border-border bg-muted/30 p-1">
        {PRESETS.map((p) => (
          <button
            key={p.value}
            onClick={() => {
              setOpen(false)
              onChange(p.value)
            }}
            className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
              period === p.value
                ? 'bg-primary text-primary-foreground shadow'
                : 'text-muted-foreground hover:text-foreground'
            }`}
          >
            {p.label}
          </button>
        ))}
        <button
          onClick={() => setOpen((v) => !v)}
          aria-expanded={open}
          className={`flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all ${
            period === 'CUSTOM'
              ? 'bg-primary text-primary-foreground shadow'
              : 'text-muted-foreground hover:text-foreground'
          }`}
        >
          <CalendarRange className="h-3.5 w-3.5" />
          Custom
        </button>
      </div>

      {open && (
        <div className="flex flex-wrap items-end gap-2 rounded-xl border border-border bg-card p-3">
          <label className="space-y-1 text-xs">
            <span className="text-muted-foreground">From</span>
            <input
              type="date"
              value={from}
              max={to || undefined}
              onChange={(e) => setFrom(e.target.value)}
              className="block rounded-lg border border-border bg-background px-2 py-1.5 text-xs"
            />
          </label>
          <label className="space-y-1 text-xs">
            <span className="text-muted-foreground">To</span>
            <input
              type="date"
              value={to}
              min={from || undefined}
              onChange={(e) => setTo(e.target.value)}
              className="block rounded-lg border border-border bg-background px-2 py-1.5 text-xs"
            />
          </label>
          <button
            onClick={applyCustom}
            disabled={from === '' || to === '' || rangeInvalid}
            className="rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-primary-foreground disabled:opacity-50"
          >
            Apply
          </button>
          {rangeInvalid && (
            <p className="w-full text-xs text-destructive">
              The start date must come before the end date.
            </p>
          )}
        </div>
      )}
    </div>
  )
}
