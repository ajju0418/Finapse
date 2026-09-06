'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState, useCallback } from 'react'
import { BarChart3, CreditCard, FileText, Building2, Repeat, Settings, Wallet } from 'lucide-react'
import { cn } from '@/lib/utils'
import { reconciliationApi } from '@/lib/api/reconciliation'
import { FinapseLogo } from '@/components/branding/FinapseLogo'
import { UserMenu } from '@/components/auth/UserMenu'
import { RECONCILIATION_UPDATED_EVENT, type ReconciliationEventDetail } from '@/lib/events/reconciliation'

export function Sidebar() {
  const pathname = usePathname()
  const [pendingCount, setPendingCount] = useState<number>(0)

  const refreshCount = useCallback(() => {
    reconciliationApi.countPending()
      .then(setPendingCount)
      .catch(() => {/* backend not running — silent */})
  }, [])

  useEffect(() => {
    refreshCount()

    const handleReconciliationEvent = (e: Event) => {
      const customEvent = e as CustomEvent<ReconciliationEventDetail>
      if (customEvent.detail && typeof customEvent.detail.pendingCount === 'number') {
        setPendingCount(customEvent.detail.pendingCount)
      } else {
        refreshCount()
      }
    }

    window.addEventListener(RECONCILIATION_UPDATED_EVENT, handleReconciliationEvent)
    window.addEventListener('focus', refreshCount)

    // Periodic live sync every 20 seconds
    const interval = setInterval(refreshCount, 20000)

    return () => {
      window.removeEventListener(RECONCILIATION_UPDATED_EVENT, handleReconciliationEvent)
      window.removeEventListener('focus', refreshCount)
      clearInterval(interval)
    }
  }, [refreshCount, pathname])

  const navItems = [
    { href: '/app/money',      label: 'Money',      icon: BarChart3,  badge: pendingCount > 0 ? pendingCount : null },
    { href: '/app/budgets',    label: 'Budgets',    icon: Wallet,     badge: null },
    { href: '/app/banks',      label: 'Banks',      icon: Building2,   badge: null },
    { href: '/app/cards',      label: 'Cards',      icon: CreditCard, badge: null },
    { href: '/app/subscriptions', label: 'Subscriptions', icon: Repeat, badge: null },
    { href: '/app/statements', label: 'Statements', icon: FileText,   badge: null },
  ]

  const secondaryItems = [
    { href: '/app/settings', label: 'Settings', icon: Settings },
  ]

  return (
    <aside className="flex h-screen w-56 flex-col border-r border-border bg-background px-3 py-4">
      <div className="mb-8 px-2">
        <Link href="/app/money" className="hover:opacity-80 transition-opacity">
          <FinapseLogo size={24} />
        </Link>
      </div>

      <nav className="flex flex-1 flex-col gap-1">
        {navItems.map(({ href, label, icon: Icon, badge }) => (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              pathname.startsWith(href)
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
            )}
          >
            <Icon className="h-4 w-4 shrink-0" />
            <span className="flex-1">{label}</span>
            {badge != null && (
              <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-amber-500/20 border border-amber-500/40 px-1.5 text-[11px] font-bold font-mono text-amber-400 shadow-sm animate-in fade-in zoom-in-75 duration-200">
                {badge}
              </span>
            )}
          </Link>
        ))}
      </nav>

      <div className="flex flex-col gap-1 border-t border-border pt-3">
        {secondaryItems.map(({ href, label, icon: Icon }) => (
          <Link
            key={href}
            href={href}
            className={cn(
              'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
              pathname.startsWith(href)
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
            )}
          >
            <Icon className="h-4 w-4" />
            {label}
          </Link>
        ))}
        <UserMenu />
      </div>
    </aside>
  )
}

