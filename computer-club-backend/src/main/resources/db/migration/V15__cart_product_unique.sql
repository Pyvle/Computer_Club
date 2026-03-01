-- Deduplicate cart_product_lines by (cart_id, product_id)
-- Keep the smallest id, sum qty, and keep latest snapshots.

WITH d AS (
    SELECT
        cart_id,
        product_id,
        MIN(id)  AS keep_id,
        SUM(qty) AS total_qty,
        MAX(price_rub_snapshot) AS price_rub_snapshot,
        MAX(title_snapshot) AS title_snapshot
    FROM cart_product_lines
    GROUP BY cart_id, product_id
    HAVING COUNT(*) > 1
), upd AS (
    UPDATE cart_product_lines l
    SET
        qty = d.total_qty,
        price_rub_snapshot = d.price_rub_snapshot,
        title_snapshot = d.title_snapshot
    FROM d
    WHERE l.id = d.keep_id
    RETURNING l.id
)
DELETE FROM cart_product_lines l
USING d
WHERE l.cart_id = d.cart_id
  AND l.product_id = d.product_id
  AND l.id <> d.keep_id;

-- Prevent duplicates going forward
CREATE UNIQUE INDEX IF NOT EXISTS uq_cart_product_lines_cart_product
    ON cart_product_lines(cart_id, product_id);
