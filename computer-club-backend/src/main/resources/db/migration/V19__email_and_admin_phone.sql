ALTER TABLE users ADD COLUMN IF NOT EXISTS email varchar(255);

-- задаём телефон для GLOBAL_ADMIN, чтобы он мог войти через OTP
UPDATE users SET phone = '+70000000000' WHERE username = 'admin' AND phone IS NULL;
