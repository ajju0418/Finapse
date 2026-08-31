'use client'

import { forwardRef, useId, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { AlertCircle, Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AuthFieldProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string
  error?: string
  icon?: React.ReactNode
}

/**
 * Premium text field for the auth screens.
 *
 * The label sits *above* the input rather than floating inside it. A floating
 * label has to share one box with the icon and the value, which is what made the
 * earlier version feel cramped and let text collide at small field heights.
 * Keeping the label outside gives the value a full-height row of its own.
 *
 * The error line occupies reserved space, so validation appearing or clearing
 * never pushes the rest of the form around.
 */
export const AuthField = forwardRef<HTMLInputElement, AuthFieldProps>(
  ({ label, error, icon, className, type = 'text', value, ...props }, ref) => {
    const id = useId()
    const errorId = `${id}-error`
    const [focused, setFocused] = useState(false)
    const [revealed, setRevealed] = useState(false)

    const isPassword = type === 'password'
    const resolvedType = isPassword && revealed ? 'text' : type
    const invalid = Boolean(error)

    return (
      <div className="flex flex-col">
        <label
          htmlFor={id}
          className={cn(
            'mb-2 text-[0.7rem] font-semibold uppercase tracking-[0.14em] transition-colors duration-200',
            invalid
              ? 'text-[oklch(0.70_0.19_25)]'
              : focused
                ? 'text-[var(--violet)]'
                : 'text-[var(--ink-faint)]'
          )}
        >
          {label}
        </label>

        <div
          className={cn(
            'premium-edge relative h-12 rounded-xl border bg-[var(--ink-field)] transition-all duration-200',
            invalid
              ? 'border-[oklch(0.55_0.19_25)]'
              : focused
                ? 'border-[var(--violet)]'
                : 'border-[var(--line-soft)] hover:border-[var(--line)]'
          )}
          style={{
            boxShadow: invalid
              ? '0 0 0 3px oklch(0.55 0.19 25 / 12%)'
              : focused
                ? '0 0 0 3px oklch(0.64 0.19 295 / 15%), 0 8px 24px -12px oklch(0.64 0.19 295 / 45%)'
                : '0 1px 2px oklch(0 0 0 / 30%)',
          }}
        >
          {icon && (
            <span
              className={cn(
                'pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 transition-colors duration-200',
                "[&_svg]:h-[1.05rem] [&_svg]:w-[1.05rem]",
                invalid
                  ? 'text-[oklch(0.60_0.19_25)]'
                  : focused
                    ? 'text-[var(--violet)]'
                    : 'text-[var(--ink-faint)]'
              )}
            >
              {icon}
            </span>
          )}

          <input
            {...props}
            ref={ref}
            id={id}
            type={resolvedType}
            value={value}
            aria-invalid={invalid}
            aria-describedby={error ? errorId : undefined}
            onFocus={(e) => {
              setFocused(true)
              props.onFocus?.(e)
            }}
            onBlur={(e) => {
              setFocused(false)
              props.onBlur?.(e)
            }}
            className={cn(
              // h-full keeps the value on a single centred row — no vertical crowding.
              'h-full w-full rounded-xl bg-transparent text-[0.9rem] font-medium outline-none',
              'text-[var(--ink-text)] placeholder:text-[oklch(0.42_0.02_292)]',
              'disabled:cursor-not-allowed disabled:opacity-50',
              icon ? 'pl-11' : 'pl-4',
              isPassword ? 'pr-12' : 'pr-4',
              className
            )}
          />

          {isPassword && (
            <button
              type="button"
              onClick={() => setRevealed((r) => !r)}
              tabIndex={-1}
              aria-label={revealed ? 'Hide password' : 'Show password'}
              className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-2 text-[var(--ink-faint)] transition-colors hover:bg-white/5 hover:text-[var(--ink-dim)]"
            >
              {revealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          )}
        </div>

        {/* Reserved height so validation never reflows the form. */}
        <div className="min-h-[1.375rem] overflow-hidden pt-1.5">
          <AnimatePresence mode="wait">
            {error && (
              <motion.p
                key={error}
                id={errorId}
                initial={{ opacity: 0, y: -4 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -4 }}
                transition={{ duration: 0.18 }}
                className="flex items-center gap-1.5 text-[0.7rem] font-medium text-[oklch(0.70_0.19_25)]"
              >
                <AlertCircle className="h-3.5 w-3.5 shrink-0" />
                {error}
              </motion.p>
            )}
          </AnimatePresence>
        </div>
      </div>
    )
  }
)

AuthField.displayName = 'AuthField'
