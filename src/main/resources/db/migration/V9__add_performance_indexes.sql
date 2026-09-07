-- V9__add_performance_indexes.sql
-- Adds B-tree indexes for foreign keys and common query access patterns to eliminate Seq Scans.
-- Note: Overlapping time-range checks for reservations are already handled by GiST exclusion constraint (V4).
-- The B-tree index on reservation(spot_id) specifically optimizes queries filtering purely by spot_id without time bounds.

CREATE INDEX IF NOT EXISTS idx_spot_parking_lot_id ON spot(parking_lot_id);

CREATE INDEX IF NOT EXISTS idx_reservation_user_id ON reservation(user_id);
CREATE INDEX IF NOT EXISTS idx_reservation_user_id_status ON reservation(user_id, status);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON reservation(status);
CREATE INDEX IF NOT EXISTS idx_reservation_spot_id ON reservation(spot_id);
CREATE INDEX IF NOT EXISTS idx_reservation_created_at ON reservation(created_at);

CREATE INDEX IF NOT EXISTS idx_spot_anomaly_spot_id ON spot_anomaly(spot_id);
CREATE INDEX IF NOT EXISTS idx_spot_anomaly_resolved_at ON spot_anomaly(resolved_at);

CREATE INDEX IF NOT EXISTS idx_sensor_event_spot_id ON sensor_event(spot_id);
CREATE INDEX IF NOT EXISTS idx_sensor_event_sensor_timestamp ON sensor_event(sensor_timestamp);

CREATE INDEX IF NOT EXISTS idx_reservation_audit_reservation_id ON reservation_audit(reservation_id);

CREATE INDEX IF NOT EXISTS idx_parking_lot_lat_lng ON parking_lot(latitude, longitude);
