create table if not exists carts (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    club_id bigint not null references clubs(id),
    updated_at timestamp not null default now()
);

create unique index if not exists uq_carts_user_club
    on carts(user_id, club_id);

create table if not exists cart_booking_lines (
    id bigserial primary key,
    cart_id bigint not null references carts(id) on delete cascade,
    start_at timestamp not null,
    end_at timestamp not null,
    package_hours int,
    created_at timestamp not null default now(),
    constraint chk_cart_booking_time check (end_at > start_at)
);

create table if not exists cart_booking_seats (
    cart_booking_line_id bigint not null references cart_booking_lines(id) on delete cascade,
    seat_id bigint not null references seats(id),
    primary key (cart_booking_line_id, seat_id)
);

create table if not exists cart_product_lines (
    id bigserial primary key,
    cart_id bigint not null references carts(id) on delete cascade,
    product_id bigint not null references products(id),
    qty int not null check (qty > 0),
    price_rub_snapshot int not null check (price_rub_snapshot >= 0),
    title_snapshot varchar(160) not null
);

create index if not exists idx_cart_booking_lines_cart
    on cart_booking_lines(cart_id, id);

create index if not exists idx_cart_product_lines_cart
    on cart_product_lines(cart_id, id);