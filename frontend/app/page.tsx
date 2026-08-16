"use client"

import Link from 'next/link'
import { ArrowRight, CheckCircle2, AlertTriangle } from 'lucide-react'
import { motion } from 'framer-motion'

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-background text-foreground overflow-x-hidden">
      {/* Atmospheric Glows */}
      <div className="fixed top-0 left-1/2 -translate-x-1/2 w-full h-full pointer-events-none z-0">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] rounded-full bg-primary/10 blur-[120px]" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] rounded-full bg-primary/5 blur-[120px]" />
      </div>

      {/* Nav */}
      <header className="fixed top-4 inset-x-0 z-50 flex justify-center px-4">
        <nav className="flex items-center justify-between w-full max-w-6xl glass-card rounded-full px-6 py-3">
          <span className="text-xl font-bold tracking-tighter text-glow">Finapse</span>
          <Link
            href="/app/money"
            className="rounded-full bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:scale-105 transition-transform active:scale-95 shadow-lg shadow-primary/20"
          >
            Open App
          </Link>
        </nav>
      </header>

      {/* Hero */}
      <section className="relative z-10 mx-auto max-w-5xl px-8 pt-40 pb-24 text-center">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: "easeOut" }}
        >
          <h1 className="text-6xl md:text-8xl font-extrabold tracking-tighter leading-[1.1] bg-gradient-to-b from-white to-white/50 bg-clip-text text-transparent">
            Understand where your<br />
            <span className="text-primary">money actually goes.</span>
          </h1>
          <p className="mt-8 text-xl text-muted-foreground max-w-2xl mx-auto leading-relaxed">
            Import your bank and credit-card statements, reconcile transactions,
            eliminate double-counting, and see your real cash flow in a high-fidelity environment.
          </p>
          <div className="mt-12 flex items-center justify-center gap-6">
            <Link
              href="/app/money"
              className="group flex items-center gap-2 rounded-full bg-primary px-8 py-4 text-base font-bold text-primary-foreground hover:scale-105 transition-all shadow-xl shadow-primary/25"
            >
              Get Started <ArrowRight className="h-5 w-5 group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link
              href="#how-it-works"
              className="rounded-full border border-white/10 px-8 py-4 text-base font-semibold hover:bg-white/5 transition-colors backdrop-blur-sm"
            >
              See How It Works
            </Link>
          </div>
        </motion.div>
      </section>

      {/* Double-counting demo */}
      <section id="how-it-works" className="relative z-10 mx-auto max-w-6xl px-8 pb-32">
        <div className="text-center mb-16">
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight mb-4">The Intelligence Edge</h2>
          <p className="text-muted-foreground">Stop the noise of double-counted transactions.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-center">
          {/* Without Finapse */}
          <div className="rounded-3xl border border-white/5 bg-white/[0.02] p-8 grayscale opacity-60">
            <p className="text-xs font-bold uppercase tracking-widest text-muted-foreground mb-6 flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-muted-foreground" />
              Legacy View
            </p>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <div className="space-y-1">
                  <p className="text-sm font-medium opacity-80">Amazon Purchase</p>
                  <p className="text-xs opacity-50">Credit Card</p>
                </div>
                <span className="text-sm font-mono font-bold text-destructive">−₹2,000</span>
              </div>
              <div className="flex justify-between items-center">
                <div className="space-y-1">
                  <p className="text-sm font-medium opacity-80">Card Bill Payment</p>
                  <p className="text-xs opacity-50">Bank Account</p>
                </div>
                <span className="text-sm font-mono font-bold text-destructive">−₹2,000</span>
              </div>
              <div className="border-t border-white/10 pt-4 flex justify-between items-center">
                <div className="flex items-center gap-2 text-muted-foreground">
                  <AlertTriangle className="h-4 w-4" />
                  <span className="text-xs font-bold uppercase tracking-tighter">Double Counted</span>
                </div>
                <span className="text-lg font-mono font-black text-destructive">₹4,000</span>
              </div>
            </div>
          </div>

          {/* Center Piece */}
          <div className="flex flex-col items-center justify-center py-12">
            <motion.div
              animate={{ rotate: 360 }}
              transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
              className="relative"
            >
              <div className="absolute inset-0 rounded-full bg-primary/20 blur-2xl" />
              <div className="relative rounded-full bg-primary p-6 shadow-2xl shadow-primary/40">
                <span className="text-3xl font-black text-primary-foreground">F</span>
              </div>
            </motion.div>
            <p className="mt-6 text-sm font-medium text-center text-muted-foreground max-w-[200px] leading-tight">
              The Reconciliation Engine
            </p>
          </div>

          {/* With Finapse */}
          <div className="rounded-3xl glass-card p-8 relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-4">
               <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
            </div>
            <p className="text-xs font-bold uppercase tracking-widest text-primary mb-6 flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-primary" />
              Intelligence View
            </p>
            <div className="space-y-4">
              <div className="flex justify-between items-center">
                <div className="space-y-1">
                  <p className="text-sm font-medium">Amazon Purchase</p>
                  <p className="text-xs text-muted-foreground">Expense</p>
                </div>
                <span className="text-sm font-mono font-bold text-destructive">−₹2,000</span>
              </div>
              <div className="flex justify-between items-center">
                <div className="space-y-1">
                  <p className="text-sm font-medium">Card Bill Payment</p>
                  <p className="text-xs text-muted-foreground">Card Settlement ✓</p>
                </div>
                <span className="text-sm font-mono font-bold text-muted-foreground">₹2,000</span>
              </div>
              <div className="border-t border-white/10 pt-4 flex justify-between items-center">
                <div className="flex items-center gap-2 text-primary">
                  <CheckCircle2 className="h-4 w-4" />
                  <span className="text-xs font-bold uppercase tracking-tighter">Real Spending</span>
                </div>
                <span className="text-lg font-mono font-black text-primary text-glow">₹2,000</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="relative z-10 border-t border-white/5 bg-white/[0.02] px-8 py-32">
        <div className="mx-auto max-w-6xl">
          <div className="text-center mb-20">
            <h2 className="text-3xl md:text-5xl font-bold tracking-tight mb-4">Architected for Precision</h2>
            <p className="text-muted-foreground">Professional-grade tools for personal wealth intelligence.</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[
              { title: 'Import Statements',    desc: 'Seamless upload of bank and credit-card CSV statements.' },
              { title: 'Reconcile Payments',   desc: 'Auto-detect credit-card payments and eliminate phantom expenses.' },
              { title: 'Track Cashback',       desc: 'Isolate cashback to determine your true effective spending.' },
              { title: 'Detect Duplicates',    desc: 'Smart flagging of potential duplicate transactions for review.' },
              { title: 'Understand Refunds',   desc: 'Separate refunds from income for accurate net cash flow.' },
              { title: 'Real Cash Flow',       desc: 'High-fidelity visibility into income, spending, and savings.' },
            ].map(({ title, desc }, i) => (
              <motion.div
                key={title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                whileHover={{ y: -5, backgroundColor: 'rgba(255,255,255,0.08)' }}
                className="rounded-2xl glass-card p-6 transition-all cursor-default group"
              >
                <h3 className="text-lg font-bold mb-2 group-hover:text-primary transition-colors">{title}</h3>
                <p className="text-sm text-muted-foreground leading-relaxed">{desc}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      <footer className="relative z-10 border-t border-white/5 px-8 py-12 text-center text-sm text-muted-foreground">
        <div className="max-w-4xl mx-auto flex flex-col md:flex-row justify-between items-center gap-6">
          <p>© 2026 Finapse — Privacy-first personal finance intelligence.</p>
          <p className="opacity-50">Your data never leaves your laptop.</p>
        </div>
      </footer>
    </div>
  )
}
