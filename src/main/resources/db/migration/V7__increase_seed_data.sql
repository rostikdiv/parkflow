DO $$
DECLARE
    mega_lot_id UUID := '20000000-0000-0000-0000-000000000001';
    airport_lot_id UUID := '20000000-0000-0000-0000-000000000002';
    i INTEGER;
    spot_id UUID;
    spot_type VARCHAR;
BEGIN
    -- Seed 2 New Large Parking Lots
    INSERT INTO parking_lot (id, name, address, latitude, longitude, type, hourly_rate, opens_at, closes_at, time_zone, status)
    VALUES
    (mega_lot_id, 'Gulliver Mega Parking', 'Sportyvna Square, 1, Kyiv', 50.4390, 30.5230, 'UNDERGROUND', 100.00, '00:00:00', '23:59:59', 'Europe/Kyiv', 'ACTIVE'),
    (airport_lot_id, 'Boryspil Airport P1', 'Boryspil, Kyiv Region', 50.3450, 30.8950, 'OPEN_AIR', 20.00, '00:00:00', '23:59:59', 'Europe/Kyiv', 'ACTIVE');

    -- Seed 500 spots for Mega Parking
    FOR i IN 1..500 LOOP
        spot_id := gen_random_uuid();
        -- Randomize spot type
        IF i % 10 = 0 THEN
            spot_type := 'DISABLED';
        ELSIF i % 15 = 0 THEN
            spot_type := 'EV_CHARGING';
        ELSIF i % 20 = 0 THEN
            spot_type := 'COMPACT';
        ELSE
            spot_type := 'STANDARD';
        END IF;

        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, mega_lot_id, 'M-' || i, spot_type, 'UNKNOWN', 0, (i % 20) * 1.5, floor(i / 20) * 2.0);
    END LOOP;

    -- Seed 500 spots for Airport Parking
    FOR i IN 1..500 LOOP
        spot_id := gen_random_uuid();
        -- Randomize spot type
        IF i % 20 = 0 THEN
            spot_type := 'DISABLED';
        ELSE
            spot_type := 'STANDARD';
        END IF;

        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, airport_lot_id, 'P1-' || i, spot_type, 'UNKNOWN', 0, (i % 50) * 1.5, floor(i / 50) * 2.5);
    END LOOP;
END $$;
