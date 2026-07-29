CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE parking_lot (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    type VARCHAR(50) NOT NULL,
    hourly_rate NUMERIC(10, 2) NOT NULL,
    opens_at TIME,
    closes_at TIME,
    time_zone VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL
);

CREATE TABLE spot (
    id UUID PRIMARY KEY,
    parking_lot_id UUID NOT NULL REFERENCES parking_lot(id),
    code VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    physical_status VARCHAR(50) NOT NULL,
    last_sensor_update TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    layout_x DOUBLE PRECISION,
    layout_y DOUBLE PRECISION
);

CREATE TABLE reservation (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    spot_id UUID NOT NULL REFERENCES spot(id),
    license_plate VARCHAR(50) NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservation(id) UNIQUE,
    amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    external_ref VARCHAR(255),
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE TABLE sensor_event (
    id UUID PRIMARY KEY,
    external_event_id VARCHAR(255) NOT NULL UNIQUE,
    spot_id UUID NOT NULL REFERENCES spot(id),
    raw_payload JSONB NOT NULL,
    status VARCHAR(50) NOT NULL,
    sensor_timestamp TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE TABLE spot_anomaly (
    id UUID PRIMARY KEY,
    spot_id UUID NOT NULL REFERENCES spot(id),
    type VARCHAR(50) NOT NULL,
    details TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE TABLE reservation_audit (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES reservation(id),
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
