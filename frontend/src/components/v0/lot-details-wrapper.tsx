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

export function LotDetailsPanelWrapper({
  apiLot,
  onClose,
  onSelectSpot,
}: {
  apiLot: ApiParkingLot;
  onClose: () => void;
  onSelectSpot: (spot: Spot, lot: V0ParkingLot) => void;
}) {
  const [{ from, to }] = useState(() => ({
    from: new Date().toISOString(),
    to: new Date(new Date().getTime() + 2 * 60 * 60 * 1000).toISOString()
  }));

  const [{ data }] = useQuery({
    query: AVAILABILITY_QUERY,
    variables: { lotId: apiLot.id, from, to },
  });

  const [liveSpots, setLiveSpots] = useState<SpotAvailability[]>([]);
  const [flashing, setFlashing] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (data?.availability) {
      setLiveSpots(data.availability);
    }
  }, [data]);

  useSubscription(
    { query: SPOT_STATUS_SUBSCRIPTION, variables: { lotId: apiLot.id } },
    (_, response) => {
      if (response.spotStatusChanged) {
        const event = response.spotStatusChanged as SpotStatusEvent;
        const isAvail = event.status === 'FREE';
        
        setLiveSpots(prev => prev.map(spot => 
          spot.spotId === event.spotId 
            ? { ...spot, isAvailable: isAvail } 
            : spot
        ));
        
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
      onClose={onClose}
      onSelectSpot={(spot) => onSelectSpot(spot, v0Lot)}
    />
  );
}
