'use client'

import { cn } from '@/lib/utils'
import { lotStats, type ParkingLot } from '@/lib/parking'

export function LotPin({
  lot,
  active,
  onSelect,
}: {
  lot: ParkingLot
  active: boolean
  onSelect: () => void
}) {
  const { total, free } = lotStats(lot)
  const ratio = free / total
  // Status color: plenty (>40%) emerald, some (>10%) amber, full red.
  const dot =
    ratio > 0.4
      ? 'bg-primary'
      : ratio > 0.1
        ? 'bg-[oklch(0.78_0.15_85)]'
        : 'bg-destructive'

  return (
    <button
      type="button"
      onClick={onSelect}
      aria-label={`${lot.name}, ${free} spots free, ₴${lot.hourlyRate.toFixed(2)} per hour`}
      className="absolute -translate-x-1/2 -translate-y-full focus:outline-none"
      style={{ left: `${lot.x}%`, top: `${lot.y}%` }}
    >
      <div
        className={cn(
          'flex items-center gap-2 rounded-full border py-1.5 pl-1.5 pr-3 backdrop-blur-md transition-all duration-150',
          active
            ? 'border-primary bg-primary text-primary-foreground elevation-4 scale-105'
            : 'border-border bg-card/95 text-card-foreground elevation-2 hover:scale-105',
        )}
      >
        <span
          className={cn(
            'grid size-7 place-items-center rounded-full text-[11px] font-bold tabular-nums',
            active ? 'bg-primary-foreground/15 text-primary-foreground' : 'bg-secondary text-foreground',
          )}
        >
          <span className={cn('size-2 rounded-full', dot, ratio <= 0.1 && 'animate-live-pulse')} />
        </span>
        <span className="flex flex-col items-start leading-tight">
          <span className="text-xs font-semibold tabular-nums">
            {free} free
          </span>
          <span className={cn('text-[10px]', active ? 'text-primary-foreground/80' : 'text-muted-foreground')}>
            ₴{lot.hourlyRate.toFixed(2)}/hr
          </span>
        </span>
      </div>
      {/* Pin stem */}
      <span
        className={cn(
          'mx-auto block size-2.5 -translate-y-1 rotate-45 rounded-[2px] border',
          active ? 'border-primary bg-primary' : 'border-border bg-card',
        )}
        aria-hidden="true"
      />
    </button>
  )
}
