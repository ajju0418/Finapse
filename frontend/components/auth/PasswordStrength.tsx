'use client'

import { motion } from 'framer-motion'
import { Check } from 'lucide-react'
import { assessPassword } from '@/lib/auth/password'
import { cn } from '@/lib/utils'

const SEGMENT_TONES = [
  'bg-[oklch(0.60_0.19_25)]',
  'bg-[oklch(0.60_0.19_25)]',
  'bg-[oklch(0.75_0.15_75)]',
  'bg-[var(--violet)]',
  'bg-[var(--violet)]',
]

/**
 * Compact strength meter.
 *
 * Height is reserved rather than animated. An expanding panel here would push
 * the confirm-password field down on every keystroke, which is jarring while
 * typing — the meter now occupies the same space whether or not it has content.
 */
export function PasswordStrength({ password }: { password: string }) {
  const { rules, score, label } = assessPassword(password)
  const active = password.length > 0

  return (
    <div className="min-h-[3.25rem] pt-0.5">
      <div className="flex items-center gap-3">
        <div className="flex flex-1 gap-1.5">
          {[0, 1, 2, 3].map((segment) => (
            <span
              key={segment}
              className="h-[3px] flex-1 overflow-hidden rounded-full bg-white/[0.08]"
            >
              <motion.span
                className={cn('block h-full rounded-full', SEGMENT_TONES[score])}
                initial={false}
                animate={{ scaleX: active && segment < score ? 1 : 0 }}
                style={{ originX: 0 }}
                transition={{ duration: 0.28, delay: segment * 0.04 }}
              />
            </span>
          ))}
        </div>
        <span
          className={cn(
            'w-[4.5rem] text-right text-[0.68rem] font-semibold transition-colors',
            !active
              ? 'text-transparent'
              : score >= 3
                ? 'text-[var(--violet)]'
                : score === 2
                  ? 'text-[oklch(0.75_0.15_75)]'
                  : 'text-[oklch(0.70_0.19_25)]'
          )}
        >
          {label || '—'}
        </span>
      </div>

      {/* Rules sit on one wrapped row to keep the block short. */}
      <ul className="mt-2 flex flex-wrap gap-x-3 gap-y-1">
        {rules.map((rule) => (
          <li
            key={rule.id}
            className={cn(
              'flex items-center gap-1 text-[0.66rem] transition-colors',
              rule.satisfied ? 'text-[var(--violet)]' : 'text-[var(--ink-faint)]'
            )}
          >
            <span
              className={cn(
                'flex h-3 w-3 items-center justify-center rounded-full border transition-colors',
                rule.satisfied
                  ? 'border-[var(--violet)] bg-[var(--violet)]/20'
                  : 'border-[var(--line)]'
              )}
            >
              {rule.satisfied && <Check className="h-2 w-2" />}
            </span>
            {rule.label}
          </li>
        ))}
      </ul>
    </div>
  )
}
