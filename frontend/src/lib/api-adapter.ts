import type { ParkingLot as ApiParkingLot, SpotAvailability } from '../types';
import type { ParkingLot, SpotRow } from './parking';

export function adaptLot(apiLot: ApiParkingLot, apiSpots?: SpotAvailability[]): ParkingLot {
  let rows: SpotRow[] = [];
  
  if (apiSpots && apiSpots.length > 0) {
    // For v0 SpotGrid, we just chunk the spots into logical rows of 12.
    // The physical layoutX/Y from the backend doesn't fit the flex-based UI well for large lots.
    const ROW_SIZE = 12;
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    
    // Sort spots by code so they appear sequentially
    const sortedSpots = [...apiSpots].sort((a, b) => {
      // Try to parse numbers from code (e.g. "M-1" vs "M-10")
      const numA = parseInt(a.code.replace(/[^0-9]/g, ''), 10) || 0;
      const numB = parseInt(b.code.replace(/[^0-9]/g, ''), 10) || 0;
      if (numA !== numB) return numA - numB;
      return a.code.localeCompare(b.code);
    });

    for (let i = 0; i < sortedSpots.length; i += ROW_SIZE) {
      const slice = sortedSpots.slice(i, i + ROW_SIZE);
      const letter = letters[Math.floor(i / ROW_SIZE)] || `R${Math.floor(i / ROW_SIZE)}`;
      
      rows.push({
        id: letter,
        letter: letter,
        spots: slice.map((s, col) => ({
          id: s.spotId,
          label: s.code,
          status: s.isAnomaly ? 'anomaly' : ((s.isAvailable === true || (s as any).available === true) ? 'available' : 'occupied'),
          kind: (s.type.toLowerCase() === 'accessible' || s.type.toLowerCase() === 'ev') ? (s.type.toLowerCase() as any) : 'standard',
          col: col,
          bookedUntil: s.bookedUntil,
        })),
      });
    }
  }

  return {
    id: apiLot.id,
    name: apiLot.name,
    address: apiLot.address,
    // Distance requires the user's GPS location — not available at MVP; set to 0
    distanceKm: 0,
    hourlyRate: apiLot.hourlyRate || 0,
    x: 0,
    y: 0,
    rows,
  };
}
