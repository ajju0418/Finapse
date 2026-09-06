'use client'

import { useEffect, useState } from 'react'
import { cn } from '@/lib/utils'

interface AnimatedCurrencyProps {
  value: number
  className?: string
  prefix?: string
  duration?: number
  decimals?: number
  /** Currency code for Intl. Defaults to INR. */
  currency?: string
  /** Locale for Intl. Defaults to en-IN. */
  locale?: string
  /** Optional sign prefix ("+" for positive). */
  showSign?: boolean
}

/**
 * Animated currency figure. Counts up from 0 → value on mount.
 * Uses tabular numerals so digits don't jitter horizontally.
 */
export function AnimatedCurrency({
  value,
  className,
  prefix,
  duration = 1.1,
  decimals = 0,
  currency = 'INR',
  locale = 'en-IN',
  showSign = false,
}: AnimatedCurrencyProps) {
  const [displayValue, setDisplayValue] = useState(0)

  useEffect(() => {
    let startTimestamp: number | null = null
    const startValue = 0
    const endValue = value
    const durationMs = duration * 1000

    let animationFrameId: number

    const step = (timestamp: number) => {
      if (!startTimestamp) startTimestamp = timestamp
      const progress = Math.min((timestamp - startTimestamp) / durationMs, 1)
      const current = startValue + (endValue - startValue) * progress
      setDisplayValue(current)

      if (progress < 1) {
        animationFrameId = requestAnimationFrame(step)
      }
    }

    animationFrameId = requestAnimationFrame(step)
    return () => cancelAnimationFrame(animationFrameId)
  }, [value, duration])

  const formatter = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    maximumFractionDigits: decimals,
    minimumFractionDigits: decimals,
  })

  const sign = showSign && value > 0 ? '+' : ''

  return (
    <span className={cn('num inline-flex items-baseline', className)}>
      {prefix}
      {sign}
      {formatter.format(displayValue)}
    </span>
  )
}

