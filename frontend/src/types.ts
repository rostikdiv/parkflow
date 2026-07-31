export interface ParkingLot {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
  type: string;
  hourlyRate: number;
  status: string;
}

export interface SpotAvailability {
  spotId: string;
  code: string;
  type: string;
  isAvailable: boolean;
  layoutX: number;
  layoutY: number;
  bookedUntil?: string;
  isAnomaly: boolean;
}

export interface SpotStatusEvent {
  spotId: string;
  status: string;
  at: string;
}

export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ReservationResponse {
  id: string;
  spotId: string;
  spotCode: string;
  lotId: string;
  lotName: string;
  licensePlate: string;
  startTime: string;
  endTime: string;
  status: string;
  totalPrice: number;
  createdAt: string;
}

export interface AdminReservationResponse extends ReservationResponse {
  userId: string;
  userEmail: string;
  userFullName: string;
}

export interface SpotAnomalyResponse {
  id: string;
  spotId: string;
  spotCode: string;
  lotId: string;
  lotName: string;
  type: string;
  details: string;
  detectedAt: string;
  resolvedAt: string | null;
}
