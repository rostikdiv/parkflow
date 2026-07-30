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
}

export interface SpotStatusEvent {
  spotId: string;
  status: string;
  at: string;
}
