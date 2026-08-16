'use client'

import { useEffect, useState } from 'react'
import { statementsApi } from '@/lib/api/statements'
import type { Statement } from '@/types/statement'
import { StatementUploadWizard } from '@/components/statements/StatementUploadWizard'
import { StatementHistory } from '@/components/statements/StatementHistory'
import { TransactionList } from '@/components/transactions/TransactionList'
import { Skeleton } from '@/components/ui/skeleton'

export default function StatementsPage() {
  const [statements, setStatements] = useState<Statement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showWizard, setShowWizard] = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)

  useEffect(() => {
    statementsApi.getAll()
      .then(setStatements)
      .catch(() => setError('Could not load statements. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [])

  function handleImported(statement: Statement) {
    setStatements(prev => [statement, ...prev])
    setSelectedId(statement.id)
  }

  const selected = statements.find(s => s.id === selectedId)

  return (
    <div className="p-8">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Statements</h1>
            <p className="text-sm text-muted-foreground mt-1">Import and manage financial statements</p>
          </div>
          <button
            onClick={() => setShowWizard(true)}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            + Upload Statement
          </button>
        </div>

        {showWizard && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-lg rounded-xl border border-border bg-background p-6 shadow-lg">
              <StatementUploadWizard
                onImported={(s) => { handleImported(s); setShowWizard(false) }}
                onCancel={() => setShowWizard(false)}
              />
            </div>
          </div>
        )}

        {loading && (
          <div className="space-y-3">
            {[1, 2, 3].map(i => <Skeleton key={i} className="h-12 w-full rounded-lg" />)}
          </div>
        )}

        {error && (
          <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
            {error}
          </div>
        )}

        {!loading && !error && statements.length === 0 && (
          <div className="rounded-xl border border-dashed border-border p-16 text-center">
            <p className="text-muted-foreground font-medium">No statements imported yet.</p>
            <p className="text-sm text-muted-foreground mt-1">
              Upload a CSV bank or credit-card statement to get started.
            </p>
            <button
              onClick={() => setShowWizard(true)}
              className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              Upload Statement
            </button>
          </div>
        )}

        {!loading && !error && statements.length > 0 && (
          <div className="space-y-6">
            <StatementHistory
              statements={statements}
              selectedId={selectedId}
              onSelect={id => setSelectedId(prev => prev === id ? null : id)}
            />

            {selected && (
              <div className="rounded-xl border border-border bg-card p-6">
                <div className="mb-4 flex items-center justify-between">
                  <div>
                    <h2 className="font-semibold">{selected.originalFileName}</h2>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {selected.transactionCount} transactions · {selected.statementType}
                    </p>
                  </div>
                  <button
                    onClick={() => setSelectedId(null)}
                    className="text-xs text-muted-foreground hover:text-foreground"
                  >
                    Close ✕
                  </button>
                </div>
                <TransactionList statementId={selected.id} />
              </div>
            )}
          </div>
        )}
      </div>
  )
}
