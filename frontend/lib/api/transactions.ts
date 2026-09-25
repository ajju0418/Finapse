import { apiClient } from './client'
import type {
  Category,
  LearnedRule,
  ManualTransactionInput,
  Transaction,
  TransactionCorrection,
} from '@/types/transaction'
import { toPageQuery, type Page, type PageParams } from '@/types/pagination'

export const transactionsApi = {
  getByStatement: (statementId: string, params?: PageParams) =>
    apiClient.get<Page<Transaction>>(`/transactions/statement/${statementId}${toPageQuery(params)}`),
  getByCard: (cardId: string, params?: PageParams) =>
    apiClient.get<Page<Transaction>>(`/transactions/card/${cardId}${toPageQuery(params)}`),
  getByAccount: (accountId: string, params?: PageParams) =>
    apiClient.get<Page<Transaction>>(`/transactions/account/${accountId}${toPageQuery(params)}`),
  getById: (id: string) =>
    apiClient.get<Transaction>(`/transactions/${id}`),

  /** Records cash or other off-statement spending. */
  createManual: (input: ManualTransactionInput) =>
    apiClient.post<Transaction>('/transactions', input),

  /** Only manually added transactions can be removed. */
  deleteManual: (id: string) => apiClient.delete(`/transactions/${id}`),

  /** Correct a classification and optionally teach the engine from it. */
  correct: (id: string, correction: TransactionCorrection) =>
    apiClient.patch<Transaction>(`/transactions/${id}`, correction),

  updateType: (id: string, type: string) =>
    apiClient.patch<Transaction>(`/transactions/${id}/type?type=${type}`),
  updateCategory: (id: string, categoryId: string) =>
    apiClient.patch<Transaction>(`/transactions/${id}/category?categoryId=${categoryId}`),
}

export const categoriesApi = {
  getAll: () => apiClient.get<Category[]>('/categories'),
}

export const rulesApi = {
  getAll: () => apiClient.get<LearnedRule[]>('/rules'),
  forget: (ruleId: string) => apiClient.delete(`/rules/${ruleId}`),
}
