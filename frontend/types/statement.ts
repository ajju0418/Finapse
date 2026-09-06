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
  importError: string | null
  periodStart: string | null
  periodEnd: string | null
  uploadedAt: string
  processedAt: string | null
}

/** Header names the user can remap when auto-detection picks the wrong column. */
export interface ColumnMapping {
  dateColumn: string | null
  postedDateColumn: string | null
  descriptionColumn: string | null
  debitColumn: string | null
  creditColumn: string | null
  amountColumn: string | null
}

export interface StatementPreviewRow {
  sourceRowNumber: number
  date: string | null
  description: string | null
  amount: string | null
  direction: string | null
}

export interface StatementPreview {
  fileName: string
  availableColumns: string[]
  detectedMapping: ColumnMapping
  mappingComplete: boolean
  message: string
  parsedRowCount: number
  invalidRowCount: number
  sampleRows: StatementPreviewRow[]
  sampleInvalidRows: { rowNumber: number; reason: string; rawLine: string }[]
  passwordRequired?: boolean
}
