'use client'

import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { GraduationCap, Trash2 } from 'lucide-react'
import { rulesApi } from '@/lib/api/transactions'
import { TRANSACTION_TYPE_LABELS } from '@/lib/constants'
import type { LearnedRule } from '@/types/transaction'
import { Skeleton } from '@/components/ui/skeleton'

/**
 * Shows what the classifier has learned from the user's corrections, and lets
 * them undo any of it. Without this the learning loop would be a black box.
 */
export function LearnedRules() {
  const [rules, setRules] = useState<LearnedRule[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [removing, setRemoving] = useState<string | null>(null)

  useEffect(() => {
    rulesApi
      .getAll()
      .then(setRules)
      .catch(() => setError('Could not load learned rules.'))
      .finally(() => setLoading(false))
  }, [])

  async function handleForget(id: string) {
    setRemoving(id)
    try {
      await rulesApi.forget(id)
      setRules((prev) => prev.filter((r) => r.id !== id))
    } catch {
      setError('Could not remove that rule.')
    } finally {
      setRemoving(null)
    }
  }

  return (
    <section className="mt-10">
      <div className="mb-1 flex items-center gap-2">
        <GraduationCap className="h-4 w-4 text-primary" />
        <h2 className="text-base font-semibold">What Finapse has learned</h2>
      </div>
      <p className="mb-4 text-xs text-muted-foreground">
        Rules created when you corrected a transaction. These are applied automatically
        to future imports.
      </p>

      {loading && <Skeleton className="h-20" />}
      {error && <p className="text-sm text-destructive">{error}</p>}

      {!loading && !error && rules.length === 0 && (
        <div className="rounded-xl border border-dashed border-border p-6 text-center">
          <p className="text-xs leading-relaxed text-muted-foreground">
            Nothing learned yet. Correct a transaction&apos;s type and tick
            &ldquo;Remember this&rdquo; to teach the engine.
          </p>
        </div>
      )}

      <div className="flex flex-col gap-2">
        <AnimatePresence initial={false}>
          {rules.map((rule) => (
            <motion.div
              key={rule.id}
              layout
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, height: 0, marginBottom: 0 }}
              transition={{ duration: 0.22 }}
              className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3"
            >
              <div className="flex min-w-0 flex-col gap-0.5">
                <span className="truncate font-mono text-xs" title={rule.narrationPattern}>
                  {rule.narrationPattern}
                </span>
                <span className="flex flex-wrap items-center gap-x-2 text-[0.7rem] text-muted-foreground">
                  <span className="font-semibold text-primary">
                    {TRANSACTION_TYPE_LABELS[rule.transactionType] ?? rule.transactionType}
                  </span>
                  {rule.categoryName && (
                    <>
                      <span aria-hidden>·</span>
                      <span>{rule.categoryName}</span>
                    </>
                  )}
                  <span aria-hidden>·</span>
                  <span>
                    applied {rule.timesApplied} {rule.timesApplied === 1 ? 'time' : 'times'}
                  </span>
                </span>
              </div>

              <button
                type="button"
                onClick={() => handleForget(rule.id)}
                disabled={removing === rule.id}
                aria-label={`Forget rule for ${rule.narrationPattern}`}
                className="shrink-0 rounded-md p-1.5 text-muted-foreground transition-colors hover:bg-destructive/10 hover:text-destructive disabled:opacity-50"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </section>
  )
}
