DO $$
DECLARE
    lot1_id UUID := '10000000-0000-0000-0000-000000000001';
    lot2_id UUID := '10000000-0000-0000-0000-000000000002';
    lot3_id UUID := '10000000-0000-0000-0000-000000000003';
    i INTEGER;
    spot_id UUID;
BEGIN
    -- Seed 3 Parking Lots in Kyiv
    INSERT INTO parking_lot (id, name, address, latitude, longitude, type, hourly_rate, opens_at, closes_at, time_zone, status)
    VALUES
    (lot1_id, 'Khreshchatyk Open Air', 'Khreshchatyk St, 24, Kyiv', 50.4475, 30.5225, 'OPEN_AIR', 50.00, '08:00:00', '22:00:00', 'Europe/Kyiv', 'ACTIVE'),
    (lot2_id, 'Gulliver Underground', 'Sportyvna Square, 1a, Kyiv', 50.4389, 30.5229, 'UNDERGROUND', 80.00, '00:00:00', '23:59:59', 'Europe/Kyiv', 'ACTIVE'),
    (lot3_id, 'Ocean Plaza Indoor', 'Antonovycha St, 176, Kyiv', 50.4124, 30.5226, 'INDOOR', 60.00, '10:00:00', '22:00:00', 'Europe/Kyiv', 'ACTIVE');

    -- Seed 40 spots for each lot
    FOR i IN 1..40 LOOP
        -- Mix some spot types
        IF i = 1 THEN
            spot_id := '00000000-0000-0000-0000-000000000001';
        ELSIF i = 2 THEN
            spot_id := '00000000-0000-0000-0000-000000000002';
        ELSE
            spot_id := gen_random_uuid();
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, lot1_id, 'A-' || i, CASE WHEN i <= 5 THEN 'DISABLED' ELSE 'STANDARD' END, 'UNKNOWN', 0, i * 1.5, 0.0);

        IF i = 1 THEN
            spot_id := '00000000-0000-0000-0000-000000000011';
        ELSE
            spot_id := gen_random_uuid();
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, lot2_id, 'B-' || i, CASE WHEN i <= 10 THEN 'COMPACT' WHEN i >= 35 THEN 'EV_CHARGING' ELSE 'STANDARD' END, 'UNKNOWN', 0, i * 1.2, 0.0);

        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (gen_random_uuid(), lot3_id, 'C-' || i, 'STANDARD', 'UNKNOWN', 0, i * 1.5, 0.0);
    END LOOP;
END $$;
