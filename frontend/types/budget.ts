export type BudgetPeriod = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export type BudgetStatus = 'ON_TRACK' | 'AT_RISK' | 'EXCEEDED'

export interface Budget {
  id: string
  categoryId: string | null
  categoryName: string | null
  limitAmount: number
  period: BudgetPeriod
  alertThreshold: number
  periodStart: string
  periodEnd: string
  spent: number
  remaining: number
  percentUsed: number
  status: BudgetStatus
  projectedSpend: number
  projectedToExceed: boolean
}

export interface BudgetInput {
  categoryId: string | null
  limitAmount: number
  period: BudgetPeriod
  alertThreshold?: number
}
