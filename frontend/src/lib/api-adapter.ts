import type { ParkingLot as ApiParkingLot, SpotAvailability } from '../types';
import type { ParkingLot, SpotRow } from './parking';

export function adaptLot(apiLot: ApiParkingLot, apiSpots?: SpotAvailability[]): ParkingLot {
  let rows: SpotRow[] = [];

  if (apiSpots && apiSpots.length > 0) {
    // Group spots by their zone letter (the part before the dash, e.g., 'A' from 'A-1')
    const groupedByZone: Record<string, SpotAvailability[]> = {};

    apiSpots.forEach(s => {
      const parts = s.code.split('-');
      const zone = parts.length > 1 ? parts[0] : s.code.charAt(0) || 'A';
      if (!groupedByZone[zone]) {
        groupedByZone[zone] = [];
      }
      groupedByZone[zone].push(s);
    });

    const ROW_SIZE = 25; // Render up to 25 spots per visual row to avoid infinite scrolling
    
    // Sort zones alphabetically
    const zones = Object.keys(groupedByZone).sort();
    
    zones.forEach(zone => {
      const zoneSpots = groupedByZone[zone];
      
      // Sort spots sequentially within the zone
      zoneSpots.sort((a, b) => {
        const numA = parseInt(a.code.replace(/[^0-9]/g, ''), 10) || 0;
        const numB = parseInt(b.code.replace(/[^0-9]/g, ''), 10) || 0;
        if (numA !== numB) return numA - numB;
        return a.code.localeCompare(b.code);
      });

      // Chunk long zones into multiple visual rows
      for (let i = 0; i < zoneSpots.length; i += ROW_SIZE) {
        const slice = zoneSpots.slice(i, i + ROW_SIZE);
        
        rows.push({
          id: `${zone}-${i}`,
          letter: zone,
          spots: slice.map((s, col) => ({
            id: s.spotId,
            label: s.code,
            status: s.isAnomaly ? 'anomaly' : ((s.isAvailable === true || (s as any).available === true) ? 'available' : 'occupied'),
            // Fixed mapping: V3 schema uses 'EV_CHARGING' and 'DISABLED'
            kind: (s.type === 'DISABLED' || s.type.toLowerCase() === 'accessible') ? 'accessible' 
                : (s.type === 'EV_CHARGING' || s.type.toLowerCase() === 'ev') ? 'ev' 
                : 'standard',
            col: col,
            bookedUntil: s.bookedUntil,
          })),
        });
      }
    });
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
