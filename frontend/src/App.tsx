import { useState, useMemo, useCallback, useEffect } from 'react';
import { useQuery, gql } from 'urql';
import { MapOverview } from './components/MapOverview';
import { AppLayout } from './components/v0/app-layout';
import { LotDetailsPanelWrapper } from './components/v0/lot-details-wrapper';
import { BookingDialog } from './components/v0/booking-dialog';
import { MyReservationsPanel } from './components/v0/my-reservations-panel';
import { ProfilePanel } from './components/v0/profile-panel';
import { AdminPanel } from './components/v0/admin-panel';
import type { ParkingLot as ApiParkingLot } from './types';
import type { Spot, ParkingLot as V0ParkingLot } from './lib/parking';
import { SnackbarProvider, useSnackbar } from './components/v0/snackbar';
import { AuthProvider, useAuth } from './lib/auth';
import { Auth } from './components/Auth';

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

function ColdStartOverlay() {
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState(false);
  const [showOverlay, setShowOverlay] = useState(false);

  // Always use relative path. 
  // In dev, Vite Proxy forwards it. In prod, Vercel Rewrites forwards it.
  const healthUrl = '/actuator/health';

  useEffect(() => {
    // Only show the overlay if the server takes longer than 1.5s to respond
    const showTimeoutId = setTimeout(() => setShowOverlay(true), 1500);

    let pingTimeoutId: ReturnType<typeof setTimeout>;
    
    const pingServer = async () => {
      try {
        const [resHealth, resEmulator] = await Promise.all([
          fetch(healthUrl),
          fetch('/api/v1/emulator/status')
        ]);
        
        if (resHealth.ok && resEmulator.ok) {
          setIsReady(true);
          clearTimeout(showTimeoutId);
          return;
        }
      } catch (e) {
        // Ignore and retry
      }
      
      // Retry after 2 seconds
      pingTimeoutId = setTimeout(pingServer, 2000);
    };

    pingServer();
    
    // Fallback: if server doesn't wake up in 15 seconds, show error/continue anyway
    const fallbackTimeout = setTimeout(() => {
      if (!isReady) {
        setError(true);
        setTimeout(() => setIsReady(true), 3000); // Let them in after showing error
      }
    }, 15000);

    return () => {
      clearTimeout(showTimeoutId);
      clearTimeout(pingTimeoutId);
      clearTimeout(fallbackTimeout);
    };
  }, [healthUrl]);

  if (isReady || !showOverlay) return null;

  return (
    <div className="fixed inset-0 z-[9999] flex flex-col items-center justify-center bg-background text-foreground transition-opacity duration-1000">
      <div className="relative flex flex-col items-center">
        {/* Pulsing ring */}
        <div className="absolute inset-0 rounded-full bg-primary/20 animate-ping"></div>
        {/* Logo or Icon */}
        <div className="relative z-10 w-24 h-24 mb-6 rounded-full bg-primary flex items-center justify-center shadow-lg shadow-primary/30">
          <svg className="w-12 h-12 text-primary-foreground" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
          </svg>
        </div>
        
        <h2 className="text-2xl font-bold mb-2 tracking-tight">Waking up the cloud</h2>
        <p className="text-muted-foreground animate-pulse">
          {error ? "Taking longer than expected..." : "Please wait a moment. The server is warming up."}
        </p>
      </div>
    </div>
  );
}

function AppContent() {
  const { isAuthenticated, token } = useAuth();
  const [selectedLot, setSelectedLot] = useState<ApiParkingLot | null>(null);
  const [query, setQuery] = useState('');

  const [bookingSpot, setBookingSpot] = useState<Spot | null>(null);
  const [bookingLot, setBookingLot] = useState<V0ParkingLot | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);

  // Track the most recently booked spotId for optimistic UI update in LotDetailsPanelWrapper
  const [lastBookedSpotId, setLastBookedSpotId] = useState<string | null>(null);

  // Panels visibility
  const [showReservations, setShowReservations] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [showAdmin, setShowAdmin] = useState(false);

  const { notify } = useSnackbar();

  const [{ data, fetching, error }] = useQuery({
    query: PARKING_LOTS_QUERY,
    pause: !isAuthenticated,
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
      // Start 1 minute in the future to satisfy @FutureOrPresent validation
      const from = new Date(Date.now() + 60 * 1000);
      const to   = new Date(from.getTime() + hours * 60 * 60 * 1000);

      const res = await fetch('/api/v1/reservations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID(),
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          spotId: bookingSpot.id,
          from: from.toISOString(),
          to: to.toISOString(),
          licensePlate: plate.toUpperCase(),
        }),
      });

      if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.detail || body.title || 'Failed to create reservation');
      }

      setDialogOpen(false);
      // Optimistically mark the spot as occupied in the details panel
      setLastBookedSpotId(bookingSpot.id);
      notify(`Spot ${bookingSpot.label} booked for ${hours} ${hours === 1 ? 'hour' : 'hours'} · ${plate}`, 'success');
    } catch (err: any) {
      notify(err.message || 'Something went wrong. Please try again.', 'error');
    }
  }

  const handleCancelReservation = useCallback(async (_id: string) => {
    notify('Reservation cancelled.', 'info');
  }, [notify]);

  if (!isAuthenticated) {
    return <Auth />;
  }

  return (
    <>
      <AppLayout
        query={query}
        onQueryChange={setQuery}
        onOpenReservations={() => setShowReservations(true)}
        onOpenProfile={() => setShowProfile(true)}
        onOpenAdmin={() => setShowAdmin(true)}
      >
        {fetching && (
          <div className="absolute inset-0 flex items-center justify-center bg-background/50 z-50">
            <div className="animate-pulse text-primary font-semibold text-lg">Loading city map…</div>
          </div>
        )}

        {error && (
          <div className="absolute top-4 left-1/2 -translate-x-1/2 z-[3000] flex items-center gap-2 bg-yellow-500/20 border border-yellow-500/50 text-yellow-600 dark:text-yellow-500 px-4 py-2 rounded-full shadow-lg backdrop-blur-md">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-yellow-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-yellow-500"></span>
            </span>
            <span className="text-sm font-medium">Дані можуть бути застарілими</span>
          </div>
        )}

        <MapOverview
          lots={filteredLots}
          selectedLot={selectedLot}
          onSelectLot={setSelectedLot}
        />
      </AppLayout>

      {/* Lot detail sidebar / bottom sheet */}
      {selectedLot && (
        <LotDetailsPanelWrapper
          apiLot={selectedLot}
          onClose={() => setSelectedLot(null)}
          onSelectSpot={handleSelectSpot}
          bookedSpotId={lastBookedSpotId}
        />
      )}

      {/* Booking confirmation dialog */}
      {bookingLot && bookingSpot && (
        <BookingDialog
          open={dialogOpen}
          lot={bookingLot}
          spot={bookingSpot}
          onOpenChange={setDialogOpen}
          onConfirm={handleConfirm}
        />
      )}

      {/* My Reservations panel */}
      {showReservations && (
        <MyReservationsPanel
          onClose={() => setShowReservations(false)}
          onCancelReservation={handleCancelReservation}
        />
      )}

      {/* User Profile panel */}
      {showProfile && (
        <ProfilePanel onClose={() => setShowProfile(false)} />
      )}

      {/* Admin Panel */}
      {showAdmin && (
        <AdminPanel onClose={() => setShowAdmin(false)} />
      )}
    </>
  );
}

function App() {
  return (
    <AuthProvider>
      <SnackbarProvider>
        <ColdStartOverlay />
        <AppContent />
      </SnackbarProvider>
    </AuthProvider>
  );
}

export default App;
