import { useState, useEffect } from 'react';
import { useQuery, useSubscription, gql } from 'urql';
import { MapPin, CircleDollarSign, Info, X } from 'lucide-react';
import type { ParkingLot, SpotAvailability, SpotStatusEvent } from '../types';
import { SpotGridMap } from './SpotGridMap';
import { ReservationModal } from './ReservationModal';

const AVAILABILITY_QUERY = gql`
  query GetAvailability($lotId: ID!, $from: String!, $to: String!) {
    availability(lotId: $lotId, from: $from, to: $to) {
      spotId
      code
      type
      isAvailable
      layoutX
      layoutY
    }
  }
`;

const SPOT_STATUS_SUBSCRIPTION = gql`
  subscription OnSpotStatusChanged($lotId: ID!) {
    spotStatusChanged(lotId: $lotId) {
      spotId
      status
      at
    }
  }
`;

interface ParkingLotSidebarProps {
  lot: ParkingLot;
  onClose: () => void;
}

export function ParkingLotSidebar({ lot, onClose }: ParkingLotSidebarProps) {
  const [selectedSpot, setSelectedSpot] = useState<SpotAvailability | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  // Hold from/to in state so they don't change on every render
  const [{ from, to }] = useState(() => ({
    from: new Date().toISOString(),
    to: new Date(new Date().getTime() + 2 * 60 * 60 * 1000).toISOString()
  }));

  const [{ data, fetching, error }, reexecuteQuery] = useQuery({
    query: AVAILABILITY_QUERY,
    variables: { lotId: lot.id, from, to },
  });

  // Local state to hold merged spots (initial fetch + real-time updates)
  const [liveSpots, setLiveSpots] = useState<SpotAvailability[]>([]);

  useEffect(() => {
    if (data?.availability) {
      setLiveSpots(data.availability);
    }
  }, [data]);

  // Subscription for real-time updates
  useSubscription(
    { query: SPOT_STATUS_SUBSCRIPTION, variables: { lotId: lot.id } },
    (_, response) => {
      if (response.spotStatusChanged) {
        const event = response.spotStatusChanged as SpotStatusEvent;
        // The event structure we built uses physical status (FREE, OCCUPIED).
        // Let's assume OCCUPIED means isAvailable = false, FREE means isAvailable = true.
        // Or re-execute query to let backend recalculate time overlaps?
        // Let's just optimistic update:
        const isAvail = event.status === 'FREE';
        
        setLiveSpots(prev => prev.map(spot => 
          spot.spotId === event.spotId 
            ? { ...spot, isAvailable: isAvail } 
            : spot
        ));
      }
      return response;
    }
  );

  return (
    <>
      <div className="absolute bottom-0 md:top-0 right-0 h-[60vh] md:h-full w-full md:w-[450px] bg-gray-900/95 backdrop-blur-md shadow-[0_-10px_40px_rgba(0,0,0,0.5)] md:shadow-2xl border-t md:border-t-0 md:border-l border-gray-800 flex flex-col z-[1000] transition-transform duration-300 ease-in-out rounded-t-3xl md:rounded-none">
        {/* Header */}
        <div className="p-6 border-b border-gray-800">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h2 className="text-2xl font-bold text-white mb-1">{lot.name}</h2>
              <p className="text-gray-400 flex items-center gap-1.5 text-sm">
                <MapPin size={16} /> {lot.address}
              </p>
            </div>
            <button onClick={onClose} className="p-2 hover:bg-gray-800 rounded-full text-gray-400 transition-colors">
              <X size={20} />
            </button>
          </div>
          
          <div className="flex gap-4 mt-6">
            <div className="flex items-center gap-2 text-sm text-gray-300 bg-gray-800 px-3 py-1.5 rounded-lg border border-gray-700">
              <CircleDollarSign size={16} className="text-emerald-500" />
              <span>₴{lot.hourlyRate}/h</span>
            </div>
            <div className="flex items-center gap-2 text-sm text-gray-300 bg-gray-800 px-3 py-1.5 rounded-lg border border-gray-700">
              <Info size={16} className="text-blue-500" />
              <span>{lot.type}</span>
            </div>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6 scrollbar-thin scrollbar-thumb-gray-700">
          <h3 className="text-lg font-semibold text-white mb-4">Live Availability</h3>
          
          {fetching && <div className="text-gray-400 animate-pulse">Loading spot data...</div>}
          {error && <div className="text-rose-500 bg-rose-500/10 p-4 rounded-lg">{error.message}</div>}
          
          {!fetching && !error && (
            <SpotGridMap 
              spots={liveSpots} 
              onBookSpot={(spot) => setSelectedSpot(spot)} 
            />
          )}

          <div className="mt-6 bg-blue-500/10 border border-blue-500/20 rounded-xl p-4 flex gap-3">
            <Info className="text-blue-400 shrink-0 mt-0.5" size={20} />
            <p className="text-sm text-blue-200">
              The map updates in real-time. If a car physical leaves or parks on a spot, the sensor emulator will update the status and you will see it here instantly via GraphQL Subscriptions.
            </p>
          </div>
        </div>
      </div>

      {/* Reservation Modal */}
      {selectedSpot && (
        <ReservationModal 
          spot={selectedSpot} 
          lot={lot}
          onClose={() => setSelectedSpot(null)}
          onSuccess={() => {
            setSelectedSpot(null);
            setToastMessage('Reservation confirmed successfully!');
            // Re-fetch to get accurate overlapping state
            reexecuteQuery({ requestPolicy: 'network-only' });
            setTimeout(() => setToastMessage(null), 5000);
          }}
        />
      )}

      {/* Toast */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-[2000] bg-emerald-500 text-white px-6 py-3 rounded-lg shadow-xl font-medium transition-all duration-300">
          {toastMessage}
        </div>
      )}
    </>
  );
}
