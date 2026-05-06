ALTER TABLE product_order_items ALTER COLUMN product_id DROP NOT NULL;

ALTER TABLE product_order_items
    DROP CONSTRAINT product_order_items_product_id_fkey,
    ADD CONSTRAINT product_order_items_product_id_fkey
        FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL;
