// Domain types + mock data for ParkFlow.
// In production these shapes would come from the GraphQL API and spot status
// deltas would arrive over a WebSocket subscription. Here we simulate both.

export type SpotStatus = 'available' | 'occupied'

export type SpotKind = 'standard' | 'accessible' | 'ev'

export interface Spot {
  id: string
  /** Human label, e.g. "A-12" */
  label: string
  status: SpotStatus
  kind: SpotKind
  /** Column index within its row (0-based) */
  col: number
}

export interface SpotRow {
  id: string
  /** Row letter, e.g. "A" */
  letter: string
  spots: Spot[]
}

export interface ParkingLot {
  id: string
  name: string
  address: string
  /** Distance from the user in km */
  distanceKm: number
  /** Hourly rate in USD */
  hourlyRate: number
  /** Position on the stylized map as percentages (0-100) */
  x: number
  y: number
  rows: SpotRow[]
}

export function lotStats(lot: ParkingLot) {
  let total = 0
  let free = 0
  for (const row of lot.rows) {
    for (const spot of row.spots) {
      total += 1
      if (spot.status === 'available') free += 1
    }
  }
  return { total, free, occupied: total - free }
}

// Deterministic PRNG (mulberry32) so the initial dataset is identical on the
// server and the client — this avoids React hydration mismatches. Live updates
// after mount use Math.random freely since they only run in the browser.
function makeRng(seed: number) {
  let a = seed
  return () => {
    a |= 0
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

function makeRow(
  rng: () => number,
  letter: string,
  count: number,
  freeRatio: number,
  specials: Record<number, SpotKind> = {},
): SpotRow {
  const spots: Spot[] = []
  for (let i = 0; i < count; i++) {
    const isFree = rng() < freeRatio
    spots.push({
      id: `${letter}-${i + 1}`,
      label: `${letter}-${i + 1}`,
      status: isFree ? 'available' : 'occupied',
      kind: specials[i] ?? 'standard',
      col: i,
    })
  }
  return { id: letter, letter, spots }
}

export function createLots(): ParkingLot[] {
  const rng = makeRng(20260730)
  return [
    {
      id: 'lot-central',
      name: 'Central Plaza Garage',
      address: '210 Market Street, Downtown',
      distanceKm: 0.4,
      hourlyRate: 4.5,
      x: 46,
      y: 42,
      rows: [
        makeRow(rng, 'A', 10, 0.55, { 0: 'accessible', 1: 'accessible' }),
        makeRow(rng, 'B', 10, 0.35),
        makeRow(rng, 'C', 10, 0.6, { 8: 'ev', 9: 'ev' }),
        makeRow(rng, 'D', 10, 0.5),
      ],
    },
    {
      id: 'lot-harbor',
      name: 'Harbor View Parking',
      address: '58 Wharf Avenue, Marina',
      distanceKm: 1.2,
      hourlyRate: 3.0,
      x: 24,
      y: 66,
      rows: [
        makeRow(rng, 'A', 8, 0.7, { 0: 'accessible' }),
        makeRow(rng, 'B', 8, 0.65),
        makeRow(rng, 'C', 8, 0.8, { 7: 'ev' }),
      ],
    },
    {
      id: 'lot-union',
      name: 'Union Square Lot',
      address: '1 Union Square, Midtown',
      distanceKm: 0.9,
      hourlyRate: 6.25,
      x: 63,
      y: 30,
      rows: [
        makeRow(rng, 'A', 12, 0.2, { 0: 'accessible', 1: 'accessible' }),
        makeRow(rng, 'B', 12, 0.15),
        makeRow(rng, 'C', 12, 0.25, { 10: 'ev', 11: 'ev' }),
        makeRow(rng, 'D', 12, 0.3),
      ],
    },
    {
      id: 'lot-riverside',
      name: 'Riverside Deck',
      address: '400 Riverside Drive, Eastbank',
      distanceKm: 2.1,
      hourlyRate: 2.25,
      x: 78,
      y: 58,
      rows: [
        makeRow(rng, 'A', 9, 0.85, { 0: 'accessible' }),
        makeRow(rng, 'B', 9, 0.9, { 8: 'ev' }),
        makeRow(rng, 'C', 9, 0.75),
      ],
    },
    {
      id: 'lot-northgate',
      name: 'Northgate Station',
      address: '77 Transit Way, North End',
      distanceKm: 1.7,
      hourlyRate: 3.75,
      x: 40,
      y: 18,
      rows: [
        makeRow(rng, 'A', 10, 0.45, { 0: 'accessible' }),
        makeRow(rng, 'B', 10, 0.4, { 9: 'ev' }),
        makeRow(rng, 'C', 10, 0.5),
      ],
    },
  ]
}
