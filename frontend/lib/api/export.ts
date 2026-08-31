import { API_BASE_URL } from '@/lib/constants'
import { getAccessToken } from '@/lib/auth/token-store'
import { ApiError, refreshAccessToken } from './client'

/**
 * Downloads a CSV of the user's transactions.
 *
 * Kept outside `apiClient` because the response is a file, not JSON. The 401
 * retry mirrors the JSON client so an expired access token is refreshed once.
 */
async function fetchCsv(path: string, isRetry = false): Promise<Blob> {
  const token = getAccessToken()
  const res = await fetch(`${API_BASE_URL}${path}`, {
    credentials: 'include',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })

  if (res.status === 401 && !isRetry) {
    const refreshed = await refreshAccessToken()
    if (refreshed) return fetchCsv(path, true)
  }

  if (!res.ok) {
    throw new ApiError('Export failed. Please try again.', res.status, 'EXPORT_FAILED')
  }
  return res.blob()
}

function saveBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export const exportApi = {
  async downloadTransactionsCsv(range?: { from: string; to: string }): Promise<void> {
    const query = range ? `?from=${range.from}&to=${range.to}` : ''
    const blob = await fetchCsv(`/export/transactions.csv${query}`)
    saveBlob(blob, `finapse-transactions-${new Date().toISOString().slice(0, 10)}.csv`)
  },
}
