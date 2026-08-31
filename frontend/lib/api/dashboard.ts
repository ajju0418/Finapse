import { apiClient } from './client'
import type {
  DashboardData,
  DashboardPeriod,
  DateRange,
  TrendData,
} from '@/types/dashboard'

function rangeQuery(range?: DateRange): string {
  return range ? `&from=${range.from}&to=${range.to}` : ''
}

export const dashboardApi = {
  get: (period: DashboardPeriod = 'THIS_MONTH', range?: DateRange) =>
    apiClient.get<DashboardData>(`/dashboard?period=${period}${rangeQuery(range)}`),

  /** Monthly income / spending / net series. A range overrides `months`. */
  trends: (months = 12, range?: DateRange) =>
    apiClient.get<TrendData>(`/dashboard/trends?months=${months}${rangeQuery(range)}`),
}
