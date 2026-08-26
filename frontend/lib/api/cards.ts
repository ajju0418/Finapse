import { apiClient } from './client'
import type { Card, CardAnalytics } from '@/types/card'

export const cardsApi = {
  getAll: () => apiClient.get<Card[]>('/cards'),
  getById: (id: string) => apiClient.get<Card>(`/cards/${id}`),
  getAnalytics: (id: string) => apiClient.get<CardAnalytics>(`/cards/${id}/analytics`),
  create: (data: Omit<Card, 'id' | 'userId' | 'active' | 'createdAt' | 'updatedAt'>) =>
    apiClient.post<Card>('/cards', data),
  update: (id: string, data: Omit<Card, 'id' | 'userId' | 'active' | 'createdAt' | 'updatedAt'>) =>
    apiClient.put<Card>(`/cards/${id}`, data),
  delete: (id: string) => apiClient.delete(`/cards/${id}`),
  deactivate: (id: string) => apiClient.patch<Card>(`/cards/${id}/deactivate`),
}
