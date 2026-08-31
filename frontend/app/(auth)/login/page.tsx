'use client'

import { Suspense, useState } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { motion } from 'framer-motion'
import { Lock, Mail } from 'lucide-react'
import { useAuth } from '@/components/auth/AuthProvider'
import { AuthAlert } from '@/components/auth/AuthAlert'
import { AuthField } from '@/components/auth/AuthField'
import { AuthSubmitButton } from '@/components/auth/AuthSubmitButton'
import { FinapseLogo } from '@/components/branding/FinapseLogo'
import { ApiError } from '@/lib/api/client'
import { staggerChild, staggerParent } from '@/lib/motion'

export default function LoginPage() {
  return (
    <Suspense fallback={null}>
      <LoginForm />
    </Suspense>
  )
}

function LoginForm() {
  const { login } = useAuth()
  const router = useRouter()
  const searchParams = useSearchParams()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<{ email?: string; password?: string }>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  function validate() {
    const errors: { email?: string; password?: string } = {}
    if (!email.trim()) errors.email = 'Enter your email address'
    if (!password) errors.password = 'Enter your password'
    setFieldErrors(errors)
    return Object.keys(errors).length === 0
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setFormError(null)
    if (!validate()) return

    setSubmitting(true)
    try {
      await login({ email: email.trim(), password })
      // Only same-origin relative paths are honoured, so ?next cannot be used
      // as an open redirect to an attacker-controlled site.
      const next = searchParams.get('next')
      const safeNext = next && next.startsWith('/') && !next.startsWith('//') ? next : '/app/money'
      router.replace(safeNext)
    } catch (error) {
      setFormError(
        error instanceof ApiError
          ? error.message
          : 'We could not reach the server. Check that the backend is running.'
      )
      setSubmitting(false)
    }
  }

  return (
    <motion.div variants={staggerParent} initial="hidden" animate="show" className="flex flex-col gap-8">
      <motion.div variants={staggerChild} className="flex flex-col gap-3">
        <div className="lg:hidden">
          <FinapseLogo size={30} />
        </div>
        <h1 className="font-heading text-3xl font-bold tracking-tight text-white">
          Welcome back
        </h1>
        <p className="text-sm leading-relaxed text-white/45">
          Sign in to pick up where your money left off.
        </p>
      </motion.div>

      <motion.form variants={staggerChild} onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
        <AuthAlert message={formError} />

        <AuthField
          label="Email address"
          type="email"
          name="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            setFieldErrors((prev) => ({ ...prev, email: undefined }))
          }}
          error={fieldErrors.email}
          icon={<Mail />}
          autoComplete="email"
          autoFocus
          disabled={submitting}
        />

        <AuthField
          label="Password"
          type="password"
          name="password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value)
            setFieldErrors((prev) => ({ ...prev, password: undefined }))
          }}
          error={fieldErrors.password}
          icon={<Lock />}
          autoComplete="current-password"
          disabled={submitting}
        />

        <AuthSubmitButton loading={submitting} loadingLabel="Signing you in…">
          Sign in
        </AuthSubmitButton>
      </motion.form>

      <motion.p variants={staggerChild} className="text-center text-sm text-white/45">
        New to Finapse?{' '}
        <Link
          href="/register"
          className="font-semibold text-primary underline-offset-4 transition-colors hover:underline"
        >
          Create an account
        </Link>
      </motion.p>
    </motion.div>
  )
}
