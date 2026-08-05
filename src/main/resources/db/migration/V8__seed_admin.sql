-- Seed an ADMIN user to allow testing of the AdminAnomalyController and other secured admin endpoints
INSERT INTO app_user (id, email, password_hash, full_name, phone, role)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'admin@parkflow.com',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', -- hash for 'password'
    'System Admin',
    '+380000000000',
    'ADMIN'
);
