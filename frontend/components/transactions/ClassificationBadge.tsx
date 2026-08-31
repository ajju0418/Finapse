'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { useState } from 'react'
import {
  BrainCircuit,
  Building2,
  CircleHelp,
  Fingerprint,
  History,
  ScanSearch,
  Sparkles,
  UserCheck,
} from 'lucide-react'
import type { ClassificationSource, Transaction } from '@/types/transaction'
import { cn } from '@/lib/utils'

/**
 * Explains why the engine classified a transaction the way it did.
 *
 * Confidence drives the colour, so low-confidence guesses are visually distinct
 * from decisions the engine (or the user) is certain about.
 */

const SOURCE_META: Record<
  ClassificationSource,
  { label: string; icon: React.ElementType; blurb: string }
> = {
  USER_OVERRIDE: {
    label: 'Your rule',
    icon: UserCheck,
    blurb: 'You taught the engine this one.',
  },
  MERCHANT_DATABASE: {
    label: 'Merchant match',
    icon: Building2,
    blurb: 'Matched against the known-merchant database.',
  },
  EXACT_RULE: {
    label: 'Exact rule',
    icon: Fingerprint,
    blurb: 'The narration matched a known pattern exactly.',
  },
  FUZZY_RULE: {
    label: 'Fuzzy rule',
    icon: ScanSearch,
    blurb: 'The narration approximately matched a known pattern.',
  },
  HISTORICAL: {
    label: 'Your history',
    icon: History,
    blurb: 'Based on how similar past transactions were classified.',
  },
  PATTERN: {
    label: 'Pattern',
    icon: Sparkles,
    blurb: 'Inferred from amount and description patterns.',
  },
  LLM: {
    label: 'Model',
    icon: BrainCircuit,
    blurb: 'Classified by a language model.',
  },
  UNKNOWN: {
    label: 'Unresolved',
    icon: CircleHelp,
    blurb: 'The engine could not confidently classify this.',
  },
}

function confidenceTone(confidence: number | null) {
  if (confidence == null) return { text: 'text-muted-foreground', ring: 'border-border', bar: 'bg-muted-foreground' }
  if (confidence >= 0.85) return { text: 'text-primary', ring: 'border-primary/40', bar: 'bg-primary' }
  if (confidence >= 0.6) return { text: 'text-amber-500', ring: 'border-amber-500/40', bar: 'bg-amber-500' }
  return { text: 'text-destructive', ring: 'border-destructive/40', bar: 'bg-destructive' }
}

export function ClassificationBadge({ transaction }: { transaction: Transaction }) {
  const [open, setOpen] = useState(false)

  const source = transaction.classificationSource ?? 'UNKNOWN'
  const meta = SOURCE_META[source] ?? SOURCE_META.UNKNOWN
  const Icon = meta.icon
  const confidence = transaction.classificationConfidence
  const tone = confidenceTone(confidence)
  const percent = confidence != null ? Math.round(confidence * 100) : null

  return (
    <div
      className="relative inline-flex"
      onMouseEnter={() => setOpen(true)}
      onMouseLeave={() => setOpen(false)}
    >
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-label={`Why this is ${transaction.transactionType}`}
        className={cn(
          'inline-flex items-center gap-1 rounded-full border px-1.5 py-0.5 transition-colors',
          tone.ring,
          tone.text,
          'hover:bg-accent/60'
        )}
      >
        <Icon className="h-3 w-3" />
        {percent != null && <span className="text-[0.6rem] font-bold tabular-nums">{percent}%</span>}
      </button>

      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0, y: 6, scale: 0.96 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 4, scale: 0.96 }}
            transition={{ duration: 0.16, ease: [0.2, 0.8, 0.2, 1] }}
            role="tooltip"
            className="absolute bottom-full left-1/2 z-50 mb-2 w-72 -translate-x-1/2 rounded-xl border border-border bg-popover p-3 text-left shadow-2xl"
          >
            <div className="flex items-center justify-between gap-2">
              <span className="flex items-center gap-1.5 text-xs font-bold text-popover-foreground">
                <Icon className={cn('h-3.5 w-3.5', tone.text)} />
                {meta.label}
              </span>
              {percent != null && (
                <span className={cn('text-[0.65rem] font-bold tabular-nums', tone.text)}>
                  {percent}% confident
                </span>
              )}
            </div>

            {percent != null && (
              <div className="mt-2 h-1 overflow-hidden rounded-full bg-muted">
                <motion.div
                  className={cn('h-full rounded-full', tone.bar)}
                  initial={{ width: 0 }}
                  animate={{ width: `${percent}%` }}
                  transition={{ duration: 0.4, ease: 'easeOut' }}
                />
              </div>
            )}

            <p className="mt-2 text-[0.7rem] leading-relaxed text-muted-foreground">
              {transaction.classificationReason ?? meta.blurb}
            </p>

            {transaction.isRecurring && (
              <p className="mt-2 flex items-center gap-1.5 border-t border-border pt-2 text-[0.7rem] font-medium text-primary">
                <History className="h-3 w-3" />
                Detected as a recurring charge
              </p>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
