import { apiClient } from './client'
import type { SubscriptionSummary } from '@/types/subscription'

export const subscriptionsApi = {
  getAll: () => apiClient.get<SubscriptionSummary>('/subscriptions'),
}
