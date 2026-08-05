import { useState, useEffect } from 'react';
import { useQuery, useSubscription, gql } from 'urql';
import { LotDetailsPanel } from './lot-details-panel';
import type { ParkingLot as ApiParkingLot, SpotAvailability, SpotStatusEvent } from '../../types';
import type { Spot, ParkingLot as V0ParkingLot } from '../../lib/parking';
import { adaptLot } from '../../lib/api-adapter';

const AVAILABILITY_QUERY = gql`
  query GetAvailability($lotId: ID!, $from: String!, $to: String!) {
    availability(lotId: $lotId, from: $from, to: $to) {
      spotId
      code
      type
      isAvailable
      layoutX
      layoutY
      bookedUntil
      isAnomaly
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

function makeDefaultRange() {
  // Default window: now → now + 2h, rounded to the current minute
  const from = new Date();
  from.setSeconds(0, 0);
  const to = new Date(from.getTime() + 2 * 60 * 60 * 1000);
  return { fromIso: from.toISOString(), toIso: to.toISOString() };
}

export function LotDetailsPanelWrapper({
  apiLot,
  onClose,
  onSelectSpot,
  bookedSpotId,
}: {
  apiLot: ApiParkingLot;
  onClose: () => void;
  onSelectSpot: (spot: Spot, lot: V0ParkingLot) => void;
  /** Spot ID that was just successfully booked — triggers optimistic occupied state */
  bookedSpotId?: string | null;
}) {
  const [{ fromIso, toIso }, setRange] = useState(makeDefaultRange);

  // Re-fetch availability whenever time range changes.
  // requestPolicy is 'network-only' (set at client level, no cacheExchange).
  const [{ data }] = useQuery({
    query: AVAILABILITY_QUERY,
    variables: { lotId: apiLot.id, from: fromIso, to: toIso },
  });

  const [liveSpots, setLiveSpots] = useState<SpotAvailability[]>([]);
  const [flashing, setFlashing] = useState<Set<string>>(new Set());

  // Sync GraphQL result into local live state
  useEffect(() => {
    if (data?.availability) {
      setLiveSpots(data.availability);
    }
  }, [data]);

  // Optimistically mark a spot as occupied immediately after booking —
  // the real-time update from the sensor simulator will follow shortly.
  useEffect(() => {
    if (!bookedSpotId) return;
    setLiveSpots(prev =>
      prev.map(s => (s.spotId === bookedSpotId ? { ...s, isAvailable: false } : s))
    );
  }, [bookedSpotId]);

  // Real-time subscription: update individual spot availability on sensor events
  useSubscription(
    { query: SPOT_STATUS_SUBSCRIPTION, variables: { lotId: apiLot.id } },
    (_, response) => {
      if (response.spotStatusChanged) {
        const event = response.spotStatusChanged as SpotStatusEvent;

        setLiveSpots(prev =>
          prev.map(spot => {
            if (spot.spotId === event.spotId) {
              const isBooked = spot.bookedUntil && new Date(spot.bookedUntil) > new Date();
              // If it's booked, it is NEVER available for someone else. 
              // If it's not booked, availability depends on the physical sensor.
              const isAvail = !isBooked && event.status === 'FREE';
              // Anomaly: physically occupied but no active booking
              const isAnomaly = !isBooked && event.status === 'OCCUPIED';
              
              return { ...spot, isAvailable: isAvail, isAnomaly };
            }
            return spot;
          })
        );

        // Trigger the flash ring animation on the changed spot
        setFlashing(new Set([`${apiLot.id}:${event.spotId}`]));
        setTimeout(() => setFlashing(new Set()), 900);
      }
      return response;
    }
  );

  const v0Lot = adaptLot(apiLot, liveSpots);

  return (
    <LotDetailsPanel
      lot={v0Lot}
      flashing={flashing}
      fromIso={fromIso}
      toIso={toIso}
      onFromChange={(iso) => setRange(r => ({ ...r, fromIso: iso }))}
      onToChange={(iso) => setRange(r => ({ ...r, toIso: iso }))}
      onClose={onClose}
      onSelectSpot={(spot) => onSelectSpot(spot, v0Lot)}
    />
  );
}
