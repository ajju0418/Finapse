import { apiClient } from './client'
import type { ReconciliationReview } from '@/types/reconciliation'

export const reconciliationApi = {
  getPending: () =>
    apiClient.get<ReconciliationReview[]>('/reconciliation/reviews'),
  countPending: () =>
    apiClient.get<number>('/reconciliation/reviews/count'),
  decide: (id: string, approved: boolean, note?: string) =>
    apiClient.post<ReconciliationReview>(`/reconciliation/reviews/${id}/decide`, { approved, note }),
}
