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
  createdAt: string
}
