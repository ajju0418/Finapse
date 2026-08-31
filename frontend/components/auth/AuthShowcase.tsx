'use client'

import { useEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { ArrowLeft, Link2, ShieldCheck, Sparkles } from 'lucide-react'
import Link from 'next/link'
import { FinapseLogo } from '@/components/branding/FinapseLogo'

/**
 * The atmospheric half of the auth screen.
 *
 * It runs a three-beat loop that dramatises what the product actually does:
 * duplicate entries arrive, the engine links them, and a single truthful figure
 * remains. Purely decorative — nothing here is interactive.
 */

const BEATS = [
  {
    key: 'noise',
    eyebrow: 'Raw import',
    headline: 'Two statements.\nOne purchase.',
    caption: 'Your card and your bank both report the same ₹2,000.',
  },
  {
    key: 'link',
    eyebrow: 'Reconciling',
    headline: 'The engine\nfinds the link.',
    caption: 'Settlements are matched to the spending that caused them.',
  },
  {
    key: 'truth',
    eyebrow: 'Resolved',
    headline: 'One number\nyou can trust.',
    caption: 'No phantom expenses. No double counting. Just reality.',
  },
] as const

const BEAT_DURATION = 3600

export function AuthShowcase() {
  const [beat, setBeat] = useState(0)

  useEffect(() => {
    const timer = setInterval(() => setBeat((b) => (b + 1) % BEATS.length), BEAT_DURATION)
    return () => clearInterval(timer)
  }, [])

  const current = BEATS[beat]

  return (
    <div className="auth-surface relative hidden h-screen overflow-hidden bg-[var(--ink)] lg:flex lg:flex-col">
      <Atmosphere />

      {/*
        shrink-0 header/footer with a flexible, min-h-0 middle. Without min-h-0
        a flex child refuses to shrink below its content, which is what let the
        fixed-height stage overflow and collide with the copy below it.
      */}
      <div className="relative z-10 flex h-full min-h-0 flex-col justify-between gap-8 p-10 xl:p-14">
        <div className="flex shrink-0 items-center justify-between">
          <FinapseLogo size={30} />
          <Link
            href="/"
            className="group inline-flex items-center gap-2 rounded-full border border-[var(--line-soft)] bg-white/[0.02] px-4 py-2 text-xs font-medium text-[var(--ink-dim)] backdrop-blur-sm transition-colors hover:border-[var(--line)] hover:text-[var(--ink-text)]"
          >
            <ArrowLeft className="h-3.5 w-3.5 transition-transform group-hover:-translate-x-0.5" />
            Back to site
          </Link>
        </div>

        <div className="flex min-h-0 flex-1 flex-col justify-center gap-10">
          <ReconciliationStage beat={beat} />

          <div className="min-h-[170px] shrink-0">
            <AnimatePresence mode="wait">
              <motion.div
                key={current.key}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -12 }}
                transition={{ duration: 0.5, ease: [0.2, 0.8, 0.2, 1] }}
                className="flex flex-col gap-4"
              >
                <span className="inline-flex w-fit items-center gap-2 rounded-full border border-[var(--violet)]/30 bg-[var(--violet)]/10 px-3 py-1 text-[0.65rem] font-bold uppercase tracking-[0.22em] text-[var(--violet)]">
                  <Sparkles className="h-3 w-3" />
                  {current.eyebrow}
                </span>
                <h2 className="font-heading whitespace-pre-line text-[2.5rem] font-bold leading-[1.12] tracking-tight text-[var(--ink-text)] xl:text-5xl">
                  {current.headline}
                </h2>
                <p className="max-w-md text-[0.95rem] leading-relaxed text-[var(--ink-faint)]">
                  {current.caption}
                </p>
              </motion.div>
            </AnimatePresence>
          </div>
        </div>

        <div className="flex shrink-0 items-center justify-between">
          <div className="flex gap-2" role="presentation">
            {BEATS.map((b, i) => (
              <span
                key={b.key}
                className="h-1 overflow-hidden rounded-full bg-white/15 transition-all duration-500"
                style={{ width: i === beat ? 40 : 16 }}
              >
                {i === beat && (
                  <motion.span
                    className="block h-full bg-[var(--violet)]"
                    initial={{ width: '0%' }}
                    animate={{ width: '100%' }}
                    transition={{ duration: BEAT_DURATION / 1000, ease: 'linear' }}
                  />
                )}
              </span>
            ))}
          </div>

          <p className="flex items-center gap-2 text-xs font-medium text-[var(--ink-faint)]">
            <ShieldCheck className="h-3.5 w-3.5" />
            Your data never leaves your machine
          </p>
        </div>
      </div>
    </div>
  )
}

