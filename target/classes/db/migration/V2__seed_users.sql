-- 1. Insert Test Users (Password: Password123! hashed via standard Argon2id)
INSERT INTO users (id, username, email, password, enabled, created_at)
VALUES
    (1, 'admin', 'admin@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (2, 'dean_morris', 'dean.morris@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (3, 'chair_clark', 'chair.clark@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (4, 'faculty_alice', 'faculty.alice@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (5, 'registrar_bob', 'registrar.bob@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (6, 'student_john', 'student.john@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', TRUE, NOW()),
    (7, 'inactive_user', 'disabled@example.com', '$argon2i$v=19$m=16,t=2,p=1$Q1hqRGcwNlRmSVZXTEV3dw$nUsGxR/gKXU3BUmayXcZxQ', FALSE, NOW());

-- 2. Assign Enum Roles (Stored exactly matching Roles enum names)
INSERT INTO user_roles (user_id, role)
VALUES
    (1, 'ADMIN'),
    (2, 'DEAN'),
    (2, 'FACULTY'),
    (3, 'CHAIRPERSON'),
    (3, 'FACULTY'),
    (4, 'GUIDANCE'),
    (5, 'REGISTRAR'),
    (6, 'STUDENT'),
    (7, 'FACULTY');

-- 3. Insert Sample Refresh Tokens (Active, Revoked, and Expired states)
INSERT INTO refresh_tokens (user_id, token, expiry_date, revoked, created_at)
VALUES
    -- Active Admin Session (Valid for 7 days)
    (1, 'WllGtuQqG1zVNYaJ7mcv7mf4E7d8Sjgxyn5j7JO6aiXu1vtTQm-7QCDMC9M-V89SjRpCPGURB27tMHcQ5Jfrcw', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, NOW()),

    -- Active Faculty Session
    (4, 'ZabHtuQqG1zVNYaJ7mcv7mf4E7d8Sjgxyn5j7JO6aiXu1vtTQm-7QCDMC9M-V89SjRpCPGURB27tMHcQ5Jfrcw', DATE_ADD(NOW(), INTERVAL 7 DAY), FALSE, NOW()),

    -- Revoked Token (Used for testing theft/reuse detection)
    (4, '9a3bb93f-c1f3-4f9e-991c-2c9b31d8e6a1-revoked-token-sample', DATE_ADD(NOW(), INTERVAL 7 DAY), TRUE, NOW()),

    -- Expired Token
    (6, '1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed-expired-token-sample', DATE_SUB(NOW(), INTERVAL 1 DAY), FALSE, DATE_SUB(NOW(), INTERVAL 8 DAY));

-- 4. Insert Sample Password Reset Tokens (Active and Expired states)
INSERT INTO password_reset_tokens (user_id, token, expiry_date, created_at)
VALUES
    -- Active Token for student_john (Valid for 30 minutes)
    (6, SHA2('a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d', 256), DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW()),

    -- Expired Token for faculty_alice (Expired 10 minutes ago)
    (4, SHA2('f9e8d7c6-b5a4-3f2e-1d0c-9b8a7f6e5d4c', 256), DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_SUB(NOW(), INTERVAL 40 MINUTE));