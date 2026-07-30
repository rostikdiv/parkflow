-- V8__add_timestamps_to_payment.sql
ALTER TABLE payment
    ADD COLUMN created_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN updated_at TIMESTAMPTZ DEFAULT NOW();
