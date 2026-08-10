export type LinkType = 'CREDIT_CARD_PAYMENT' | 'TRANSFER' | 'REFUND' | 'DUPLICATE' | 'CASHBACK'
export type LinkStatus = 'SUGGESTED' | 'REVIEW_REQUIRED' | 'CONFIRMED' | 'REJECTED'
export type ReviewType =
  | 'POSSIBLE_DUPLICATE'
  | 'POSSIBLE_CARD_PAYMENT'
  | 'POSSIBLE_TRANSFER'
  | 'POSSIBLE_REFUND'
  | 'POSSIBLE_CASHBACK'
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface ReconciliationReview {
  id: string
  linkId: string
  linkType: LinkType
  confidenceScore: number
  reviewType: ReviewType
  status: ReviewStatus
  systemReason: string
  userDecision: string | null
  sourceTransactionId: string
  targetTransactionId: string
  createdAt: string
  reviewedAt: string | null
}
