import { apiClient } from './client'
import type { Card, CardAnalytics } from '@/types/card'

export const cardsApi = {
  getAll: () => apiClient.get<Card[]>('/cards'),
  getById: (id: string) => apiClient.get<Card>(`/cards/${id}`),
  getAnalytics: (id: string) => apiClient.get<CardAnalytics>(`/cards/${id}/analytics`),
  create: (data: Omit<Card, 'id' | 'userId' | 'createdAt' | 'isActive'> & { isActive: boolean }) =>
    apiClient.post<Card>('/cards', data),
  deactivate: (id: string) => apiClient.patch<Card>(`/cards/${id}/deactivate`),
}
