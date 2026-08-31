import type { Metadata } from 'next'
import { AuthShowcase } from '@/components/auth/AuthShowcase'

export const metadata: Metadata = {
  title: 'Sign in · Finapse',
  robots: { index: false, follow: false },
}

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="auth-surface grid min-h-screen bg-[var(--ink)] lg:grid-cols-[1.05fr_1fr]">
      <AuthShowcase />

      {/*
        min-h-screen + overflow-y-auto rather than a fixed height: on short
        laptop viewports the form can exceed the fold, and clipping it would
        hide the submit button. It scrolls instead.
      */}
      <main className="relative flex min-h-screen items-center justify-center overflow-y-auto px-6 py-14 sm:px-10">
        {/* Ambient wash so the form side is not flat next to the showcase. */}
        <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
          <div className="absolute -top-40 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-[var(--violet)]/10 blur-[130px]" />
          <div className="absolute -bottom-40 right-0 h-80 w-80 rounded-full bg-[var(--violet)]/[0.06] blur-[110px]" />
        </div>

        <div className="relative z-10 w-full max-w-[25rem] py-2">{children}</div>
      </main>
    </div>
  )
}
