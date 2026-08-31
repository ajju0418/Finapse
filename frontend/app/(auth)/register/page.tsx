'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { motion } from 'framer-motion'
import { Lock, Mail, User } from 'lucide-react'
import { useAuth } from '@/components/auth/AuthProvider'
import { AuthAlert } from '@/components/auth/AuthAlert'
import { AuthField } from '@/components/auth/AuthField'
import { AuthSubmitButton } from '@/components/auth/AuthSubmitButton'
import { PasswordStrength } from '@/components/auth/PasswordStrength'
import { FinapseLogo } from '@/components/branding/FinapseLogo'
import { ApiError } from '@/lib/api/client'
import { assessPassword } from '@/lib/auth/password'
import { staggerChild, staggerParent } from '@/lib/motion'

interface FieldErrors {
  name?: string
  email?: string
  password?: string
  confirmPassword?: string
}

export default function RegisterPage() {
  const { register } = useAuth()
  const router = useRouter()

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate(): boolean {
    const errors: FieldErrors = {}

    if (name.trim().length < 2) {
      errors.name = 'Enter your name (at least 2 characters)'
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())) {
      errors.email = 'Enter a valid email address'
    }
    if (!assessPassword(password).valid) {
      errors.password = 'Password does not meet the requirements below'
    }
    if (password !== confirmPassword) {
      errors.confirmPassword = 'Passwords do not match'
    }

    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFormError(null)
    if (!validate()) return

    setSubmitting(true)
    try {
      await register({ name: name.trim(), email: email.trim(), password })
      router.replace('/app/money')
    } catch (error) {
      setFormError(
        error instanceof ApiError
          ? error.message
          : 'We could not reach the server. Check that the backend is running.'
      )
      setSubmitting(false)
    }
  }

  const clearError = (field: keyof FieldErrors) =>
    setFieldErrors((prev) => ({ ...prev, [field]: undefined }))

  return (
    <motion.div variants={staggerParent} initial="hidden" animate="show" className="flex flex-col gap-7">
      <motion.div variants={staggerChild} className="flex flex-col gap-2.5">
        <div className="mb-1 lg:hidden">
          <FinapseLogo size={30} />
        </div>
        <h1 className="font-heading text-[1.75rem] font-bold leading-tight tracking-tight text-[var(--ink-text)]">
          Create your account
        </h1>
        <p className="text-[0.875rem] leading-relaxed text-[var(--ink-faint)]">
          One account. Every statement, reconciled.
        </p>
      </motion.div>

      <motion.form variants={staggerChild} onSubmit={handleSubmit} noValidate className="flex flex-col">
        <div className="mb-4">
          <AuthAlert message={formError} />
        </div>

        <AuthField
          label="Full name"
          name="name"
          value={name}
          onChange={(e) => {
            setName(e.target.value)
            clearError('name')
          }}
          error={fieldErrors.name}
          icon={<User />}
          autoComplete="name"
          autoFocus
          disabled={submitting}
        />

        <AuthField
          label="Email address"
          type="email"
          name="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            clearError('email')
          }}
          error={fieldErrors.email}
          icon={<Mail />}
          autoComplete="email"
          disabled={submitting}
        />

        <div className="flex flex-col">
          <AuthField
            label="Password"
            type="password"
            name="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              clearError('password')
            }}
            error={fieldErrors.password}
            icon={<Lock />}
            autoComplete="new-password"
            disabled={submitting}
          />
          <PasswordStrength password={password} />
        </div>

        <AuthField
          label="Confirm password"
          type="password"
          name="confirmPassword"
          value={confirmPassword}
          onChange={(e) => {
            setConfirmPassword(e.target.value)
            clearError('confirmPassword')
          }}
          error={fieldErrors.confirmPassword}
          icon={<Lock />}
          autoComplete="new-password"
          disabled={submitting}
        />

        <div className="mt-2">
          <AuthSubmitButton loading={submitting} loadingLabel="Creating your account…">
            Create account
          </AuthSubmitButton>
        </div>
      </motion.form>

      <motion.p variants={staggerChild} className="text-center text-[0.85rem] text-[var(--ink-faint)]">
        Already have an account?{' '}
        <Link
          href="/login"
          className="font-semibold text-[var(--violet)] underline-offset-4 transition-colors hover:text-[oklch(0.74_0.16_295)] hover:underline"
        >
          Sign in
        </Link>
      </motion.p>
    </motion.div>
  )
}
