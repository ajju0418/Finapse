import type { Transition, Variants } from 'framer-motion'

/**
 * Shared motion presets for Finapse.
 * Use these instead of inline transitions to keep motion consistent.
 */

export const spring: Transition = {
  type: 'spring',
  stiffness: 260,
  damping: 28,
  mass: 0.9,
}

export const springSnappy: Transition = {
  type: 'spring',
  stiffness: 400,
  damping: 32,
  mass: 0.8,
}

export const ease = [0.2, 0.8, 0.2, 1] as const

export const fadeUp: Variants = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.45, ease } },
}

export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { duration: 0.4, ease } },
}

export const staggerParent: Variants = {
  hidden: {},
  show: {
    transition: {
      staggerChildren: 0.06,
      delayChildren: 0.05,
    },
  },
}

export const staggerChild: Variants = {
  hidden: { opacity: 0, y: 10 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease } },
}
