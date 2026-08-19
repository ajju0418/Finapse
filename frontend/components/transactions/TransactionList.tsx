'use client'

import { useEffect, useState } from 'react'
import { transactionsApi } from '@/lib/api/transactions'
import type { Transaction, TransactionType } from '@/types/transaction'
import { formatCurrency, formatDate } from '@/lib/utils/format'

const TYPE_STYLES: Record<TransactionType, string> = {
  EXPENSE:             'bg-red-100 text-red-700',
  INCOME:              'bg-green-100 text-green-700',
  TRANSFER:            'bg-blue-100 text-blue-700',
  CREDIT_CARD_PAYMENT: 'bg-purple-100 text-purple-700',
  CASHBACK:            'bg-emerald-100 text-emerald-700',
  REFUND:              'bg-teal-100 text-teal-700',
  FEE:                 'bg-orange-100 text-orange-700',
  INTEREST:            'bg-yellow-100 text-yellow-700',
  UNKNOWN:             'bg-gray-100 text-gray-600',
}

// Types where money leaves the user's pocket — always show red with −
const MONEY_OUT: TransactionType[] = ['EXPENSE', 'FEE', 'INTEREST', 'CREDIT_CARD_PAYMENT']
// Types where money comes back — always show green with +
const MONEY_IN: TransactionType[]  = ['INCOME', 'CASHBACK', 'REFUND']

function amountColor(tx: Transaction): string {
  if (MONEY_OUT.includes(tx.transactionType)) return 'text-red-600'
  if (MONEY_IN.includes(tx.transactionType))  return 'text-green-600'
  // TRANSFER / UNKNOWN — fall back to raw direction
  return tx.direction === 'DEBIT' ? 'text-red-600' : 'text-green-600'
}

function amountSign(tx: Transaction): string {
  if (MONEY_OUT.includes(tx.transactionType)) return '−'
  if (MONEY_IN.includes(tx.transactionType))  return '+'
  return tx.direction === 'DEBIT' ? '−' : '+'
}

type Props =
  | { statementId: string; cardId?: never; accountId?: never }
  | { cardId: string; statementId?: never; accountId?: never }
  | { accountId: string; statementId?: never; cardId?: never }

export function TransactionList({ statementId, cardId, accountId }: Props) {
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const fetch = statementId
      ? transactionsApi.getByStatement(statementId)
      : cardId
        ? transactionsApi.getByCard(cardId)
        : transactionsApi.getByAccount(accountId!)

    fetch
      .then(setTransactions)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [statementId, cardId, accountId])

  if (loading) return <p className="text-sm text-gray-500 py-4">Loading transactions…</p>
  if (error)   return <p className="text-sm text-red-500 py-4">{error}</p>
  if (transactions.length === 0) return <p className="text-sm text-gray-500 py-4">No transactions found.</p>

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b text-left text-gray-500">
            <th className="pb-2 pr-4 font-medium">Date</th>
            <th className="pb-2 pr-4 font-medium">Description</th>
            <th className="pb-2 pr-4 font-medium">Merchant</th>
            <th className="pb-2 pr-4 font-medium">Type</th>
            <th className="pb-2 font-medium text-right">Amount</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map(tx => (
            <tr key={tx.id} className="border-b last:border-0 hover:bg-gray-50">
              <td className="py-2 pr-4 text-gray-500 whitespace-nowrap">
                {formatDate(tx.transactionDate)}
              </td>
              <td className="py-2 pr-4 max-w-xs truncate" title={tx.description}>
                {tx.description}
              </td>
              <td className="py-2 pr-4 text-gray-600">
                {tx.merchantName ?? <span className="text-gray-400">—</span>}
              </td>
              <td className="py-2 pr-4">
                <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TYPE_STYLES[tx.transactionType]}`}>
                  {tx.transactionType.replace(/_/g, ' ')}
                </span>
              </td>
              <td className={`py-2 text-right font-medium whitespace-nowrap ${amountColor(tx)}`}>
                {amountSign(tx)}{formatCurrency(tx.amount)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
