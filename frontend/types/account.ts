export interface Account {
  id: string
  userId: string
  name: string
  institutionName: string | null
  accountType: 'BANK'
  lastFourDigits: string | null
  currency: string
  hasStatementPassword?: boolean
  statementPassword?: string
  isActive: boolean
  createdAt: string
}

export interface AccountAnalyticsResponse {
  accountId: string
  accountName: string
  totalInflow: number
  totalOutflow: number
  netChange: number
  transactionCount: number
}
