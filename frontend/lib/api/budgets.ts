import { apiClient } from './client'
import type { Budget, BudgetInput } from '@/types/budget'

export const budgetsApi = {
  getAll: () => apiClient.get<Budget[]>('/budgets'),
  create: (input: BudgetInput) => apiClient.post<Budget>('/budgets', input),
  update: (id: string, input: BudgetInput) => apiClient.put<Budget>(`/budgets/${id}`, input),
  delete: (id: string) => apiClient.delete(`/budgets/${id}`),
}
