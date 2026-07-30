'use client'

import { Fragment } from 'react'
import { Car, Accessibility, Zap, ArrowDown, ArrowUp, LogIn, LogOut } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ParkingLot, Spot } from '@/lib/parking'

export function SpotGrid({
  lot,
  flashing,
  onSelectSpot,
}: {
  lot: ParkingLot
  flashing: Set<string>
  onSelectSpot: (spot: Spot) => void
}) {
  return (
    <div className="rounded-2xl border border-border bg-[oklch(0.19_0.015_278)] p-3">
      {/* Entry / Exit indicators */}
      <div className="mb-3 flex items-center justify-between px-1">
        <span className="flex items-center gap-1.5 rounded-full bg-primary/15 px-2.5 py-1 text-[11px] font-medium text-primary">
          <LogIn size={13} aria-hidden="true" /> Entry
        </span>
        <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
          <Legend className="bg-available" label="Free" />
          <Legend className="bg-occupied" label="Taken" />
        </div>
        <span className="flex items-center gap-1.5 rounded-full bg-secondary px-2.5 py-1 text-[11px] font-medium text-muted-foreground">
          <LogOut size={13} aria-hidden="true" /> Exit
        </span>
      </div>

      {/* Scrollable / pannable lot layout */}
      <div className="overflow-x-auto scrollbar-slim">
        <div className="mx-auto flex min-w-max flex-col gap-1 px-2">
          {lot.rows.map((row, rowIndex) => (
            <Fragment key={row.id}>
              <div className="flex items-center gap-2">
                <span className="w-5 shrink-0 text-center font-mono text-xs font-medium text-muted-foreground">
                  {row.letter}
                </span>
                <div className="flex gap-1.5">
                  {row.spots.map((spot) => (
                    <SpotCell
                      key={spot.id}
                      spot={spot}
                      flashing={flashing.has(`${lot.id}:${spot.id}`)}
                      onSelect={() => onSelectSpot(spot)}
                    />
                  ))}
                </div>
              </div>

              {/* Driving lane between paired rows */}
              {rowIndex % 2 === 1 && rowIndex < lot.rows.length - 1 && <Lane />}
            </Fragment>
          ))}
        </div>
      </div>
    </div>
  )
}

function Lane() {
  return (
    <div className="relative my-1 ml-7 flex h-6 items-center overflow-hidden rounded-md bg-[oklch(0.15_0.01_278)]">
      <div className="flex w-full items-center justify-center gap-6 text-muted-foreground/50">
        <ArrowDown size={12} aria-hidden="true" />
        <span className="h-px w-8 border-t border-dashed border-muted-foreground/40" />
        <ArrowUp size={12} aria-hidden="true" />
        <span className="h-px w-8 border-t border-dashed border-muted-foreground/40" />
        <ArrowDown size={12} aria-hidden="true" />
      </div>
    </div>
  )
}

function SpotCell({
  spot,
  flashing,
  onSelect,
}: {
  spot: Spot
  flashing: boolean
  onSelect: () => void
}) {
  const available = spot.status === 'available'
  const KindIcon = spot.kind === 'accessible' ? Accessibility : spot.kind === 'ev' ? Zap : null

  return (
    <button
      type="button"
      disabled={!available}
      onClick={onSelect}
      aria-label={
        available
          ? `Spot ${spot.label} available, tap to book`
          : `Spot ${spot.label} occupied`
      }
      className={cn(
        'relative flex h-16 w-9 flex-col items-center justify-center rounded-md border-t-2 text-[9px] font-medium transition-all duration-200',
        flashing && 'animate-spot-flash',
        available
          ? 'cursor-pointer border-primary bg-primary/20 text-primary hover:bg-primary/35 hover:shadow-[0_0_12px_-2px_var(--primary)] active:scale-95'
          : 'cursor-not-allowed border-occupied bg-occupied/25 text-occupied-foreground',
      )}
    >
      {available ? (
        <>
          {KindIcon ? (
            <KindIcon size={14} aria-hidden="true" />
          ) : (
            <span className="size-1.5 rounded-full bg-primary" aria-hidden="true" />
          )}
          <span className="mt-1 font-mono tabular-nums">{spot.label}</span>
        </>
      ) : (
        <Car size={16} className="opacity-70" aria-hidden="true" />
      )}
    </button>
  )
}

function Legend({ className, label }: { className: string; label: string }) {
  return (
    <span className="flex items-center gap-1">
      <span className={cn('size-2.5 rounded-[3px]', className)} aria-hidden="true" />
      {label}
    </span>
  )
}
