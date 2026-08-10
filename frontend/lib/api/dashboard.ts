import { apiClient } from './client'
import type { DashboardData, DashboardPeriod } from '@/types/dashboard'

export const dashboardApi = {
  get: (period: DashboardPeriod = 'THIS_MONTH') =>
    apiClient.get<DashboardData>(`/dashboard?period=${period}`),
}
