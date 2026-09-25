/** Mirror of the backend `PageResponse<T>` envelope returned by list endpoints. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
}

/** Query parameters a paginated endpoint accepts. */
export interface PageParams {
  /** Zero-indexed page number. */
  page?: number
  /** Rows per page. The backend caps this at 100. */
  size?: number
  /** `property,direction` — e.g. `transactionDate,desc`. */
  sort?: string
}

/** Builds a `?page=&size=&sort=` query string, omitting unset values. */
export function toPageQuery(params: PageParams = {}): string {
  const search = new URLSearchParams()
  if (params.page !== undefined) search.set('page', String(params.page))
  if (params.size !== undefined) search.set('size', String(params.size))
  if (params.sort !== undefined) search.set('sort', params.sort)
  const query = search.toString()
  return query ? `?${query}` : ''
}
