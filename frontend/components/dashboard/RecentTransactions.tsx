import { formatCurrency, formatDate } from '@/lib/utils/format'
import type { Transaction, TransactionType } from '@/types/transaction'

interface Props {
  transactions: Transaction[]
}

const TYPE_COLORS: Record<TransactionType, string> = {
  EXPENSE: 'text-red-600',
  INCOME: 'text-green-600',
  TRANSFER: 'text-blue-600',
  CREDIT_CARD_PAYMENT: 'text-purple-600',
  CASHBACK: 'text-emerald-600',
  REFUND: 'text-teal-600',
  FEE: 'text-orange-600',
  INTEREST: 'text-yellow-600',
  UNKNOWN: 'text-gray-500',
}

export function RecentTransactions({ transactions }: Props) {
  if (transactions.length === 0) {
    return (
      <p className="text-sm text-muted-foreground py-4">
        No transactions yet. Upload a statement to get started.
      </p>
    )
  }

  return (
    <div className="space-y-1">
      {transactions.map(tx => (
        <div key={tx.id} className="flex items-center justify-between rounded-lg px-3 py-2.5 hover:bg-muted/40 transition-colors">
          <div className="flex-1 min-w-0 mr-4">
            <p className="text-sm font-medium truncate">
              {tx.merchantName ?? tx.description}
            </p>
            <div className="flex items-center gap-2 mt-0.5">
              <span className={`text-xs font-medium ${TYPE_COLORS[tx.transactionType]}`}>
                {tx.transactionType.replace(/_/g, ' ')}
              </span>
              <span className="text-xs text-muted-foreground">·</span>
              <span className="text-xs text-muted-foreground">{formatDate(tx.transactionDate)}</span>
              {tx.categoryName && (
                <>
                  <span className="text-xs text-muted-foreground">·</span>
                  <span className="text-xs text-muted-foreground">{tx.categoryName}</span>
                </>
              )}
            </div>
          </div>
          <span className={`text-sm font-semibold whitespace-nowrap ${tx.direction === 'DEBIT' ? 'text-red-600' : 'text-green-600'}`}>
            {tx.direction === 'DEBIT' ? '−' : '+'}{formatCurrency(tx.amount)}
          </span>
        </div>
      ))}
    </div>
  )
}
