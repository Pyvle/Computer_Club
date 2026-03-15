-- удаляем устаревшие и всегда-null колонки
ALTER TABLE users DROP COLUMN IF EXISTS email;

ALTER TABLE purchases DROP COLUMN IF EXISTS external_payment_id;
ALTER TABLE purchases DROP COLUMN IF EXISTS payment_method;

ALTER TABLE product_orders DROP COLUMN IF EXISTS ready_by;
ALTER TABLE product_orders DROP COLUMN IF EXISTS ready_by_policy;
ALTER TABLE product_orders DROP COLUMN IF EXISTS status;

DROP TABLE IF EXISTS idempotency_keys;
