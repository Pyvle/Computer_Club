create table if not exists purchases (
    id bigserial primary key,
    user_id bigint not null references users(id),
    club_id bigint not null references clubs(id),
    created_at timestamp not null default now(),
    booking_total_rub int not null default 0 check (booking_total_rub >= 0),
    products_total_rub int not null default 0 check (products_total_rub >= 0),
    total_rub int not null default 0 check (total_rub >= 0),
    payment_method varchar(24) not null,   -- CARD
    payment_status varchar(24) not null,   -- CREATED/PAID/FAILED/CANCELED/REFUND
    external_payment_id varchar(128)
);

alter table bookings
    add column if not exists purchase_id bigint references purchases(id);

create index if not exists idx_purchases_user_created_at
    on purchases(user_id, created_at desc);

create index if not exists idx_bookings_purchase_id
    on bookings(purchase_id);

create table if not exists product_orders (
    id bigserial primary key,
    purchase_id bigint not null unique references purchases(id) on delete cascade,
    user_id bigint not null references users(id),
    club_id bigint not null references clubs(id),
    created_at timestamp not null default now(),
    ready_by timestamp,
    ready_by_policy varchar(24) not null, -- ASAP/BOOKING_START/CUSTOM
    status varchar(24) not null,          -- NOT_READY/READY/CANCELED
    total_rub_snapshot int not null default 0 check (total_rub_snapshot >= 0)
);

create table if not exists product_order_items (
    id bigserial primary key,
    product_order_id bigint not null references product_orders(id) on delete cascade,
    product_id bigint not null references products(id),
    title_snapshot varchar(160) not null,
    price_rub_snapshot int not null check (price_rub_snapshot >= 0),
    qty int not null check (qty > 0)
);

create index if not exists idx_product_orders_user_created_at
    on product_orders(user_id, created_at desc);

create index if not exists idx_product_order_items_order_id
    on product_order_items(product_order_id);
