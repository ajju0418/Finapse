'use client'

import React from 'react'
import {
  TrendingUp,
  TrendingDown,
  CreditCard,
  Building2,
  ArrowUpRight,
  ArrowDownLeft
} from 'lucide-react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'

interface FinancialSourceCardProps {
  source: {
    name: string
    institution: string
    currentBalance: string // formatted
    totalSpending: string // formatted
    isCard: boolean
  }
  onClick?: () => void
}

export function FinancialSourceCard({ source, onClick }: FinancialSourceCardProps) {
  const { name, institution, currentBalance, totalSpending, isCard } = source

  return (
    <Card
      className="group cursor-pointer transition-all hover:shadow-md hover:border-primary/50"
      onClick={onClick}
    >
      <CardHeader className="pb-2">
        <div className="flex items-center justify-between">
          <div className="p-2 rounded-lg bg-primary/10 text-primary">
            {isCard ? <CreditCard className="h-5 w-5" /> : <Building2 className="h-5 w-5" />}
          </div>
          <Badge variant="outline" className="text-[10px] uppercase tracking-wider">
            {isCard ? 'Credit Card' : 'Bank Account'}
          </Badge>
        </div>
        <CardTitle className="text-lg font-bold mt-3 group-hover:text-primary transition-colors">
          {name}
        </CardTitle>
        <p className="text-xs text-muted-foreground">{institution}</p>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs text-muted-foreground">
            {isCard ? 'Outstanding' : 'Net Change'}
          </span>
          <div className="flex items-center gap-1 font-semibold">
            {isCard ? <TrendingDown className="h-3 w-3 text-red-500" /> : <TrendingUp className="h-3 w-3 text-emerald-500" />}
            <span>{currentBalance}</span>
          </div>
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-border/50">
          <span className="text-xs text-muted-foreground">
            Total Spending
          </span>
          <span className="font-medium">{totalSpending}</span>
        </div>
      </CardContent>
    </Card>
  )
}
