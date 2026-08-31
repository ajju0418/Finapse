import type { Metadata } from 'next'
import { AuthShowcase } from '@/components/auth/AuthShowcase'

export const metadata: Metadata = {
  title: 'Sign in · Finapse',
  robots: { index: false, follow: false },
}

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="grid min-h-screen bg-[oklch(0.11_0.03_300)] lg:grid-cols-[1.05fr_1fr]">
      <AuthShowcase />
      <main className="relative flex items-center justify-center overflow-hidden px-6 py-12 sm:px-10">
        {/* Ambient wash so the form side is not flat next to the showcase. */}
        <div aria-hidden className="pointer-events-none absolute inset-0">
          <div className="absolute -top-40 left-1/2 h-96 w-96 -translate-x-1/2 rounded-full bg-primary/10 blur-[120px]" />
          <div className="absolute -bottom-40 right-0 h-80 w-80 rounded-full bg-primary/5 blur-[110px]" />
        </div>
        <div className="relative z-10 w-full max-w-[26rem]">{children}</div>
      </main>
    </div>
  )
}
