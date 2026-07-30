'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { createLots, type ParkingLot, type SpotStatus } from '@/lib/parking'

/**
 * Simulates a live parking feed. In production this would open a WebSocket
 * subscription and merge incoming spot-status deltas into local state.
 * Here we flip a few random spots on an interval so the UI updates without
 * any page reload — mirroring real-time behavior.
 */
export function useParking() {
  const [lots, setLots] = useState<ParkingLot[]>(() => createLots())
  // Set of spot ids that changed on the most recent tick (for flash animation).
  const [flashing, setFlashing] = useState<Set<string>>(new Set())
  const flashTimeout = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    const interval = setInterval(() => {
      setLots((prev) => {
        const next = prev.map((lot) => ({
          ...lot,
          rows: lot.rows.map((row) => ({ ...row, spots: row.spots.map((s) => ({ ...s })) })),
        }))
        const changed = new Set<string>()

        // Flip 1-3 random spots across all lots each tick.
        const flips = 1 + Math.floor(Math.random() * 3)
        for (let i = 0; i < flips; i++) {
          const lot = next[Math.floor(Math.random() * next.length)]
          const row = lot.rows[Math.floor(Math.random() * lot.rows.length)]
          const spot = row.spots[Math.floor(Math.random() * row.spots.length)]
          spot.status = spot.status === 'available' ? 'occupied' : 'available'
          changed.add(`${lot.id}:${spot.id}`)
        }

        setFlashing(changed)
        return next
      })
    }, 2600)

    return () => clearInterval(interval)
  }, [])

  // Clear flash markers shortly after they are set.
  useEffect(() => {
    if (flashing.size === 0) return
    if (flashTimeout.current) clearTimeout(flashTimeout.current)
    flashTimeout.current = setTimeout(() => setFlashing(new Set()), 900)
    return () => {
      if (flashTimeout.current) clearTimeout(flashTimeout.current)
    }
  }, [flashing])

  /** Optimistically mark a spot with a given status (used after booking). */
  const setSpotStatus = useCallback((lotId: string, spotId: string, status: SpotStatus) => {
    setLots((prev) =>
      prev.map((lot) =>
        lot.id !== lotId
          ? lot
          : {
              ...lot,
              rows: lot.rows.map((row) => ({
                ...row,
                spots: row.spots.map((s) => (s.id === spotId ? { ...s, status } : s)),
              })),
            },
      ),
    )
  }, [])

  return { lots, flashing, setSpotStatus }
}

/** Simulate a network booking request. Resolves after a short delay. */
export function submitBooking(): Promise<{ ok: true }> {
  return new Promise((resolve) => setTimeout(() => resolve({ ok: true }), 1400))
}
