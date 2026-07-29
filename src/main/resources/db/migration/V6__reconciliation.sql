CREATE INDEX IF NOT EXISTS idx_spot_anomaly_unresolved ON spot_anomaly (detected_at) WHERE resolved_at IS NULL;
