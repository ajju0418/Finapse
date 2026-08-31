export interface Subscription {
  recurringGroupId: string
  merchantName: string
  merchantId: string | null
  categoryName: string | null
  amount: number
  averageAmount: number
  occurrences: number
  averageIntervalDays: number
  cadence: string
  firstCharge: string
  lastCharge: string
  estimatedNextCharge: string | null
  totalSpent: number
  priceIncreased: boolean
  possiblyInactive: boolean
}

export interface SubscriptionSummary {
  subscriptions: Subscription[]
  activeCount: number
  monthlyTotal: number
  annualisedTotal: number
}
