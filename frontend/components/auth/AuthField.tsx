'use client'

import { forwardRef, useId, useState } from 'react'
import { motion } from 'framer-motion'
import { AlertCircle, Eye, EyeOff } from 'lucide-react'
import { cn } from '@/lib/utils'

interface AuthFieldProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string
  error?: string
  icon?: React.ReactNode
}

/**
 * Floating-label input with a focus glow.
 *
 * The label sits inside the field until it is focused or filled, then lifts into
 * the border. Errors are wired up with aria-invalid/aria-describedby so screen
 * readers announce them.
 */
export const AuthField = forwardRef<HTMLInputElement, AuthFieldProps>(
  ({ label, error, icon, className, type = 'text', value, ...props }, ref) => {
    const id = useId()
    const errorId = `${id}-error`
    const [focused, setFocused] = useState(false)
    const [revealed, setRevealed] = useState(false)

    const isPassword = type === 'password'
    const resolvedType = isPassword && revealed ? 'text' : type
    const filled = value !== undefined && String(value).length > 0
    const lifted = focused || filled

    return (
      <div className="flex flex-col gap-1.5">
        <div
          className={cn(
            'group relative rounded-xl border bg-white/[0.03] transition-colors duration-200',
            error
              ? 'border-destructive/60'
              : focused
                ? 'border-primary/70'
                : 'border-white/10 hover:border-white/20'
          )}
        >
          {/* Focus halo */}
          <motion.div
            aria-hidden
            className="pointer-events-none absolute -inset-px rounded-xl"
            animate={{
              boxShadow: focused
                ? '0 0 0 3px oklch(0.60 0.15 300 / 18%), 0 0 24px oklch(0.60 0.15 300 / 22%)'
                : '0 0 0 0px oklch(0.60 0.15 300 / 0%), 0 0 0px oklch(0.60 0.15 300 / 0%)',
            }}
            transition={{ duration: 0.22 }}
          />

          <label
            htmlFor={id}
            className={cn(
              'pointer-events-none absolute left-11 origin-left text-white/45 transition-all duration-200',
              lifted
                ? 'top-1.5 text-[0.65rem] font-semibold uppercase tracking-[0.14em] text-primary/80'
                : 'top-1/2 -translate-y-1/2 text-sm'
            )}
          >
            {label}
          </label>

          {icon && (
            <span
              className={cn(
                'pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 transition-colors [&_svg]:h-4 [&_svg]:w-4',
                focused ? 'text-primary' : 'text-white/35'
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
            aria-invalid={Boolean(error)}
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
              'peer relative w-full bg-transparent pb-2 pl-11 pt-6 text-sm text-white outline-none',
              'placeholder:text-transparent disabled:cursor-not-allowed disabled:opacity-50',
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
              className="absolute right-3 top-1/2 -translate-y-1/2 rounded-md p-1.5 text-white/35 transition-colors hover:text-white/70"
            >
              {revealed ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
            </button>
          )}
        </div>

        {error && (
          <motion.p
            id={errorId}
            initial={{ opacity: 0, x: -6 }}
            animate={{ opacity: 1, x: 0 }}
            className="flex items-center gap-1.5 pl-1 text-xs font-medium text-destructive"
          >
            <AlertCircle className="h-3.5 w-3.5 shrink-0" />
            {error}
          </motion.p>
        )}
      </div>
    )
  }
)

AuthField.displayName = 'AuthField'