function Atmosphere() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0">
      <div className="auth-aurora absolute -inset-40 opacity-70" />
      <div className="auth-grid absolute inset-x-0 bottom-0 h-1/2" />
      <div className="absolute inset-0 bg-gradient-to-t from-[var(--ink)] via-transparent to-[var(--ink)]/60" />
      <div className="auth-grain absolute inset-0 opacity-[0.12]" />
      <div className="absolute inset-y-0 right-0 w-px bg-gradient-to-b from-transparent via-[var(--violet)]/25 to-transparent" />
    </div>
  )
}

/** The three-beat visual: duplicates → linked → resolved. */
function ReconciliationStage({ beat }: { beat: number }) {
  const linked = beat >= 1
  const resolved = beat === 2

  return (
    <div className="relative w-full max-w-md shrink-0">
      {/*
        The halo is clipped to its own wrapper. Previously it used a negative
        inset on the stage itself, so the blur bled over the copy underneath.
      */}
      <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden rounded-3xl">
        <div className="auth-beam absolute left-1/2 top-1/2 h-[130%] w-[130%] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-35 blur-2xl" />
      </div>

      <div className="relative flex flex-col justify-center gap-3">
        <LedgerRow
          label="Amazon purchase"
          source="HDFC Credit Card"
          amount="−₹2,000"
          tone={resolved ? 'primary' : 'danger'}
          offset={resolved ? 14 : 0}
          dimmed={false}
        />

        <div className="relative flex h-8 items-center justify-center">
          <AnimatePresence>
            {linked && (
              <motion.div
                initial={{ opacity: 0, scaleX: 0.3 }}
                animate={{ opacity: 1, scaleX: 1 }}
                exit={{ opacity: 0, scaleX: 0.3 }}
                transition={{ duration: 0.45, ease: [0.2, 0.8, 0.2, 1] }}
                className="flex items-center gap-2"
              >
                <span className="h-px w-16 bg-gradient-to-r from-transparent to-[var(--violet)]/70" />
                <span className="flex items-center gap-1.5 rounded-full border border-[var(--violet)]/40 bg-[var(--violet)]/15 px-2.5 py-1 text-[0.6rem] font-bold uppercase tracking-widest text-[var(--violet)] backdrop-blur-sm">
                  <Link2 className="h-3 w-3" />
                  {resolved ? 'Settled' : 'Matched'}
                </span>
                <span className="h-px w-16 bg-gradient-to-l from-transparent to-[var(--violet)]/70" />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <LedgerRow
          label="Card bill payment"
          source="HDFC Bank Account"
          amount={resolved ? 'Excluded' : '−₹2,000'}
          tone={resolved ? 'muted' : 'danger'}
          offset={resolved ? -14 : 0}
          dimmed={resolved}
        />

        <AnimatePresence>
          {resolved && (
            <motion.div
              initial={{ opacity: 0, y: 14, scale: 0.96 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 10, scale: 0.96 }}
              transition={{ duration: 0.5, ease: [0.2, 0.8, 0.2, 1] }}
              className="mt-2 flex items-center justify-between rounded-xl border border-[var(--violet)]/40 bg-[var(--violet)]/10 px-4 py-3 backdrop-blur-md"
            >
              <span className="text-[0.65rem] font-bold uppercase tracking-[0.2em] text-[var(--violet)]">
                Real spending
              </span>
              <span className="font-mono text-lg font-black text-[var(--violet)] text-glow">₹2,000</span>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}

function LedgerRow({
  label,
  source,
  amount,
  tone,
  offset,
  dimmed,
}: {
  label: string
  source: string
  amount: string
  tone: 'danger' | 'primary' | 'muted'
  offset: number
  dimmed: boolean
}) {
  const amountTone =
    tone === 'danger'
      ? 'text-[oklch(0.70_0.19_25)]'
      : tone === 'primary'
        ? 'text-[var(--violet)]'
        : 'text-[var(--ink-faint)]'

  return (
    <motion.div
      animate={{ x: offset, opacity: dimmed ? 0.45 : 1 }}
      transition={{ duration: 0.55, ease: [0.2, 0.8, 0.2, 1] }}
      className="premium-edge flex items-center justify-between rounded-xl border border-[var(--line-soft)] bg-white/[0.035] px-4 py-3 backdrop-blur-md"
    >
      <div className="flex flex-col gap-0.5">
        <span className="text-sm font-medium text-[var(--ink-text)]">{label}</span>
        <span className="text-[0.7rem] text-[var(--ink-faint)]">{source}</span>
      </div>
      <span className={`font-mono text-sm font-bold ${amountTone}`}>{amount}</span>
    </motion.div>
  )
}
