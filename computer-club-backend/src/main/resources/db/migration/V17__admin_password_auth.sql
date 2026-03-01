-- staff accounts may not have a phone number
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;

-- seed initial GLOBAL_ADMIN (password: Admin1234! — change after first login)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (phone, username, password_hash, is_active, global_role, created_at, updated_at)
SELECT NULL, 'admin', crypt('Admin1234!', gen_salt('bf', 10)), true, 'GLOBAL_ADMIN', now(), now()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE global_role = 'GLOBAL_ADMIN');
