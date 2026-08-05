import { useEffect, useRef, useState, useMemo } from 'react';
import { MapContainer, TileLayer, Marker, useMap, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { renderToString } from 'react-dom/server';
import { useQuery, useSubscription, gql } from 'urql';
import type { ParkingLot as ApiParkingLot, SpotAvailability, SpotStatusEvent } from '../types';
import { LotPin } from './v0/lot-pin';

const AVAILABILITY_QUERY = gql`
  query GetMapAvailability($lotId: ID!, $from: String!, $to: String!) {
    availability(lotId: $lotId, from: $from, to: $to) {
      spotId
      isAvailable
    }
  }
`;

const SPOT_STATUS_SUBSCRIPTION = gql`
  subscription OnMapSpotStatusChanged($lotId: ID!) {
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

function createCustomIcon(lot: ApiParkingLot, active: boolean, total: number, free: number) {
  const mockV0Lot = {
    id: lot.id,
    name: lot.name,
    address: lot.address,
    distanceKm: 0,
    hourlyRate: lot.hourlyRate || 0,
    x: 0,
    y: 0,
    rows: [
      {
        id: 'A',
        letter: 'A',
        spots: Array(Math.max(total, 1)).fill(null).map((_, i) => ({
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
      <LotPin lot={mockV0Lot} active={active} onSelect={() => {}} />
    </div>
  );

  return L.divIcon({
    html,
    className: 'custom-leaflet-pin',
    iconSize: [120, 48],
    iconAnchor: [60, 48],
  });
}

function LotMarker({
  lot,
  selectedLot,
  onSelectLot,
}: {
  lot: ApiParkingLot;
  selectedLot: ApiParkingLot | null;
  onSelectLot: (l: ApiParkingLot) => void;
}) {
  const markerRef = useRef<L.Marker>(null);

  const [{ from, to }] = useState(() => {
    const f = new Date();
    f.setSeconds(0, 0);
    const t = new Date(f.getTime() + 2 * 60 * 60 * 1000);
    return { from: f.toISOString(), to: t.toISOString() };
  });

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
        setLiveSpots(prev =>
          prev.map(spot =>
            spot.spotId === event.spotId ? { ...spot, isAvailable: isAvail } : spot
          )
        );
      }
      return response;
    }
  );

  const total = liveSpots.length;
  const free  = liveSpots.filter(s => s.isAvailable).length;
  const isActive = selectedLot?.id === lot.id;

  const icon = useMemo(() => {
    return createCustomIcon(lot, isActive, Math.max(total, 1), free);
  }, [lot, isActive, total, free]);

  return (
    <Marker
      ref={markerRef}
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

  // Only show ACTIVE parking lots on the map (filter out CLOSED ones)
  const openLots = lots.filter(lot => lot.status === 'ACTIVE');

  return (
    <div className="w-full h-full z-0 bg-background">
      <MapContainer
        center={center}
        zoom={13}
        className="w-full h-full"
        zoomControl={false}
      >
        <ZoomControl position="bottomright" />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
          subdomains="abcd"
          maxZoom={19}
        />
        {openLots.map((lot) => (
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
