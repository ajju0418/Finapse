'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { Check, Minus } from 'lucide-react'
import { assessPassword } from '@/lib/auth/password'
import { cn } from '@/lib/utils'

const SEGMENT_TONES = [
  'bg-destructive',
  'bg-destructive',
  'bg-amber-500',
  'bg-primary',
  'bg-primary',
]

export function PasswordStrength({ password }: { password: string }) {
  const { rules, score, label } = assessPassword(password)
  const visible = password.length > 0

  return (
    <AnimatePresence initial={false}>
      {visible && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.28, ease: [0.2, 0.8, 0.2, 1] }}
          className="overflow-hidden"
        >
          <div className="flex flex-col gap-2.5 pt-1">
            <div className="flex items-center gap-3">
              <div className="flex flex-1 gap-1.5">
                {[0, 1, 2, 3].map((segment) => (
                  <span key={segment} className="h-1 flex-1 overflow-hidden rounded-full bg-white/10">
                    <motion.span
                      className={cn('block h-full rounded-full', SEGMENT_TONES[score])}
                      initial={{ scaleX: 0 }}
                      animate={{ scaleX: segment < score ? 1 : 0 }}
                      style={{ originX: 0 }}
                      transition={{ duration: 0.3, delay: segment * 0.05 }}
                    />
                  </span>
                ))}
              </div>
              <span
                className={cn(
                  'w-20 text-right text-[0.7rem] font-semibold',
                  score >= 3 ? 'text-primary' : score === 2 ? 'text-amber-500' : 'text-destructive'
                )}
              >
                {label}
              </span>
            </div>

            <ul className="flex flex-col gap-1">
              {rules.map((rule) => (
                <li
                  key={rule.id}
                  className={cn(
                    'flex items-center gap-2 text-[0.7rem] transition-colors',
                    rule.satisfied ? 'text-primary' : 'text-white/40'
                  )}
                >
                  <span
                    className={cn(
                      'flex h-3.5 w-3.5 items-center justify-center rounded-full border transition-colors',
                      rule.satisfied ? 'border-primary bg-primary/20' : 'border-white/20'
                    )}
                  >
                    {rule.satisfied ? (
                      <Check className="h-2.5 w-2.5" />
                    ) : (
                      <Minus className="h-2.5 w-2.5" />
                    )}
                  </span>
                  {rule.label}
                </li>
              ))}
            </ul>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
