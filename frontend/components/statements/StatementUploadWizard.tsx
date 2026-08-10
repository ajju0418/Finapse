'use client'

import { useState, useEffect } from 'react'
import { accountsApi } from '@/lib/api/accounts'
import { cardsApi } from '@/lib/api/cards'
import { statementsApi } from '@/lib/api/statements'
import type { Account } from '@/types/account'
import type { Card } from '@/types/card'
import type { Statement, StatementType } from '@/types/statement'
import { CsvDropzone } from './CsvDropzone'
import { X, CheckCircle2, AlertTriangle } from 'lucide-react'

interface Props {
  onImported: (statement: Statement) => void
  onCancel: () => void
}

type Step = 'type' | 'source' | 'upload' | 'done'

export function StatementUploadWizard({ onImported, onCancel }: Props) {
  const [step, setStep] = useState<Step>('type')
  const [statementType, setStatementType] = useState<StatementType | null>(null)
  const [accounts, setAccounts] = useState<Account[]>([])
  const [cards, setCards] = useState<Card[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState<string>('')
  const [selectedCardId, setSelectedCardId] = useState<string>('')
  const [file, setFile] = useState<File | null>(null)
  const [uploading, setUploading] = useState(false)
  const [result, setResult] = useState<Statement | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    accountsApi.getAll().then(setAccounts).catch(() => {})
    cardsApi.getAll().then(setCards).catch(() => {})
  }, [])

  function selectType(type: StatementType) {
    setStatementType(type)
    setStep('source')
  }

  function goToUpload() {
    if (statementType === 'BANK' && !selectedAccountId) return
    if (statementType === 'CREDIT_CARD' && !selectedCardId) return
    setStep('upload')
  }

  async function handleUpload() {
    if (!file || !statementType) return
    setError(null)
    setUploading(true)
    try {
      const form = new FormData()
      form.append('file', file)
      form.append('statementType', statementType)
      if (statementType === 'BANK') form.append('accountId', selectedAccountId)
      if (statementType === 'CREDIT_CARD') form.append('cardId', selectedCardId)

      const statement = await statementsApi.upload(form)
      setResult(statement)
      setStep('done')
      onImported(statement)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="w-full max-w-lg">
      {/* Header */}
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-lg font-semibold">Upload Statement</h2>
          <p className="text-xs text-muted-foreground mt-0.5">
            Step {step === 'type' ? 1 : step === 'source' ? 2 : step === 'upload' ? 3 : 4} of 3
          </p>
        </div>
        <button onClick={onCancel} className="text-muted-foreground hover:text-foreground">
          <X className="h-4 w-4" />
        </button>
      </div>

      {/* Step 1 — Statement type */}
      {step === 'type' && (
        <div className="space-y-3">
          <p className="text-sm font-medium mb-4">Select statement type</p>
          {(['BANK', 'CREDIT_CARD'] as StatementType[]).map(type => (
            <button
              key={type}
              onClick={() => selectType(type)}
              className="w-full rounded-lg border border-border p-4 text-left hover:border-primary hover:bg-primary/5 transition-colors"
            >
              <p className="font-medium text-sm">
                {type === 'BANK' ? 'Bank Statement' : 'Credit Card Statement'}
              </p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {type === 'BANK'
                  ? 'Import transactions from a bank account'
                  : 'Import transactions from a credit card'}
              </p>
            </button>
          ))}
        </div>
      )}

      {/* Step 2 — Select account or card */}
      {step === 'source' && (
        <div className="space-y-4">
          <p className="text-sm font-medium">
            Select {statementType === 'BANK' ? 'bank account' : 'credit card'}
          </p>

          {statementType === 'BANK' && (
            accounts.length === 0
              ? <p className="text-sm text-muted-foreground">No bank accounts found. Add one first.</p>
              : <div className="space-y-2">
                  {accounts.map(a => (
                    <button
                      key={a.id}
                      onClick={() => setSelectedAccountId(a.id)}
                      className={`w-full rounded-lg border p-3 text-left transition-colors
                        ${selectedAccountId === a.id
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:border-primary/50'}`}
                    >
                      <p className="text-sm font-medium">{a.name}</p>
                      {a.institutionName && (
                        <p className="text-xs text-muted-foreground">{a.institutionName}</p>
                      )}
                    </button>
                  ))}
                </div>
          )}

          {statementType === 'CREDIT_CARD' && (
            cards.length === 0
              ? <p className="text-sm text-muted-foreground">No credit cards found. Add one first.</p>
              : <div className="space-y-2">
                  {cards.map(c => (
                    <button
                      key={c.id}
                      onClick={() => setSelectedCardId(c.id)}
                      className={`w-full rounded-lg border p-3 text-left transition-colors
                        ${selectedCardId === c.id
                          ? 'border-primary bg-primary/5'
                          : 'border-border hover:border-primary/50'}`}
                    >
                      <p className="text-sm font-medium">{c.name}</p>
                      {c.issuer && (
                        <p className="text-xs text-muted-foreground">{c.issuer}{c.lastFourDigits ? ` •••• ${c.lastFourDigits}` : ''}</p>
                      )}
                    </button>
                  ))}
                </div>
          )}

          <div className="flex gap-3 pt-2">
            <button
              onClick={() => setStep('type')}
              className="flex-1 rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors"
            >
              Back
            </button>
            <button
              onClick={goToUpload}
              disabled={(statementType === 'BANK' && !selectedAccountId) || (statementType === 'CREDIT_CARD' && !selectedCardId)}
              className="flex-1 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              Continue
            </button>
          </div>
        </div>
      )}

      {/* Step 3 — Upload CSV */}
      {step === 'upload' && (
        <div className="space-y-4">
          <CsvDropzone onFileSelected={setFile} disabled={uploading} />

          {error && (
            <div className="flex items-start gap-2 rounded-lg border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">
              <AlertTriangle className="h-4 w-4 mt-0.5 shrink-0" />
              <p>{error}</p>
            </div>
          )}

          <div className="flex gap-3">
            <button
              onClick={() => setStep('source')}
              disabled={uploading}
              className="flex-1 rounded-md border border-border px-4 py-2 text-sm font-medium hover:bg-accent transition-colors disabled:opacity-50"
            >
              Back
            </button>
            <button
              onClick={handleUpload}
              disabled={!file || uploading}
              className="flex-1 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
            >
              {uploading ? 'Importing…' : 'Import Statement'}
            </button>
          </div>
        </div>
      )}

      {/* Step 4 — Done */}
      {step === 'done' && result && (
        <div className="space-y-4 text-center">
          <CheckCircle2 className="h-12 w-12 text-green-500 mx-auto" />
          <div>
            <p className="font-semibold">Statement imported</p>
            <p className="text-sm text-muted-foreground mt-1">{result.originalFileName}</p>
          </div>
          <div className="rounded-lg border border-border bg-muted/30 p-4 text-left space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-muted-foreground">Transactions</span>
              <span className="font-medium">{result.transactionCount}</span>
            </div>
            {result.periodStart && result.periodEnd && (
              <div className="flex justify-between">
                <span className="text-muted-foreground">Period</span>
                <span className="font-medium">{result.periodStart} → {result.periodEnd}</span>
              </div>
            )}
            <div className="flex justify-between">
              <span className="text-muted-foreground">Status</span>
              <span className={`font-medium ${result.importStatus === 'REVIEW_REQUIRED' ? 'text-yellow-600' : 'text-green-600'}`}>
                {result.importStatus === 'REVIEW_REQUIRED' ? 'Review Required' : 'Completed'}
              </span>
            </div>
          </div>
          <button
            onClick={onCancel}
            className="w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Done
          </button>
        </div>
      )}
    </div>
  )
}
