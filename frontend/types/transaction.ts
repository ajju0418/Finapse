export type TransactionDirection = 'DEBIT' | 'CREDIT'

export type TransactionType =
  | 'EXPENSE'
  | 'INCOME'
  | 'TRANSFER'
  | 'CREDIT_CARD_PAYMENT'
  | 'CASHBACK'
  | 'REFUND'
  | 'FEE'
  | 'INTEREST'
  | 'UNKNOWN'

export type ReconciliationStatus =
  | 'UNMATCHED'
  | 'MATCHED'
  | 'REVIEW_REQUIRED'
  | 'CONFIRMED_DUPLICATE'
  | 'CONFIRMED_TRANSFER'
  | 'CONFIRMED_CARD_PAYMENT'

/** Which part of the engine decided this transaction's type. */
export type ClassificationSource =
  | 'USER_OVERRIDE'
  | 'MERCHANT_DATABASE'
  | 'EXACT_RULE'
  | 'FUZZY_RULE'
  | 'HISTORICAL'
  | 'PATTERN'
  | 'LLM'
  | 'UNKNOWN'

export interface Transaction {
  id: string
  statementId: string
  accountId: string | null
  cardId: string | null
  merchantId: string | null
  categoryId: string | null
  merchantName: string | null
  categoryName: string | null
  transactionDate: string
  postedDate: string | null
  description: string
  amount: number
  direction: TransactionDirection
  transactionType: TransactionType
  cashbackAmount: number | null
  reconciliationStatus: ReconciliationStatus
  sourceRowNumber: number | null
  // Explainability
  classificationSource: ClassificationSource | null
  classificationConfidence: number | null
  classificationReason: string | null
  // Recurring detection
  isRecurring: boolean
  recurringGroupId: string | null
  createdAt: string
}

export interface TransactionCorrection {
  transactionType: TransactionType
  categoryId?: string | null
  /** Persist this as a rule so future imports classify the same way. */
  applyToSimilar?: boolean
}

/** A transaction the user enters by hand — typically cash spending. */
export interface ManualTransactionInput {
  transactionDate: string
  description: string
  amount: number
  direction: TransactionDirection
  transactionType: TransactionType
  categoryId?: string | null
  accountId?: string | null
  cardId?: string | null
}

export interface Category {
  id: string
  name: string
  displayName: string
}

export interface LearnedRule {
  id: string
  narrationPattern: string
  matchType: 'EXACT' | 'CONTAINS' | 'STARTS_WITH'
  transactionType: TransactionType
  categoryName: string | null
  merchantName: string | null
  timesApplied: number
  createdAt: string
}
