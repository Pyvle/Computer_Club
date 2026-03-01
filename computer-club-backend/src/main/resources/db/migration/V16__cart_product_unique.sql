-- Dedupe cart_product_lines and prevent duplicates (cart_id, product_id).
-- This fixes "ghost" items when a cart accidentally has multiple lines for the same product.

-- 1) Merge duplicates: sum qty into the smallest id per (cart_id, product_id), keep latest snapshots
with dupes as (
    select
        cart_id,
        product_id,
        min(id) as keep_id,
        sum(qty) as total_qty
    from cart_product_lines
    group by cart_id, product_id
    having count(*) > 1
),
latest as (
    select distinct on (cart_id, product_id)
        cart_id,
        product_id,
        price_rub_snapshot,
        title_snapshot
    from cart_product_lines
    order by cart_id, product_id, id desc
)
update cart_product_lines l
set
    qty = d.total_qty,
    price_rub_snapshot = lt.price_rub_snapshot,
    title_snapshot = lt.title_snapshot
from dupes d
join latest lt on lt.cart_id = d.cart_id and lt.product_id = d.product_id
where l.id = d.keep_id;

delete from cart_product_lines l
using (
    -- CTE scope is per-statement in PostgreSQL, so we re-compute duplicates for the DELETE.
    select
        cart_id,
        product_id,
        min(id) as keep_id
    from cart_product_lines
    group by cart_id, product_id
    having count(*) > 1
) d
where l.cart_id = d.cart_id
  and l.product_id = d.product_id
  and l.id <> d.keep_id;

-- 2) Prevent future duplicates
create unique index if not exists uq_cart_product_lines_cart_product
    on cart_product_lines(cart_id, product_id);
