'use client'

import { useEffect, useState } from 'react'
import { cardsApi } from '@/lib/api/cards'
import type { Card } from '@/types/card'
import { CardTile } from '@/components/cards/CardTile'
import { AddCardForm } from '@/components/cards/AddCardForm'
import { Skeleton } from '@/components/ui/skeleton'
import { X } from 'lucide-react'

export default function CardsPage() {
  const [cards, setCards] = useState<Card[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)

  useEffect(() => {
    cardsApi.getAll()
      .then(setCards)
      .catch(() => setError('Could not load cards. Make sure the backend is running.'))
      .finally(() => setLoading(false))
  }, [])

  function handleCardCreated(card: Card) {
    setCards(prev => [card, ...prev])
    setShowForm(false)
  }

  return (
      <div className="p-8">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold">Cards</h1>
            <p className="text-sm text-muted-foreground mt-1">Credit card management</p>
          </div>
          <button
            onClick={() => setShowForm(true)}
            className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            + Add Card
          </button>
        </div>

        {/* Add card modal */}
        {showForm && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-xl border border-border bg-background p-6 shadow-lg">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-lg font-semibold">Add Credit Card</h2>
                <button onClick={() => setShowForm(false)} className="text-muted-foreground hover:text-foreground">
                  <X className="h-4 w-4" />
                </button>
              </div>
              <AddCardForm onCreated={handleCardCreated} onCancel={() => setShowForm(false)} />
            </div>
          </div>
        )}

        {/* Loading */}
        {loading && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2].map(i => (
              <div key={i} className="rounded-xl border border-border p-6 space-y-3">
                <Skeleton className="h-5 w-32" />
                <Skeleton className="h-4 w-48" />
                <Skeleton className="h-4 w-24" />
              </div>
            ))}
          </div>
        )}

        {/* Error */}
        {error && (
          <div className="rounded-lg border border-destructive/40 bg-destructive/10 p-4 text-sm text-destructive">
            {error}
          </div>
        )}

        {/* Empty state */}
        {!loading && !error && cards.length === 0 && (
          <div className="rounded-xl border border-dashed border-border p-16 text-center">
            <p className="text-muted-foreground font-medium">No credit cards added yet.</p>
            <p className="text-sm text-muted-foreground mt-1">
              Add a credit card to track spending, cashback, and payments.
            </p>
            <button
              onClick={() => setShowForm(true)}
              className="mt-4 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              Add Card
            </button>
          </div>
        )}

        {/* Card grid */}
        {!loading && !error && cards.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {cards.map(card => (
              <CardTile key={card.id} card={card} />
            ))}
          </div>
        )}
      </div>
  )
}
