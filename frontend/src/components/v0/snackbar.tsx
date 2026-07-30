'use client'

import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { CheckCircle2, Info, X, AlertTriangle } from 'lucide-react'
import { cn } from '@/lib/utils'

type SnackKind = 'success' | 'error' | 'info'

interface Snack {
  id: number
  message: string
  kind: SnackKind
}

interface SnackbarContextValue {
  notify: (message: string, kind?: SnackKind) => void
}

const SnackbarContext = createContext<SnackbarContextValue | null>(null)

export function useSnackbar() {
  const ctx = useContext(SnackbarContext)
  if (!ctx) throw new Error('useSnackbar must be used within <SnackbarProvider>')
  return ctx
}

let counter = 0

export function SnackbarProvider({ children }: { children: React.ReactNode }) {
  const [snacks, setSnacks] = useState<Snack[]>([])

  const dismiss = useCallback((id: number) => {
    setSnacks((prev) => prev.filter((s) => s.id !== id))
  }, [])

  const notify = useCallback(
    (message: string, kind: SnackKind = 'info') => {
      const id = ++counter
      setSnacks((prev) => [...prev, { id, message, kind }])
      setTimeout(() => dismiss(id), 4200)
    },
    [dismiss],
  )

  const value = useMemo(() => ({ notify }), [notify])

  return (
    <SnackbarContext.Provider value={value}>
      {children}
      <div
        aria-live="polite"
        role="status"
        className="pointer-events-none fixed inset-x-0 bottom-0 z-[100] flex flex-col items-center gap-2 p-4 pb-[max(1rem,env(safe-area-inset-bottom))]"
      >
        {snacks.map((snack) => (
          <SnackbarItem key={snack.id} snack={snack} onDismiss={() => dismiss(snack.id)} />
        ))}
      </div>
    </SnackbarContext.Provider>
  )
}

function SnackbarItem({ snack, onDismiss }: { snack: Snack; onDismiss: () => void }) {
  const Icon =
    snack.kind === 'success' ? CheckCircle2 : snack.kind === 'error' ? AlertTriangle : Info

  return (
    <div
      className={cn(
        'pointer-events-auto flex w-full max-w-md items-center gap-3 rounded-xl border border-border bg-popover px-4 py-3 text-popover-foreground elevation-3',
        'animate-in fade-in slide-in-from-bottom-4 duration-200',
      )}
    >
      <Icon
        size={20}
        className={cn(
          'shrink-0',
          snack.kind === 'success' && 'text-primary',
          snack.kind === 'error' && 'text-destructive',
          snack.kind === 'info' && 'text-muted-foreground',
        )}
        aria-hidden="true"
      />
      <p className="min-w-0 flex-1 text-sm leading-relaxed">{snack.message}</p>
      <button
        type="button"
        onClick={onDismiss}
        aria-label="Dismiss notification"
        className="grid size-8 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
      >
        <X size={16} aria-hidden="true" />
      </button>
    </div>
  )
}
