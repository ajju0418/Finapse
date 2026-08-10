import type { Statement } from '@/types/statement'
import { formatDate } from '@/lib/utils/format'
import { CheckCircle2, AlertTriangle, XCircle, Clock } from 'lucide-react'

interface Props {
  statements: Statement[]
  selectedId?: string | null
  onSelect?: (id: string) => void
}

const statusConfig = {
  COMPLETED:       { icon: CheckCircle2, color: 'text-green-600',       label: 'Completed'       },
  REVIEW_REQUIRED: { icon: AlertTriangle, color: 'text-yellow-600',     label: 'Review Required' },
  FAILED:          { icon: XCircle,      color: 'text-destructive',     label: 'Failed'          },
  PROCESSING:      { icon: Clock,        color: 'text-blue-500',        label: 'Processing'      },
  UPLOADED:        { icon: Clock,        color: 'text-muted-foreground', label: 'Uploaded'       },
  CANCELLED:       { icon: XCircle,      color: 'text-muted-foreground', label: 'Cancelled'      },
}

export function StatementHistory({ statements, selectedId, onSelect }: Props) {
  if (statements.length === 0) return null

  return (
    <div className="rounded-xl border border-border overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-muted/50">
          <tr>
            <th className="px-4 py-3 text-left font-medium text-muted-foreground">File</th>
            <th className="px-4 py-3 text-left font-medium text-muted-foreground">Type</th>
            <th className="px-4 py-3 text-left font-medium text-muted-foreground">Source</th>
            <th className="px-4 py-3 text-right font-medium text-muted-foreground">Transactions</th>
            <th className="px-4 py-3 text-left font-medium text-muted-foreground">Uploaded</th>
            <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {statements.map(s => {
            const cfg = statusConfig[s.importStatus] ?? statusConfig.UPLOADED
            const Icon = cfg.icon
            const isSelected = selectedId === s.id
            return (
              <tr
                key={s.id}
                onClick={() => onSelect?.(s.id)}
                className={`transition-colors ${onSelect ? 'cursor-pointer' : ''} ${isSelected ? 'bg-primary/5' : 'hover:bg-muted/30'}`}
              >
                <td className="px-4 py-3 font-medium max-w-[200px] truncate">{s.originalFileName}</td>
                <td className="px-4 py-3 text-muted-foreground">
                  {s.statementType === 'BANK' ? 'Bank' : 'Credit Card'}
                </td>
                <td className="px-4 py-3 text-muted-foreground">
                  {s.accountName ?? s.cardName ?? '—'}
                </td>
                <td className="px-4 py-3 text-right">{s.transactionCount}</td>
                <td className="px-4 py-3 text-muted-foreground">{formatDate(s.uploadedAt)}</td>
                <td className="px-4 py-3">
                  <span className={`flex items-center gap-1.5 ${cfg.color}`}>
                    <Icon className="h-3.5 w-3.5" />
                    {cfg.label}
                  </span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
