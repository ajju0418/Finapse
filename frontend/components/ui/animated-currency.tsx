'use client'

import CountUp from 'react-countup'
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
      <CountUp
        end={value}
        duration={duration}
        decimals={decimals}
        separator=","
        preserveValue
        formattingFn={(n) => formatter.format(n)}
      />
    </span>
  )
}
