'use client'

import { useState } from 'react'
import { Plus, Minus, Navigation, Radio } from 'lucide-react'
import { cn } from '@/lib/utils'
import type { ParkingLot } from '@/lib/parking'
import { LotPin } from './lot-pin'
import { SearchBar } from './search-bar'
import { ProfileAvatar } from './profile-avatar'

export function MapView({
  lots,
  selectedLotId,
  onSelectLot,
  query,
  onQueryChange,
}: {
  lots: ParkingLot[]
  selectedLotId: string | null
  onSelectLot: (id: string) => void
  query: string
  onQueryChange: (value: string) => void
}) {
  const [zoom, setZoom] = useState(1)

  return (
    <div className="absolute inset-0 overflow-hidden bg-background">
      {/* Map surface */}
      <div
        className="absolute inset-0 origin-center transition-transform duration-300 ease-out"
        style={{ transform: `scale(${zoom})` }}
      >
        <img
          src="/map-dark.png"
          alt=""
          aria-hidden="true"
          className="size-full object-cover opacity-90"
        />
        <div className="absolute inset-0 bg-background/20" />

        {/* User location marker */}
        <div
          className="absolute -translate-x-1/2 -translate-y-1/2"
          style={{ left: '50%', top: '82%' }}
        >
          <span className="absolute inset-0 -m-4 animate-live-pulse rounded-full bg-[oklch(0.7_0.14_240)]/30" />
          <span className="relative block size-4 rounded-full border-2 border-background bg-[oklch(0.7_0.14_240)] elevation-2" />
        </div>

        {/* Parking lot pins */}
        {lots.map((lot) => (
          <LotPin
            key={lot.id}
            lot={lot}
            active={lot.id === selectedLotId}
            onSelect={() => onSelectLot(lot.id)}
          />
        ))}
      </div>

      {/* Top bar: search + profile */}
      <header className="absolute inset-x-0 top-0 z-30 flex items-start gap-3 p-4 pt-[max(1rem,env(safe-area-inset-top))]">
        <div className="flex min-w-0 flex-1 flex-col gap-2">
          <div className="flex items-center gap-2 px-1">
            <span className="grid size-6 place-items-center rounded-md bg-primary text-primary-foreground">
              <Navigation size={14} aria-hidden="true" />
            </span>
            <span className="text-sm font-semibold tracking-tight">ParkFlow</span>
            <span className="ml-1 flex items-center gap-1 rounded-full bg-card/90 px-2 py-0.5 text-[10px] font-medium text-primary backdrop-blur-md">
              <Radio size={11} className="animate-live-pulse" aria-hidden="true" />
              Live
            </span>
          </div>
          <SearchBar value={query} onChange={onQueryChange} />
        </div>
        <ProfileAvatar />
      </header>

      {/* Zoom controls (FAB group) */}
      <div className="absolute bottom-6 right-4 z-30 flex flex-col gap-2">
        <ZoomButton
          label="Zoom in"
          onClick={() => setZoom((z) => Math.min(1.6, +(z + 0.2).toFixed(1)))}
        >
          <Plus size={20} aria-hidden="true" />
        </ZoomButton>
        <ZoomButton
          label="Zoom out"
          onClick={() => setZoom((z) => Math.max(0.8, +(z - 0.2).toFixed(1)))}
        >
          <Minus size={20} aria-hidden="true" />
        </ZoomButton>
        <button
          type="button"
          aria-label="Recenter on my location"
          className="mt-1 grid size-14 place-items-center rounded-2xl bg-primary text-primary-foreground elevation-3 transition-transform duration-150 hover:scale-105 active:scale-95"
        >
          <Navigation size={22} aria-hidden="true" />
        </button>
      </div>
    </div>
  )
}

function ZoomButton({
  children,
  label,
  onClick,
}: {
  children: React.ReactNode
  label: string
  onClick: () => void
}) {
  return (
    <button
      type="button"
      aria-label={label}
      onClick={onClick}
      className={cn(
        'grid size-12 place-items-center rounded-xl border border-border bg-card/90 text-foreground backdrop-blur-md elevation-2',
        'transition-transform duration-150 hover:scale-105 active:scale-95',
      )}
    >
      {children}
    </button>
  )
}
