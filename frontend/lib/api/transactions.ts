import { apiClient } from './client'
import type { Transaction } from '@/types/transaction'

export const transactionsApi = {
  getByStatement: (statementId: string) =>
    apiClient.get<Transaction[]>(`/transactions/statement/${statementId}`),
  getByCard: (cardId: string) =>
    apiClient.get<Transaction[]>(`/transactions/card/${cardId}`),
  getByAccount: (accountId: string) =>
    apiClient.get<Transaction[]>(`/transactions/account/${accountId}`),
  getById: (id: string) =>
    apiClient.get<Transaction>(`/transactions/${id}`),
  updateType: (id: string, type: string) =>
    apiClient.patch<Transaction>(`/transactions/${id}/type?type=${type}`),
  updateCategory: (id: string, categoryId: string) =>
    apiClient.patch<Transaction>(`/transactions/${id}/category?categoryId=${categoryId}`),
}
