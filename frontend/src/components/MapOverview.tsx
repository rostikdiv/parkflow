import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { renderToString } from 'react-dom/server';
import { useQuery, useSubscription, gql } from 'urql';
import type { ParkingLot as ApiParkingLot, SpotAvailability, SpotStatusEvent } from '../types';
import { LotPin } from './v0/lot-pin';

const AVAILABILITY_QUERY = gql`
  query GetAvailability($lotId: ID!, $from: String!, $to: String!) {
    availability(lotId: $lotId, from: $from, to: $to) {
      spotId
      isAvailable
    }
  }
`;

const SPOT_STATUS_SUBSCRIPTION = gql`
  subscription OnSpotStatusChanged($lotId: ID!) {
    spotStatusChanged(lotId: $lotId) {
      spotId
      status
    }
  }
`;

function ChangeView({ center, zoom }: { center: [number, number]; zoom: number }) {
  const map = useMap();
  useEffect(() => {
    map.setView(center, zoom);
  }, [center, zoom, map]);
  return null;
}

interface MapOverviewProps {
  lots: ApiParkingLot[];
  selectedLot: ApiParkingLot | null;
  onSelectLot: (lot: ApiParkingLot) => void;
}

// A helper to generate a custom icon using the v0 LotPin component
function createCustomIcon(lot: ApiParkingLot, active: boolean, total: number, free: number) {
  // We mock a v0 ParkingLot just for the LotPin to render correctly.
  const mockV0Lot = {
    id: lot.id,
    name: lot.name,
    address: lot.address,
    distanceKm: 1.0,
    hourlyRate: lot.hourlyRate || 0,
    x: 0,
    y: 0,
    // Provide an empty array for rows, but mock the total/free via lotStats by overriding it?
    // Wait, lotStats in v0 lot-pin computes total/free from rows! 
    // We must pass spots to it, or we can just mock rows with the correct length and status.
    rows: [
      {
        id: 'A',
        letter: 'A',
        spots: Array(total).fill(null).map((_, i) => ({
          id: `A${i}`,
          label: `A${i}`,
          status: i < free ? 'available' : 'occupied',
          kind: 'standard',
          col: i
        } as any))
      }
    ]
  };

  const html = renderToString(
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      {/* We strip the absolute positioning from LotPin for Leaflet */}
      <LotPin lot={mockV0Lot} active={active} onSelect={() => {}} />
    </div>
  );

  return L.divIcon({
    html: html,
    className: 'custom-leaflet-pin', // transparent background
    iconSize: [100, 40],
    iconAnchor: [50, 40], // anchor at the bottom center of the pin
  });
}

function LotMarker({ lot, selectedLot, onSelectLot }: { lot: ApiParkingLot, selectedLot: ApiParkingLot | null, onSelectLot: (l: ApiParkingLot) => void }) {
  const [{ from, to }] = useState(() => ({
    from: new Date().toISOString(),
    to: new Date(new Date().getTime() + 2 * 60 * 60 * 1000).toISOString()
  }));

  const [{ data }] = useQuery({
    query: AVAILABILITY_QUERY,
    variables: { lotId: lot.id, from, to },
  });

  const [liveSpots, setLiveSpots] = useState<SpotAvailability[]>([]);

  useEffect(() => {
    if (data?.availability) {
      setLiveSpots(data.availability);
    }
  }, [data]);

  useSubscription(
    { query: SPOT_STATUS_SUBSCRIPTION, variables: { lotId: lot.id } },
    (_, response) => {
      if (response.spotStatusChanged) {
        const event = response.spotStatusChanged as SpotStatusEvent;
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

  const total = liveSpots.length;
  const free = liveSpots.filter(s => s.isAvailable).length;
  const isActive = selectedLot?.id === lot.id;

  // Render a fallback icon while loading
  const icon = total > 0 ? createCustomIcon(lot, isActive, total, free) : createCustomIcon(lot, isActive, 1, 1);

  return (
    <Marker
      position={[lot.latitude, lot.longitude]}
      icon={icon}
      eventHandlers={{
        click: () => onSelectLot(lot),
      }}
    />
  );
}

export function MapOverview({ lots, selectedLot, onSelectLot }: MapOverviewProps) {
  const center: [number, number] = [50.4501, 30.5234]; // Kyiv

  return (
    <div className="w-full h-full z-0 bg-background">
      <MapContainer
        center={center}
        zoom={13}
        className="w-full h-full"
        zoomControl={false}
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          subdomains="abcd"
          maxZoom={19}
        />
        {lots.map((lot) => (
          <LotMarker
            key={lot.id}
            lot={lot}
            selectedLot={selectedLot}
            onSelectLot={onSelectLot}
          />
        ))}
        {selectedLot && (
          <ChangeView center={[selectedLot.latitude, selectedLot.longitude]} zoom={16} />
        )}
      </MapContainer>
    </div>
  );
}
