-- clear pgcrypto hash so AdminDataSeeder sets a Spring BCrypt-compatible one on next startup
UPDATE users SET password_hash = NULL WHERE username = 'admin' AND global_role = 'GLOBAL_ADMIN';
