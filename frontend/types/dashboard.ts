import type { Transaction } from './transaction'

export type DashboardPeriod =
  | 'THIS_MONTH'
  | 'LAST_MONTH'
  | '30_DAYS'
  | '3_MONTHS'
  | '6_MONTHS'
  | '1_YEAR'
  | 'YTD'
  | 'CUSTOM'

export interface DateRange {
  from: string
  to: string
}

export interface TrendPoint {
  bucket: string
  label: string
  income: number
  grossExpenses: number
  refunds: number
  actualSpending: number
  cashback: number
  netCashFlow: number
  transactionCount: number
}

export interface TrendData {
  periodStart: string
  periodEnd: string
  granularity: string
  points: TrendPoint[]
  spendingChangeAmount: number
  spendingChangePercent: number | null
  averageMonthlySpend: number
  averageMonthlyIncome: number
}

export interface CategorySpending {
  categoryName: string
  amount: number
  percentage: number
}

export interface MerchantSpending {
  merchantName: string
  amount: number
  transactionCount: number
}

export interface DashboardData {
  income: number
  grossExpenses: number
  refunds: number
  actualSpending: number
  cashback: number
  netCashFlow: number
  periodStart: string
  periodEnd: string
  categoryBreakdown: CategorySpending[]
  topMerchants: MerchantSpending[]
  recentTransactions: Transaction[]
  pendingReviewCount: number
  sourceSummaries: {
    id: string
    name: string
    institution: string
    currentBalance: number
    totalSpending: number
    isCard: boolean
  }[]
}
