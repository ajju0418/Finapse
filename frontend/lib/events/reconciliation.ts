export const RECONCILIATION_UPDATED_EVENT = 'finapse:reconciliation-updated'

export interface ReconciliationEventDetail {
  pendingCount?: number
}

export function emitReconciliationUpdated(pendingCount?: number) {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(
      new CustomEvent<ReconciliationEventDetail>(RECONCILIATION_UPDATED_EVENT, {
        detail: { pendingCount },
      })
    )
  }
}
