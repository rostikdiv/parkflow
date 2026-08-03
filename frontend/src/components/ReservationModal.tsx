import { useState } from 'react';
import { X, Clock, Car, ShieldCheck, Loader2 } from 'lucide-react';
import type { SpotAvailability, ParkingLot } from '../types';

interface ReservationModalProps {
  spot: SpotAvailability;
  lot: ParkingLot;
  onClose: () => void;
  onSuccess: () => void;
}

export function ReservationModal({ spot, lot, onClose, onSuccess }: ReservationModalProps) {
  const [licensePlate, setLicensePlate] = useState('');
  const [durationHours, setDurationHours] = useState(2);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError(null);

    // Calculate times
    const from = new Date();
    const to = new Date(from.getTime() + durationHours * 60 * 60 * 1000);

    // Using a hardcoded demo user ID that actually exists in DB
    const demoUserId = '00000000-0000-0000-0000-000000000001';
    
    try {
      const res = await fetch('/api/v1/reservations', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Idempotency-Key': crypto.randomUUID(),
          'X-User-Id': demoUserId, // In a real app this would be a JWT Authorization header
        },
        body: JSON.stringify({
          spotId: spot.spotId,
          from: from.toISOString(),
          to: to.toISOString(),
          licensePlate: licensePlate.toUpperCase(),
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.detail || data.message || 'Failed to create reservation');
      }

      onSuccess();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    // z-[2000] ensures modal is above the sidebar (z-[1000]) on all viewports
    <div className="fixed inset-0 z-[2000] flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-sm p-0 sm:p-4">
      <div className="bg-gray-900 rounded-t-2xl sm:rounded-2xl w-full max-w-md border border-gray-700 shadow-2xl overflow-hidden transition-all duration-300 max-h-[90dvh] overflow-y-auto">
        
        {/* Header */}
        <div className="relative bg-gradient-to-r from-emerald-600 to-teal-600 p-6 text-white">
          <button onClick={onClose} className="absolute top-4 right-4 text-white/70 hover:text-white transition-colors">
            <X size={24} />
          </button>
          <h2 className="text-2xl font-bold mb-1">Book Spot {spot.code}</h2>
          <p className="text-emerald-100/80 text-sm flex items-center gap-1">
            <ShieldCheck size={16} /> Secure checkout for {lot.name}
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          {error && (
            <div className="bg-rose-500/10 border border-rose-500/30 text-rose-400 p-3 rounded-lg text-sm">
              {error}
            </div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5 flex items-center gap-1.5">
                <Car size={16} className="text-gray-500" /> License Plate
              </label>
              <input 
                type="text"
                required
                pattern="^[A-Za-zА-Яа-яІіЇїЄєҐґ0-9 -]{3,8}$"
                placeholder="AA1234BC"
                value={licensePlate}
                onChange={e => setLicensePlate(e.target.value)}
                className="w-full bg-gray-800 border border-gray-700 rounded-lg px-4 py-2.5 text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/50 focus:border-emerald-500 transition-all uppercase"
              />
              <p className="text-xs text-gray-500 mt-1.5">Format: 2 letters, 4 digits, 2 letters (e.g. AA1234BC)</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-400 mb-1.5 flex items-center gap-1.5">
                <Clock size={16} className="text-gray-500" /> Duration (Hours)
              </label>
              <div className="flex items-center gap-4">
                <input 
                  type="range"
                  min="1" max="12"
                  value={durationHours}
                  onChange={e => setDurationHours(Number(e.target.value))}
                  className="w-full accent-emerald-500 h-2 bg-gray-800 rounded-lg appearance-none cursor-pointer"
                />
                <span className="text-white font-medium w-12 text-right">{durationHours}h</span>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-gray-800">
            <button
              type="submit"
              disabled={isSubmitting || !licensePlate}
              className="w-full bg-emerald-500 hover:bg-emerald-400 disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed text-white font-semibold py-3 px-4 rounded-xl transition-all flex items-center justify-center gap-2 shadow-lg shadow-emerald-500/20 hover:shadow-emerald-500/40"
            >
              {isSubmitting ? <Loader2 className="animate-spin" size={20} /> : 'Confirm Booking'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
