import { useState, useMemo } from 'react';
import { useQuery, gql } from 'urql';
import { MapOverview } from './components/MapOverview';
import { AppLayout } from './components/v0/app-layout';
import { LotDetailsPanelWrapper } from './components/v0/lot-details-wrapper';
import { BookingDialog } from './components/v0/booking-dialog';
import type { ParkingLot as ApiParkingLot } from './types';
import type { Spot, ParkingLot as V0ParkingLot } from './lib/parking';
import { SnackbarProvider, useSnackbar } from './components/v0/snackbar';

const PARKING_LOTS_QUERY = gql`
  query {
    parkingLots {
      id
      name
      address
      latitude
      longitude
      type
      hourlyRate
      status
    }
  }
`;

function AppContent() {
  const [selectedLot, setSelectedLot] = useState<ApiParkingLot | null>(null);
  const [query, setQuery] = useState('');
  
  const [bookingSpot, setBookingSpot] = useState<Spot | null>(null);
  const [bookingLot, setBookingLot] = useState<V0ParkingLot | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const { notify } = useSnackbar();

  const [{ data, fetching, error }] = useQuery({
    query: PARKING_LOTS_QUERY,
  });

  const lots = data?.parkingLots || [];
  
  const filteredLots = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return lots;
    return lots.filter(
      (lot: ApiParkingLot) =>
        lot.name.toLowerCase().includes(q) || lot.address.toLowerCase().includes(q)
    );
  }, [lots, query]);

  function handleSelectSpot(spot: Spot, lot: V0ParkingLot) {
    setBookingSpot(spot);
    setBookingLot(lot);
    setDialogOpen(true);
  }

  async function handleConfirm({ plate, hours }: { plate: string; hours: number }) {
    if (!bookingLot || !bookingSpot) return;
    try {
      const from = new Date();
      const to = new Date(from.getTime() + hours * 60 * 60 * 1000);
      
      const res = await fetch('/api/v1/reservations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID(),
          'X-User-Id': '00000000-0000-0000-0000-000000000001',
        },
        body: JSON.stringify({
          spotId: bookingSpot.id,
          from: from.toISOString(),
          to: to.toISOString(),
          licensePlate: plate.toUpperCase(),
        }),
      });

      if (!res.ok) {
        throw new Error('Failed to create reservation');
      }

      setDialogOpen(false);
      notify(`Spot ${bookingSpot.label} booked for ${hours} ${hours === 1 ? 'hour' : 'hours'} · ${plate}`, 'success');
    } catch {
      notify('Something went wrong. Please try again.', 'error');
    }
  }

  return (
    <>
      <AppLayout query={query} onQueryChange={setQuery}>
        {fetching && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/50 z-50">
            <div className="animate-pulse text-primary font-semibold text-lg">Loading city map...</div>
          </div>
        )}
        
        {error && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/50 z-50">
            <div className="bg-destructive/10 text-destructive p-4 rounded-xl border border-destructive/20">
              {error.message}
            </div>
          </div>
        )}

        <MapOverview 
          lots={filteredLots} 
          selectedLot={selectedLot}
          onSelectLot={setSelectedLot} 
        />
      </AppLayout>

      {/* Detail Sidebar / Bottom Sheet */}
      {selectedLot && (
        <LotDetailsPanelWrapper 
          apiLot={selectedLot} 
          onClose={() => setSelectedLot(null)}
          onSelectSpot={handleSelectSpot}
        />
      )}

      {bookingLot && bookingSpot && (
        <BookingDialog
          open={dialogOpen}
          lot={bookingLot}
          spot={bookingSpot}
          onOpenChange={setDialogOpen}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
}

function App() {
  return (
    <SnackbarProvider>
      <AppContent />
    </SnackbarProvider>
  );
}

export default App;
