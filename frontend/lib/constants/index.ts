export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080/api'

export const DATE_FILTER_OPTIONS = [
  { label: '7 Days',     value: '7d'        },
  { label: '30 Days',    value: '30d'       },
  { label: 'This Month', value: 'this_month'},
  { label: '3 Months',   value: '3m'        },
  { label: '6 Months',   value: '6m'        },
  { label: '1 Year',     value: '1y'        },
  { label: 'Custom',     value: 'custom'    },
] as const

export const TRANSACTION_TYPE_LABELS: Record<string, string> = {
  EXPENSE:             'Expense',
  INCOME:              'Income',
  TRANSFER:            'Transfer',
  CREDIT_CARD_PAYMENT: 'Card Payment',
  CASHBACK:            'Cashback',
  REFUND:              'Refund',
  FEE:                 'Fee',
  INTEREST:            'Interest',
  UNKNOWN:             'Unknown',
}

export const REVIEW_TYPE_LABELS: Record<string, string> = {
  POSSIBLE_DUPLICATE:    'Possible Duplicate',
  POSSIBLE_CARD_PAYMENT: 'Possible Credit Card Payment',
  POSSIBLE_TRANSFER:     'Possible Transfer',
  POSSIBLE_REFUND:       'Possible Refund',
  POSSIBLE_CASHBACK:     'Possible Cashback',
}
