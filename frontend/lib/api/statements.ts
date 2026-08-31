import { apiClient } from './client'
import type { Statement, StatementPreview } from '@/types/statement'

export const statementsApi = {
  getAll: () => apiClient.get<Statement[]>('/statements'),
  getById: (id: string) => apiClient.get<Statement>(`/statements/${id}`),

  /** Dry run — reports detected columns and sample rows without saving anything. */
  preview: (form: FormData) => apiClient.postForm<StatementPreview>('/statements/preview', form),

  /** Queues the import; the statement comes back as PROCESSING. */
  upload: (form: FormData) => apiClient.postForm<Statement>('/statements/upload', form),

  delete: (id: string) => apiClient.delete(`/statements/${id}`),
}
