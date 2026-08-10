import { apiClient } from './client'
import type { Statement } from '@/types/statement'

export const statementsApi = {
  getAll: () => apiClient.get<Statement[]>('/statements'),
  getById: (id: string) => apiClient.get<Statement>(`/statements/${id}`),
  upload: (form: FormData) => apiClient.postForm<Statement>('/statements/upload', form),
}
