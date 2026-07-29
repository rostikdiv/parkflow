-- V4__add_reservation_exclusion_constraint.sql
-- See plan §4.3, level 3: EXCLUDE USING gist provides atomic rejection of overlapping time intervals
-- at the DB level, which is much better for time-range bookings than @Version optimistic locking.
ALTER TABLE reservation
ADD CONSTRAINT no_overlapping_reservations
EXCLUDE USING gist (
    spot_id WITH =,
    tstzrange(start_time, end_time) WITH &&
) WHERE (status IN ('PENDING', 'CONFIRMED', 'ACTIVE'));
