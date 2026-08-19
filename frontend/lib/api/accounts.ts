import { apiClient } from './client'
import type { Account, AccountAnalyticsResponse } from '@/types/account'

export const accountsApi = {
  getAll: () => apiClient.get<Account[]>('/accounts'),
  getById: (id: string) => apiClient.get<Account>(`/accounts/${id}`),
  getAnalytics: (id: string) => apiClient.get<AccountAnalyticsResponse>(`/accounts/${id}/analytics`),
  create: (data: Pick<Account, 'name' | 'institutionName' | 'lastFourDigits' | 'currency'>) =>
    apiClient.post<Account>('/accounts', data),
}
