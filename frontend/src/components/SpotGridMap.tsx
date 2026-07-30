import { useMemo } from 'react';
import type { SpotAvailability } from '../types';

interface SpotGridMapProps {
  spots: SpotAvailability[];
  onBookSpot: (spot: SpotAvailability) => void;
}

// Car SVG icon for occupied spots
function CarIcon() {
  return (
    <svg viewBox="0 0 24 24" className="w-5 h-5 text-rose-200/60 fill-current">
      <path d="M5 11l1.5-4.5h11L19 11M17 16a1 1 0 01-1 1 1 1 0 01-1-1 1 1 0 011-1 1 1 0 011 1M7 16a1 1 0 01-1 1 1 1 0 01-1-1 1 1 0 011-1 1 1 0 011 1M19.5 11H5.5A1.5 1.5 0 004 12.5V16h16v-3.5A1.5 1.5 0 0019.5 11z"/>
    </svg>
  );
}

export function SpotGridMap({ spots, onBookSpot }: SpotGridMapProps) {
  // Group spots by row (layoutY), sorted by X within each row
  const rows = useMemo(() => {
    if (spots.length === 0) return [];

    // Detect if all layoutX/Y are zero (no coordinates set) → fallback to auto-grid
    const allZero = spots.every(s => s.layoutX === 0 && s.layoutY === 0);
    if (allZero) {
      // Fallback: arrange in a grid of up to 8 per row
      const ROW_SIZE = 8;
      const result: SpotAvailability[][] = [];
      for (let i = 0; i < spots.length; i += ROW_SIZE) {
        result.push(spots.slice(i, i + ROW_SIZE));
      }
      return result;
    }

    const rowMap = new Map<number, SpotAvailability[]>();
    spots.forEach(spot => {
      const y = Math.round(spot.layoutY);
      if (!rowMap.has(y)) rowMap.set(y, []);
      rowMap.get(y)!.push(spot);
    });
    rowMap.forEach(row => row.sort((a, b) => a.layoutX - b.layoutX));
    return Array.from(rowMap.entries())
      .sort(([a], [b]) => a - b)
      .map(([, row]) => row);
  }, [spots]);

  const available = spots.filter(s => s.isAvailable).length;

  if (spots.length === 0) {
    return (
      <div className="flex items-center justify-center h-40 bg-gray-800/50 rounded-xl border border-gray-700 text-gray-500 text-sm">
        No spot data available
      </div>
    );
  }

  return (
    <div className="rounded-xl overflow-hidden border border-gray-700 shadow-inner bg-[#1e2328]">

      {/* Stats bar */}
      <div className="flex items-center justify-between px-4 py-2.5 bg-gray-900/80 border-b border-gray-700/60">
        <span className="text-sm font-semibold text-white">
          {available}
          <span className="text-gray-400 font-normal">/{spots.length} available</span>
        </span>
        <div className="flex gap-3 text-xs text-gray-400">
          <span className="flex items-center gap-1.5">
            <div className="w-2.5 h-4 rounded-sm bg-emerald-500 border border-emerald-400/40 shadow-[0_0_6px_rgba(52,211,153,0.4)]"/>
            Free
          </span>
          <span className="flex items-center gap-1.5">
            <div className="w-2.5 h-4 rounded-sm bg-rose-600 border border-rose-500/40"/>
            Taken
          </span>
        </div>
      </div>

      {/* The parking lot view — scrollable */}
      <div className="overflow-auto p-3 md:p-4" style={{ maxHeight: '380px' }}>
        {/* Entry/Exit indicator */}
        <div className="flex items-center gap-2 mb-3">
          <div className="flex-1 border-t-2 border-dashed border-gray-600"/>
          <span className="text-[10px] font-bold text-gray-500 tracking-widest uppercase whitespace-nowrap">
            ▲ ENTRY / EXIT ▲
          </span>
          <div className="flex-1 border-t-2 border-dashed border-gray-600"/>
        </div>

        <div className="space-y-0">
          {rows.map((row, rowIndex) => (
            <div key={rowIndex}>

              {/* Driving aisle between rows */}
              {rowIndex > 0 && (
                <div className="relative h-7 md:h-8 mx-0 my-1 bg-gray-700/30 rounded-sm flex items-center overflow-hidden">
                  {/* Yellow dashed center line */}
                  <div className="w-full border-t-2 border-dashed border-yellow-400/25"/>
                  {/* Lane arrows */}
                  <span className="absolute left-2 text-yellow-400/20 text-xs font-bold">→→→→→→→</span>
                </div>
              )}

              {/* Row of parking spots */}
              <div className="flex gap-1 md:gap-1.5 flex-wrap">
                {row.map(spot => (
                  <button
                    key={spot.spotId}
                    onClick={() => spot.isAvailable && onBookSpot(spot)}
                    disabled={!spot.isAvailable}
                    title={`${spot.code} — ${spot.isAvailable ? 'Available, click to book' : 'Occupied'} (${spot.type})`}
                    className={[
                      // Base: tall vertical rectangle like a real parking spot
                      'relative flex flex-col items-center justify-between rounded-sm border-2 transition-all duration-200',
                      'w-[38px] h-[68px] md:w-[48px] md:h-[88px]',
                      // White lane stripe lines on sides (via box-shadow trick)
                      spot.isAvailable
                        ? 'border-emerald-400/60 bg-emerald-900/60 hover:bg-emerald-500/80 hover:border-emerald-300 hover:shadow-[0_0_12px_rgba(52,211,153,0.5)] cursor-pointer active:scale-95'
                        : 'border-rose-500/40 bg-rose-900/40 cursor-not-allowed',
                    ].join(' ')}
                  >
                    {/* Top lane-marking stripe */}
                    <div className={`w-full h-[3px] rounded-t-sm ${spot.isAvailable ? 'bg-emerald-400/60' : 'bg-rose-500/30'}`}/>

                    {/* Center icon + code */}
                    <div className="flex flex-col items-center gap-0.5 px-0.5">
                      {spot.isAvailable ? (
                        <div className="w-5 h-5 md:w-6 md:h-6 opacity-30">
                          {/* Empty spot: faint car outline */}
                          <svg viewBox="0 0 24 24" className="w-full h-full text-emerald-300 fill-current">
                            <path d="M5 11l1.5-4.5h11L19 11M17 16a1 1 0 01-1 1 1 1 0 01-1-1 1 1 0 011-1 1 1 0 011 1M7 16a1 1 0 01-1 1 1 1 0 01-1-1 1 1 0 011-1 1 1 0 011 1M19.5 11H5.5A1.5 1.5 0 004 12.5V16h16v-3.5A1.5 1.5 0 0019.5 11z"/>
                          </svg>
                        </div>
                      ) : (
                        <CarIcon />
                      )}
                      <span className={`text-[8px] md:text-[9px] font-bold tracking-tight leading-none ${spot.isAvailable ? 'text-emerald-300' : 'text-rose-300/70'}`}>
                        {spot.code}
                      </span>
                    </div>

                    {/* Bottom lane-marking stripe */}
                    <div className={`w-full h-[3px] rounded-b-sm ${spot.isAvailable ? 'bg-emerald-400/60' : 'bg-rose-500/30'}`}/>
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>

        {/* Bottom exit indicator */}
        <div className="flex items-center gap-2 mt-3">
          <div className="flex-1 border-t-2 border-dashed border-gray-600"/>
          <span className="text-[10px] font-bold text-gray-500 tracking-widest uppercase whitespace-nowrap">
            ▼ EXIT ▼
          </span>
          <div className="flex-1 border-t-2 border-dashed border-gray-600"/>
        </div>
      </div>
    </div>
  );
}
