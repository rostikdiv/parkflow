'use client'

import { X, MapPin, Navigation2, CircleParking, AlertTriangle } from 'lucide-react'
import { cn } from '@/lib/utils'
import { lotStats, type ParkingLot, type Spot } from '@/lib/parking'
import { TimeRangePicker } from './time-range-picker'
import { SpotGrid } from './spot-grid'

export function LotDetailsPanel({
  lot,
  flashing,
  fromIso,
  toIso,
  onFromChange,
  onToChange,
  onClose,
  onSelectSpot,
}: {
  lot: ParkingLot
  flashing: Set<string>
  /** ISO string — controlled by parent which owns the GraphQL query */
  fromIso: string
  toIso: string
  onFromChange: (iso: string) => void
  onToChange: (iso: string) => void
  onClose: () => void
  onSelectSpot: (spot: Spot) => void
}) {
  const { total, free } = lotStats(lot)
  const isHistoryMode = new Date(fromIso).getTime() < Date.now() - 60000;

  const handleSelectSpot = (spot: Spot) => {
    if (isHistoryMode) return;
    onSelectSpot(spot);
  };

  return (
    <>
      {/* Mobile scrim */}
      <button
        type="button"
        aria-label="Close panel"
        onClick={onClose}
        className="fixed inset-0 z-[1990] bg-black/50 backdrop-blur-[2px] md:hidden"
      />

      <aside
        className={cn(
          'fixed z-[2000] flex flex-col border-border bg-card elevation-5',
          // Mobile: bottom sheet
          'inset-x-0 bottom-0 max-h-[86vh] rounded-t-3xl border-t',
          'animate-in slide-in-from-bottom duration-300 ease-out',
          // Desktop: right sidebar
          'md:inset-y-0 md:right-0 md:left-auto md:max-h-none md:w-[440px] md:rounded-none md:rounded-l-3xl md:border-l md:border-t-0',
          'md:animate-in md:slide-in-from-right md:fade-in',
        )}
        aria-label={`${lot.name} details`}
      >
        {/* Mobile drag handle */}
        <div className="flex justify-center pt-3 md:hidden">
          <span className="h-1.5 w-10 rounded-full bg-border" aria-hidden="true" />
        </div>

        {/* Header */}
        <header className="flex items-start gap-3 px-5 pb-4 pt-4 md:pt-6">
          <div className="grid size-11 shrink-0 place-items-center rounded-2xl bg-primary/15 text-primary">
            <CircleParking size={22} aria-hidden="true" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="text-pretty text-lg font-semibold leading-tight tracking-tight">
              {lot.name}
            </h2>
            <p className="mt-0.5 flex items-center gap-1 truncate text-sm text-muted-foreground">
              <MapPin size={13} className="shrink-0" aria-hidden="true" />
              {lot.address}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close details"
            className="grid size-9 shrink-0 place-items-center rounded-full text-muted-foreground transition-colors duration-150 hover:bg-accent hover:text-foreground"
          >
            <X size={18} aria-hidden="true" />
          </button>
        </header>

        {/* Stat strip */}
        <div className="grid grid-cols-3 gap-2 px-5 pb-4">
          <Stat label="Rate" value={`₴${lot.hourlyRate.toFixed(0)}/hr`} />
          <Stat label="Free now" value={`${free}/${total}`} highlight={free > 0} />
          <Stat
            label="Status"
            value={free === 0 ? 'Full' : free < total * 0.2 ? 'Almost full' : 'Available'}
            highlight={free > 0}
          />
        </div>

        {/* Scrollable content */}
        <div className="flex flex-col gap-4 overflow-y-auto scrollbar-slim px-5 pb-[max(1.5rem,env(safe-area-inset-bottom))] pt-1">
          <TimeRangePicker
            fromIso={fromIso}
            toIso={toIso}
            onFromChange={onFromChange}
            onToChange={onToChange}
          />

          <div>
            <div className="mb-2 flex items-center justify-between px-1">
              <h3 className="text-sm font-semibold">Select a spot</h3>
              <span className="text-xs text-muted-foreground">Tap a green spot to book</span>
            </div>
            {isHistoryMode && (
              <div className="mb-3 rounded-lg bg-yellow-500/20 border border-yellow-500/30 px-3 py-2 text-xs font-medium text-yellow-600 dark:text-yellow-500 flex items-center gap-2">
                <AlertTriangle size={14} />
                Ви переглядаєте історію. Бронювання недоступне.
              </div>
            )}
            <SpotGrid lot={lot} flashing={flashing} onSelectSpot={handleSelectSpot} />
          </div>
        </div>
      </aside>
    </>
  )
}

function Stat({
  label,
  value,
  icon,
  highlight,
}: {
  label: string
  value: string
  icon?: React.ReactNode
  highlight?: boolean
}) {
  return (
    <div className="flex flex-col gap-0.5 rounded-2xl border border-border bg-secondary/50 px-3 py-2.5">
      <span className="flex items-center gap-1 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
        {icon}
        {label}
      </span>
      <span className={cn('text-sm font-semibold tabular-nums', highlight && 'text-primary')}>
        {value}
      </span>
    </div>
  )
}
