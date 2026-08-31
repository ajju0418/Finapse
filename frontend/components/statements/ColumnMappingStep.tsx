'use client'

import { useState } from 'react'
import type { ColumnMapping, StatementPreview } from '@/types/statement'
import { AlertTriangle, CheckCircle2 } from 'lucide-react'

const FIELDS: { key: keyof ColumnMapping; label: string; hint: string; required: boolean }[] = [
  { key: 'dateColumn', label: 'Date', hint: 'When the transaction happened', required: true },
  { key: 'descriptionColumn', label: 'Description', hint: 'Narration or particulars', required: true },
  { key: 'debitColumn', label: 'Debit', hint: 'Money out', required: false },
  { key: 'creditColumn', label: 'Credit', hint: 'Money in', required: false },
  { key: 'amountColumn', label: 'Amount', hint: 'Single signed column', required: false },
  { key: 'postedDateColumn', label: 'Posted date', hint: 'Optional', required: false },
]

interface Props {
  preview: StatementPreview
  busy: boolean
  onRemap: (mapping: ColumnMapping) => void
  onConfirm: (mapping: ColumnMapping) => void
  onBack: () => void
}

/**
 * Lets the user correct Finapse's automatic column detection. Nothing has been
 * saved at this point — remapping simply re-runs the dry-run parse.
 */
export function ColumnMappingStep({ preview, busy, onRemap, onConfirm, onBack }: Props) {
  const [mapping, setMapping] = useState<ColumnMapping>(preview.detectedMapping)

  const hasAmount =
    (mapping.debitColumn != null && mapping.creditColumn != null) || mapping.amountColumn != null
  const complete = mapping.dateColumn != null && mapping.descriptionColumn != null && hasAmount

  function update(key: keyof ColumnMapping, value: string) {
    setMapping((prev) => ({ ...prev, [key]: value === '' ? null : value }))
  }

  const canRemap = preview.availableColumns.length > 0

  return (
    <div className="space-y-4">
      <div
        className={`flex items-start gap-2 rounded-lg border p-3 text-sm ${
          preview.mappingComplete
            ? 'border-primary/40 bg-primary/5 text-foreground'
            : 'border-destructive/40 bg-destructive/10 text-destructive'
        }`}
      >
        {preview.mappingComplete ? (
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
        ) : (
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
        )}
        <p>{preview.message}</p>
      </div>

      {canRemap && (
        <details className="rounded-lg border border-border" open={!preview.mappingComplete}>
          <summary className="cursor-pointer px-4 py-3 text-sm font-medium">
            Column mapping
          </summary>
          <div className="space-y-3 border-t border-border px-4 py-3">
            <p className="text-xs text-muted-foreground">
              Use either a Debit + Credit pair, or a single signed Amount column.
            </p>
            {FIELDS.map((field) => (
              <label key={field.key} className="flex items-center gap-3 text-sm">
                <span className="w-28 shrink-0">
                  {field.label}
                  {field.required && <span className="text-destructive"> *</span>}
                </span>
                <select
                  value={mapping[field.key] ?? ''}
                  onChange={(e) => update(field.key, e.target.value)}
                  className="flex-1 rounded-md border border-border bg-background px-2 py-1.5 text-xs"
                >
                  <option value="">— not used —</option>
                  {preview.availableColumns.map((col) => (
                    <option key={col} value={col}>
                      {col}
                    </option>
                  ))}
                </select>
              </label>
            ))}
            <button
              type="button"
              onClick={() => onRemap(mapping)}
              disabled={!complete || busy}
              className="rounded-md border border-border px-3 py-1.5 text-xs font-medium hover:bg-accent disabled:opacity-50"
            >
              {busy ? 'Checking…' : 'Re-check with this mapping'}
            </button>
          </div>
        </details>
      )}

      {preview.sampleRows.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-border">
          <table className="w-full text-xs">
            <thead className="bg-muted/40 text-left">
              <tr>
                <th className="px-3 py-2 font-medium">Date</th>
                <th className="px-3 py-2 font-medium">Description</th>
                <th className="px-3 py-2 text-right font-medium">Amount</th>
                <th className="px-3 py-2 font-medium">Dir</th>
              </tr>
            </thead>
            <tbody>
              {preview.sampleRows.map((row) => (
                <tr key={row.sourceRowNumber} className="border-t border-border">
                  <td className="whitespace-nowrap px-3 py-2">{row.date}</td>
                  <td className="max-w-[16rem] truncate px-3 py-2">{row.description}</td>
                  <td className="whitespace-nowrap px-3 py-2 text-right font-medium">{row.amount}</td>
                  <td className="px-3 py-2 text-muted-foreground">{row.direction}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {preview.invalidRowCount > 0 && (
        <div className="rounded-lg border border-yellow-500/40 bg-yellow-500/10 p-3 text-xs text-yellow-600">
          <p className="font-medium">
            {preview.invalidRowCount} row{preview.invalidRowCount === 1 ? '' : 's'} could not be read
            and will be skipped.
          </p>
          <ul className="mt-1 space-y-0.5">
            {preview.sampleInvalidRows.map((row) => (
              <li key={row.rowNumber}>
                Row {row.rowNumber}: {row.reason}
              </li>
            ))}
          </ul>
        </div>
      )}

      <div className="flex gap-3">
        <button
          onClick={onBack}
          disabled={busy}
          className="flex-1 rounded-md border border-border px-4 py-2 text-sm font-medium transition-colors hover:bg-accent disabled:opacity-50"
        >
          Back
        </button>
        <button
          onClick={() => onConfirm(mapping)}
          disabled={busy || !preview.mappingComplete || preview.parsedRowCount === 0}
          className="flex-1 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
        >
          Import {preview.parsedRowCount > 0 ? `${preview.parsedRowCount} rows` : ''}
        </button>
      </div>
    </div>
  )
}
