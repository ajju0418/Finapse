'use client'

import { useEffect, useRef, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { Check, GraduationCap, Loader2, Pencil, X } from 'lucide-react'
import { categoriesApi, transactionsApi } from '@/lib/api/transactions'
import { TRANSACTION_TYPE_LABELS } from '@/lib/constants'
import type { Category, Transaction, TransactionType } from '@/types/transaction'
import { cn } from '@/lib/utils'
import { springSnappy } from '@/lib/motion'

const TYPES: TransactionType[] = [
  'EXPENSE',
  'INCOME',
  'TRANSFER',
  'CREDIT_CARD_PAYMENT',
  'CASHBACK',
  'REFUND',
  'FEE',
  'INTEREST',
  'UNKNOWN',
]

/** Categories are shared across the app; fetch once and reuse. */
let categoryCache: Category[] | null = null

interface Props {
  transaction: Transaction
  onCorrected: (updated: Transaction) => void
}

/**
 * Lets the user fix a misclassification and, by default, teach the engine so the
 * same narration is classified correctly on every future import.
 */
export function CorrectionPopover({ transaction, onCorrected }: Props) {
  const [open, setOpen] = useState(false)
  const [type, setType] = useState<TransactionType>(transaction.transactionType)
  const [categoryId, setCategoryId] = useState<string>(transaction.categoryId ?? '')
  const [applyToSimilar, setApplyToSimilar] = useState(true)
  const [categories, setCategories] = useState<Category[]>(categoryCache ?? [])
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!open || categoryCache) return
    categoriesApi
      .getAll()
      .then((data) => {
        categoryCache = data
        setCategories(data)
      })
      .catch(() => {
        /* category list is optional — the type correction still works */
      })
  }, [open])

  useEffect(() => {
    if (!open) return

    function onPointerDown(event: MouseEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false)
    }
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('mousedown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('mousedown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  // Re-sync when the row's data changes underneath us.
  useEffect(() => {
    setType(transaction.transactionType)
    setCategoryId(transaction.categoryId ?? '')
  }, [transaction.transactionType, transaction.categoryId])

  const dirty = type !== transaction.transactionType || categoryId !== (transaction.categoryId ?? '')

  async function handleSave() {
    setSaving(true)
    setError(null)
    try {
      const updated = await transactionsApi.correct(transaction.id, {
        transactionType: type,
        categoryId: categoryId || null,
        applyToSimilar,
      })
      onCorrected(updated)
      setOpen(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not save the correction')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div ref={containerRef} className="relative inline-flex">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label="Correct this classification"
        aria-expanded={open}
        className="rounded-md p-1 text-muted-foreground opacity-0 transition-all hover:bg-accent hover:text-foreground focus-visible:opacity-100 group-hover/row:opacity-100"
      >
        <Pencil className="h-3.5 w-3.5" />
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 8, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 6, scale: 0.96 }}
            transition={springSnappy}
            className="absolute right-0 top-full z-50 mt-2 w-72 rounded-xl border border-border bg-popover p-3 text-left shadow-2xl"
          >
            <div className="mb-3 flex items-center justify-between">
              <h4 className="text-xs font-bold text-popover-foreground">Correct classification</h4>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="rounded p-0.5 text-muted-foreground hover:text-foreground"
                aria-label="Close"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </div>

            <label className="mb-1 block text-[0.65rem] font-semibold uppercase tracking-wider text-muted-foreground">
              Type
            </label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value as TransactionType)}
              className="mb-3 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-xs outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {TYPES.map((t) => (
                <option key={t} value={t}>
                  {TRANSACTION_TYPE_LABELS[t] ?? t.replace(/_/g, ' ')}
                </option>
              ))}
            </select>

            {categories.length > 0 && (
              <>
                <label className="mb-1 block text-[0.65rem] font-semibold uppercase tracking-wider text-muted-foreground">
                  Category
                </label>
                <select
                  value={categoryId}
                  onChange={(e) => setCategoryId(e.target.value)}
                  className="mb-3 w-full rounded-lg border border-input bg-background px-2 py-1.5 text-xs outline-none focus-visible:ring-2 focus-visible:ring-ring"
                >
                  <option value="">Leave unchanged</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.displayName}
                    </option>
                  ))}
                </select>
              </>
            )}

            <button
              type="button"
              onClick={() => setApplyToSimilar((v) => !v)}
              className={cn(
                'mb-3 flex w-full items-start gap-2 rounded-lg border p-2 text-left transition-colors',
                applyToSimilar
                  ? 'border-primary/40 bg-primary/10'
                  : 'border-border hover:bg-accent/50'
              )}
            >
              <span
                className={cn(
                  'mt-0.5 flex h-3.5 w-3.5 shrink-0 items-center justify-center rounded border transition-colors',
                  applyToSimilar ? 'border-primary bg-primary text-primary-foreground' : 'border-input'
                )}
              >
                {applyToSimilar && <Check className="h-2.5 w-2.5" />}
              </span>
              <span className="flex flex-col gap-0.5">
                <span className="flex items-center gap-1 text-[0.7rem] font-semibold text-popover-foreground">
                  <GraduationCap className="h-3 w-3" />
                  Remember this
                </span>
                <span className="text-[0.65rem] leading-snug text-muted-foreground">
                  Apply to similar transactions in future imports.
                </span>
              </span>
            </button>

            {error && <p className="mb-2 text-[0.7rem] font-medium text-destructive">{error}</p>}

            <button
              type="button"
              onClick={handleSave}
              disabled={saving || !dirty}
              className="flex h-8 w-full items-center justify-center gap-1.5 rounded-lg bg-primary text-xs font-bold text-primary-foreground transition-opacity hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {saving ? (
                <>
                  <Loader2 className="h-3.5 w-3.5 animate-spin" />
                  Saving…
                </>
              ) : (
                'Save correction'
              )}
            </button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
