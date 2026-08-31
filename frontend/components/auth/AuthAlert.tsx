'use client'

import { AnimatePresence, motion } from 'framer-motion'
import { AlertTriangle } from 'lucide-react'

/** Form-level error banner. Shakes once on appearance to draw the eye. */
export function AuthAlert({ message }: { message: string | null }) {
  return (
    <AnimatePresence initial={false}>
      {message && (
        <motion.div
          role="alert"
          initial={{ opacity: 0, height: 0, x: 0 }}
          animate={{ opacity: 1, height: 'auto', x: [0, -7, 6, -4, 0] }}
          exit={{ opacity: 0, height: 0 }}
          transition={{
            duration: 0.4,
            ease: [0.2, 0.8, 0.2, 1],
            x: { duration: 0.36, ease: 'easeInOut' },
          }}
          className="overflow-hidden"
        >
          <div className="flex items-start gap-2.5 rounded-xl border border-[oklch(0.55_0.19_25)]/45 bg-[oklch(0.55_0.19_25)]/[0.12] px-4 py-3 backdrop-blur-sm">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-[oklch(0.72_0.19_25)]" />
            <p className="text-[0.78rem] font-medium leading-relaxed text-[oklch(0.80_0.14_25)]">
              {message}
            </p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
