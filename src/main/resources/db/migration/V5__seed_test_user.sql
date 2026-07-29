-- V5__seed_test_user.sql
-- Додаємо тестового користувача для потреб M2, оскільки повноцінний модуль Security (M8) ще не реалізовано.
-- Цей UUID використовується як захардкоджений у ReservationController.
INSERT INTO app_user (id, email, password_hash, full_name, phone, role)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'test.user@example.com',
    'dummy_hash',
    'Test User',
    '+380991234567',
    'USER'
) ON CONFLICT (id) DO NOTHING;
