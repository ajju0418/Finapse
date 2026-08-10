import Link from 'next/link'
import { ArrowRight, CheckCircle2, AlertTriangle } from 'lucide-react'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      {/* Nav */}
      <header className="flex items-center justify-between border-b border-border px-8 py-4">
        <span className="text-xl font-bold tracking-tight">Finapse</span>
        <Link
          href="/app/money"
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          Open App
        </Link>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-4xl px-8 py-24 text-center">
        <h1 className="text-5xl font-bold tracking-tight leading-tight">
          Understand where your<br />money actually goes.
        </h1>
        <p className="mt-6 text-lg text-muted-foreground max-w-2xl mx-auto">
          Import your bank and credit-card statements, reconcile transactions,
          eliminate double-counting, and see your real cash flow.
        </p>
        <div className="mt-10 flex items-center justify-center gap-4">
          <Link
            href="/app/money"
            className="flex items-center gap-2 rounded-md bg-primary px-6 py-3 text-sm font-semibold text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Get Started <ArrowRight className="h-4 w-4" />
          </Link>
          <Link
            href="#how-it-works"
            className="rounded-md border border-border px-6 py-3 text-sm font-semibold hover:bg-accent transition-colors"
          >
            See How It Works
          </Link>
        </div>
      </section>

      {/* Double-counting demo */}
      <section id="how-it-works" className="mx-auto max-w-4xl px-8 pb-24">
        <h2 className="text-center text-2xl font-bold mb-12">The Problem Finapse Solves</h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
          {/* Without Finapse */}
          <div className="rounded-xl border border-border p-6">
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-4">
              Without Finapse
            </p>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-sm font-medium">Amazon Purchase</p>
                  <p className="text-xs text-muted-foreground">Credit Card</p>
                </div>
                <span className="text-sm font-semibold text-destructive">−₹2,000</span>
              </div>
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-sm font-medium">Card Bill Payment</p>
                  <p className="text-xs text-muted-foreground">Bank Account</p>
                </div>
                <span className="text-sm font-semibold text-destructive">−₹2,000</span>
              </div>
              <div className="border-t border-border pt-3 flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <AlertTriangle className="h-4 w-4 text-yellow-500" />
                  <span className="text-sm font-semibold">Counted as</span>
                </div>
                <span className="text-sm font-bold text-destructive">₹4,000</span>
              </div>
            </div>
          </div>

          {/* Arrow */}
          <div className="flex flex-col items-center gap-2">
            <div className="rounded-full bg-primary/10 p-4">
              <span className="text-2xl font-bold text-primary">F</span>
            </div>
            <p className="text-sm font-medium text-center text-muted-foreground">
              Finapse recognises the relationship
            </p>
          </div>

          {/* With Finapse */}
          <div className="rounded-xl border border-primary/40 bg-primary/5 p-6">
            <p className="text-xs font-semibold uppercase tracking-wider text-primary mb-4">
              With Finapse
            </p>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-sm font-medium">Amazon Purchase</p>
                  <p className="text-xs text-muted-foreground">Expense</p>
                </div>
                <span className="text-sm font-semibold text-destructive">−₹2,000</span>
              </div>
              <div className="flex justify-between items-center">
                <div>
                  <p className="text-sm font-medium">Card Bill Payment</p>
                  <p className="text-xs text-muted-foreground">Card Settlement ✓</p>
                </div>
                <span className="text-sm font-semibold text-muted-foreground">₹2,000</span>
              </div>
              <div className="border-t border-border pt-3 flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <CheckCircle2 className="h-4 w-4 text-green-500" />
                  <span className="text-sm font-semibold">Actual Spending</span>
                </div>
                <span className="text-sm font-bold text-green-600">₹2,000</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="border-t border-border bg-muted/30 px-8 py-20">
        <div className="mx-auto max-w-4xl">
          <h2 className="text-center text-2xl font-bold mb-12">What Finapse Does</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            {[
              { title: 'Import Statements',    desc: 'Upload bank and credit-card CSV statements.' },
              { title: 'Reconcile Payments',   desc: 'Detect credit-card payments and avoid double-counting.' },
              { title: 'Track Cashback',       desc: 'Identify cashback and calculate effective spending.' },
              { title: 'Detect Duplicates',    desc: 'Flag potential duplicate transactions for review.' },
              { title: 'Understand Refunds',   desc: 'Separate refunds from income for accurate net spending.' },
              { title: 'Real Cash Flow',       desc: 'See income, actual spending, and net cash flow clearly.' },
            ].map(({ title, desc }) => (
              <div key={title} className="rounded-lg border border-border bg-background p-5">
                <h3 className="font-semibold mb-1">{title}</h3>
                <p className="text-sm text-muted-foreground">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-border px-8 py-6 text-center text-sm text-muted-foreground">
        Finapse — Privacy-first personal finance intelligence. Your data stays on your laptop.
      </footer>
    </div>
  )
}
