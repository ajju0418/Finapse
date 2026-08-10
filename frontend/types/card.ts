export interface Card {
  id: string
  userId?: string
  name: string
  issuer: string | null
  lastFourDigits: string | null
  creditLimit: number | null
  billingCycleDay: number | null
  paymentDueDay: number | null
  isActive: boolean
  createdAt?: string
}

export interface CardAnalytics {
  cardId: string
  cardName: string
  totalSpending: number
  totalCashback: number
  totalPayments: number
  outstanding: number
  availableCredit: number | null
  transactionCount: number
}
