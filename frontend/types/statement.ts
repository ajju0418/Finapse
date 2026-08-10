export type StatementType = 'BANK' | 'CREDIT_CARD'

export type ImportStatus =
  | 'UPLOADED'
  | 'PROCESSING'
  | 'REVIEW_REQUIRED'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'

export interface Statement {
  id: string
  userId: string
  accountId: string | null
  cardId: string | null
  accountName: string | null
  cardName: string | null
  statementType: StatementType
  originalFileName: string
  transactionCount: number
  importStatus: ImportStatus
  periodStart: string | null
  periodEnd: string | null
  uploadedAt: string
  processedAt: string | null
}
