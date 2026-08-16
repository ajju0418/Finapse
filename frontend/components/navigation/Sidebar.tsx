'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useEffect, useState } from 'react'
import { BarChart3, CreditCard, FileText, Settings } from 'lucide-react'
import { cn } from '@/lib/utils'
import { reconciliationApi } from '@/lib/api/reconciliation'

export function Sidebar() {
  const pathname = usePathname()
  const [pendingCount, setPendingCount] = useState<number>(0)

  useEffect(() => {
    reconciliationApi.countPending()
      .then(setPendingCount)
      .catch(() => {/* backend not running — silent */})
  }, [pathname]) // re-check on navigation

  const navItems = [
    { href: '/app/money',      label: 'Money',      icon: BarChart3,  badge: pendingCount > 0 ? pendingCount : null },
    { href: '/app/cards',      label: 'Cards',      icon: CreditCard, badge: null },
    { href: '/app/statements', label: 'Statements', icon: FileText,   badge: null },
  ]

  const secondaryItems = [
    { href: '/app/settings', label: 'Settings', icon: Settings },
  ]

  return (
    <aside className="flex h-screen w-56 flex-col border-r border-border bg-background px-3 py-4">
      <div className="mb-8 px-2">
        <Link href="/app/money" className="text-xl font-bold tracking-tight text-foreground hover:opacity-80 transition-opacity">
          Finapse
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
              <span className="flex h-5 min-w-5 items-center justify-center rounded-full bg-yellow-500 px-1.5 text-xs font-bold text-white">
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
      </div>
    </aside>
  )
}
