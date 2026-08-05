DO $$
DECLARE
    lot1_id UUID := '10000000-0000-0000-0000-000000000001';
    lot2_id UUID := '10000000-0000-0000-0000-000000000002';
    lot3_id UUID := '10000000-0000-0000-0000-000000000003';
    lot4_id UUID := '10000000-0000-0000-0000-000000000004';
    lot5_id UUID := '10000000-0000-0000-0000-000000000005';
    i INTEGER;
    spot_id UUID;
    spot_type VARCHAR;
    zone_code VARCHAR;
BEGIN
    -- Seed 5 Realistic Parking Lots
    INSERT INTO parking_lot (id, name, address, latitude, longitude, type, hourly_rate, opens_at, closes_at, time_zone, status)
    VALUES
    (lot1_id, 'Central Plaza Underground', 'Khreshchatyk St, 24, Kyiv', 50.4475, 30.5225, 'UNDERGROUND', 80.00, '00:00:00', '23:59:59', 'Europe/Kyiv', 'ACTIVE'),
    (lot2_id, 'Mall of Kyiv Open Air', 'Sportyvna Square, 1a, Kyiv', 50.4389, 30.5229, 'OPEN_AIR', 40.00, '08:00:00', '23:00:00', 'Europe/Kyiv', 'ACTIVE'),
    (lot3_id, 'Airport Express Terminal 1', 'Boryspil Airport, Kyiv Region', 50.3450, 30.8950, 'INDOOR', 60.00, '00:00:00', '23:59:59', 'Europe/Kyiv', 'ACTIVE'),
    (lot4_id, 'Riverside Business Center', 'Naberezhno-Khreshchatytska St, Kyiv', 50.4633, 30.5194, 'UNDERGROUND', 100.00, '07:00:00', '22:00:00', 'Europe/Kyiv', 'ACTIVE'),
    (lot5_id, 'Old Town Street Parking', 'Volodymyrska St, 15, Kyiv', 50.4536, 30.5164, 'OPEN_AIR', 50.00, '08:00:00', '22:00:00', 'Europe/Kyiv', 'ACTIVE');

    -- 1. Central Plaza Underground (120 місць): Зони A(40), B(40), C(40)
    FOR i IN 1..120 LOOP
        spot_id := gen_random_uuid();
        -- Hardcode the very first spot ID to what tests expect (for M2 backwards compatibility)
        IF i = 1 THEN
            spot_id := '00000000-0000-0000-0000-000000000001';
        ELSIF i = 2 THEN
            spot_id := '00000000-0000-0000-0000-000000000002';
        END IF;

        IF i <= 40 THEN
            zone_code := 'A-' || i;
            IF i <= 10 THEN spot_type := 'EV_CHARGING';
            ELSIF i <= 20 THEN spot_type := 'DISABLED';
            ELSE spot_type := 'STANDARD'; END IF;
        ELSIF i <= 80 THEN
            zone_code := 'B-' || (i - 40);
            spot_type := 'STANDARD';
        ELSE
            zone_code := 'C-' || (i - 80);
            spot_type := 'COMPACT';
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, lot1_id, zone_code, spot_type, 'UNKNOWN', 0, (i % 20) * 1.5, floor((i - 1) / 20) * 2.0);
    END LOOP;

    -- 2. Mall of Kyiv Open Air (200 місць): Зони A(100), B(100)
    FOR i IN 1..200 LOOP
        spot_id := gen_random_uuid();
        IF i = 1 THEN
            spot_id := '00000000-0000-0000-0000-000000000011';
        END IF;

        IF i <= 100 THEN
            zone_code := 'A-' || i;
            IF i <= 10 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        ELSE
            zone_code := 'B-' || (i - 100);
            IF i <= 110 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (spot_id, lot2_id, zone_code, spot_type, 'UNKNOWN', 0, (i % 25) * 1.5, floor((i - 1) / 25) * 2.5);
    END LOOP;

    -- 3. Airport Express Terminal 1 (150 місць): Зони A(50), B(100)
    FOR i IN 1..150 LOOP
        IF i <= 50 THEN
            zone_code := 'A-' || i;
            IF i <= 5 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        ELSE
            zone_code := 'B-' || (i - 50);
            IF i <= 55 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (gen_random_uuid(), lot3_id, zone_code, spot_type, 'UNKNOWN', 0, (i % 20) * 1.5, floor((i - 1) / 20) * 2.0);
    END LOOP;

    -- 4. Riverside Business Center (80 місць): Зони A(40), B(40)
    FOR i IN 1..80 LOOP
        IF i <= 40 THEN
            zone_code := 'A-' || i;
            IF i <= 15 THEN spot_type := 'EV_CHARGING'; 
            ELSIF i <= 20 THEN spot_type := 'DISABLED'; 
            ELSE spot_type := 'STANDARD'; END IF;
        ELSE
            zone_code := 'B-' || (i - 40);
            spot_type := 'COMPACT';
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (gen_random_uuid(), lot4_id, zone_code, spot_type, 'UNKNOWN', 0, (i % 10) * 1.5, floor((i - 1) / 10) * 2.0);
    END LOOP;

    -- 5. Old Town Street Parking (50 місць): Зони A(25), B(25)
    FOR i IN 1..50 LOOP
        IF i <= 25 THEN
            zone_code := 'A-' || i;
            IF i = 1 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        ELSE
            zone_code := 'B-' || (i - 25);
            IF i = 26 THEN spot_type := 'DISABLED'; ELSE spot_type := 'STANDARD'; END IF;
        END IF;
        INSERT INTO spot (id, parking_lot_id, code, type, physical_status, version, layout_x, layout_y)
        VALUES (gen_random_uuid(), lot5_id, zone_code, spot_type, 'UNKNOWN', 0, (i % 25) * 2.5, floor((i - 1) / 25) * 1.5);
    END LOOP;
END $$;
