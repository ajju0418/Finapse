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
          <div className="flex items-start gap-2.5 rounded-xl border border-destructive/40 bg-destructive/10 px-4 py-3 backdrop-blur-sm">
            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-destructive" />
            <p className="text-xs font-medium leading-relaxed text-destructive">{message}</p>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}
