'use client'

import { useState } from 'react'
import { useQuery, gql } from 'urql'
import { X, CalendarDays, Clock, Tag, CircleX, Loader2, CheckCircle2, CircleDot, MapPin, ParkingCircle } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useAuth } from '../../lib/auth'

const MY_RESERVATIONS_QUERY = gql`
  query MyReservations($page: Int!, $size: Int!) {
    myReservations(page: $page, size: $size) {
      content {
        id
        spotId
        spotCode
        lotId
        lotName
        licensePlate
        startTime
        endTime
        status
        totalPrice
      }
      totalPages
      totalElements
      number
    }
  }
`

type Reservation = {
  id: string
  spotId: string
  spotCode: string
  lotId: string
  lotName: string
  licensePlate: string
  startTime: string
  endTime: string
  status: string
  totalPrice: number
}

interface MyReservationsPanelProps {
  onClose: () => void
  onCancelReservation: (id: string) => Promise<void>
}

export function MyReservationsPanel({ onClose, onCancelReservation }: MyReservationsPanelProps) {
  const [page, setPage] = useState(0)
  const size = 10

  const [{ data, fetching, error }] = useQuery({ 
    query: MY_RESERVATIONS_QUERY,
    variables: { page, size }
  })

  const reservations: Reservation[] = data?.myReservations?.content ?? []
  const totalPages = data?.myReservations?.totalPages ?? 0
  const active = reservations.filter(r => r.status === 'PENDING' || r.status === 'CONFIRMED')
  const past   = reservations.filter(r => r.status !== 'PENDING' && r.status !== 'CONFIRMED')

  return (
    <>
      {/* Mobile scrim */}
      <button
        type="button"
        aria-label="Close reservations"
        onClick={onClose}
        className="fixed inset-0 z-[1990] bg-black/50 backdrop-blur-[2px] md:hidden"
      />

      <aside
        className={cn(
          'fixed z-[2000] flex flex-col border-border bg-card elevation-5',
          'inset-x-0 bottom-0 max-h-[86vh] rounded-t-3xl border-t',
          'animate-in slide-in-from-bottom duration-300 ease-out',
          'md:inset-y-0 md:right-0 md:left-auto md:max-h-none md:w-[440px] md:rounded-none md:rounded-l-3xl md:border-l md:border-t-0',
          'md:animate-in md:slide-in-from-right md:fade-in',
        )}
        aria-label="My reservations"
      >
        {/* Drag handle */}
        <div className="flex justify-center pt-3 md:hidden">
          <span className="h-1.5 w-10 rounded-full bg-border" aria-hidden="true" />
        </div>

        {/* Header */}
        <header className="flex items-center gap-3 border-b border-border px-5 py-4">
          <div className="grid size-10 shrink-0 place-items-center rounded-2xl bg-primary/15 text-primary">
            <CalendarDays size={20} aria-hidden="true" />
          </div>
          <div className="flex-1">
            <h2 className="text-lg font-semibold">My Reservations</h2>
            <p className="text-xs text-muted-foreground">{data?.myReservations?.totalElements ?? 0} booking{(data?.myReservations?.totalElements !== 1) ? 's' : ''}</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="grid size-9 place-items-center rounded-full text-muted-foreground hover:bg-accent hover:text-foreground transition-colors"
          >
            <X size={18} />
          </button>
        </header>

        {/* Content */}
        <div className="flex flex-col gap-3 overflow-y-auto scrollbar-slim px-5 py-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
          {fetching && (
            <div className="flex items-center justify-center py-12 text-muted-foreground">
              <Loader2 size={24} className="animate-spin mr-2" />
              Loading…
            </div>
          )}

          {error && (
            <div className="rounded-xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
              Failed to load reservations. Please try again.
            </div>
          )}

          {!fetching && !error && reservations.length === 0 && (
            <div className="flex flex-col items-center justify-center py-16 text-center text-muted-foreground gap-3">
              <CalendarDays size={40} className="opacity-30" />
              <p className="text-sm">No reservations yet.</p>
              <p className="text-xs opacity-70">Book a parking spot to see it here.</p>
            </div>
          )}

          {active.length > 0 && (
            <>
              <p className="px-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Active</p>
              {active.map(r => (
                <ReservationCard key={r.id} reservation={r} onCancel={onCancelReservation} />
              ))}
            </>
          )}

          {past.length > 0 && (
            <>
              <p className="mt-2 px-1 text-xs font-semibold uppercase tracking-wide text-muted-foreground">Past</p>
              {past.map(r => (
                <ReservationCard key={r.id} reservation={r} onCancel={onCancelReservation} />
              ))}
            </>
          )}

          {totalPages > 1 && (
            <div className="flex items-center justify-between py-4 mt-2 border-t border-border/50">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
              >
                Previous
              </button>
              <span className="text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-sm font-medium border border-border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary transition-colors"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </aside>
    </>
  )
}

function ReservationCard({
  reservation: r,
  onCancel,
}: {
  reservation: Reservation
  onCancel: (id: string) => Promise<void>
}) {
  const { token } = useAuth()

  const start = new Date(r.startTime)
  const end   = new Date(r.endTime)
  const isCancellable = r.status === 'PENDING' || r.status === 'CONFIRMED'

  const StatusIcon =
    r.status === 'CONFIRMED' ? CheckCircle2
    : r.status === 'PENDING' ? CircleDot
    : CircleX

  const statusColor =
    r.status === 'CONFIRMED' ? 'text-primary'
    : r.status === 'PENDING' ? 'text-[oklch(0.78_0.15_85)]'
    : 'text-muted-foreground'

  async function handleCancel() {
    try {
      const res = await fetch(`/api/v1/reservations/${r.id}`, {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${token}` },
      })
      if (!res.ok) throw new Error()
      await onCancel(r.id)
    } catch {
      // parent handles notification
    }
  }

  return (
    <div className="rounded-2xl border border-border bg-secondary/40 p-4 space-y-3">
      {/* Status & license plate */}
      <div className="flex items-center justify-between">
        <span className={cn('flex items-center gap-1.5 text-xs font-semibold', statusColor)}>
          <StatusIcon size={14} aria-hidden="true" />
          {r.status}
        </span>
        <span className="rounded-full bg-card px-2.5 py-0.5 font-mono text-xs font-medium tracking-widest text-foreground border border-border">
          {r.licensePlate}
        </span>
      </div>

      {/* Parking lot & spot */}
      <div className="flex flex-col gap-1.5 rounded-xl bg-card/60 px-3 py-2.5 border border-border/60">
        <div className="flex items-center gap-2 text-sm text-foreground">
          <MapPin size={13} className="shrink-0 text-muted-foreground" aria-hidden="true" />
          <span className="font-medium truncate">{r.lotName}</span>
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <ParkingCircle size={13} className="shrink-0" aria-hidden="true" />
          <span>Spot <span className="font-mono font-semibold text-foreground">{r.spotCode}</span></span>
        </div>
      </div>

      {/* Time range */}
      <div className="flex items-center gap-2 text-sm text-foreground">
        <Clock size={13} className="shrink-0 text-muted-foreground" aria-hidden="true" />
        <span>
          {start.toLocaleDateString(undefined, { day: '2-digit', month: 'short' })}&nbsp;
          {start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
          {' – '}
          {end.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
        </span>
      </div>

      {/* Price */}
      <div className="flex items-center justify-between">
        <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
          <Tag size={12} aria-hidden="true" />
          Total
        </span>
        <span className="text-base font-bold tabular-nums text-primary">
          ₴{Number(r.totalPrice).toFixed(2)}
        </span>
      </div>

      {/* Cancel action */}
      {isCancellable && (
        <button
          type="button"
          onClick={handleCancel}
          className="w-full rounded-xl border border-destructive/40 py-2 text-sm font-medium text-destructive transition-colors hover:bg-destructive/10 active:scale-[0.98]"
        >
          Cancel reservation
        </button>
      )}
    </div>
  )
}
