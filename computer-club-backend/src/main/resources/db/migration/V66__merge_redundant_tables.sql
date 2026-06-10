-- Merge structurally close tables without dropping existing business data.

alter table product_order_items
    add column purchase_id bigint,
    add column product_order_id_snapshot bigint,
    add column product_order_created_at timestamp;

update product_order_items item
set purchase_id = po.purchase_id,
    product_order_id_snapshot = po.id,
    product_order_created_at = po.created_at
from product_orders po
where item.product_order_id = po.id;

alter table product_order_items
    alter column purchase_id set not null,
    add constraint fk_product_order_items_purchase
        foreign key (purchase_id) references purchases(id) on delete cascade;

create index idx_product_order_items_purchase_id
    on product_order_items(purchase_id);

alter table product_order_items
    drop column product_order_id;

drop table product_orders;

create table club_seat_type_settings (
    id bigserial primary key,
    club_id bigint not null references clubs(id) on delete cascade,
    seat_type varchar(20) not null check (seat_type in ('REGULAR', 'VIP')),
    price_per_hour_rub int check (price_per_hour_rub is null or price_per_hour_rub >= 0),
    title varchar(100) not null default '',
    specs_json text not null default '[]',
    unique (club_id, seat_type)
);

insert into club_seat_type_settings(club_id, seat_type, price_per_hour_rub, title, specs_json)
select
    coalesce(p.club_id, s.club_id),
    coalesce(p.seat_type, s.seat_type),
    p.price_per_hour_rub,
    coalesce(s.title, ''),
    coalesce(s.specs_json, '[]')
from club_seat_prices p
full outer join club_seat_spec s
    on s.club_id = p.club_id and s.seat_type = p.seat_type;

create index idx_club_seat_type_settings_club
    on club_seat_type_settings(club_id);

drop table club_seat_prices;
drop table club_seat_spec;

create table cart_items (
    id bigserial primary key,
    cart_id bigint not null references carts(id) on delete cascade,
    item_type varchar(16) not null check (item_type in ('BOOKING', 'PRODUCT')),
    product_id bigint references products(id),
    qty int check (qty is null or qty > 0),
    price_rub_snapshot int check (price_rub_snapshot is null or price_rub_snapshot >= 0),
    title_snapshot varchar(160),
    start_at timestamp,
    end_at timestamp,
    package_hours int,
    created_at timestamp not null default now(),
    check (
        (item_type = 'BOOKING'
            and product_id is null
            and qty is null
            and price_rub_snapshot is null
            and title_snapshot is null
            and start_at is not null
            and end_at is not null)
        or
        (item_type = 'PRODUCT'
            and product_id is not null
            and qty is not null
            and price_rub_snapshot is not null
            and title_snapshot is not null
            and start_at is null
            and end_at is null
            and package_hours is null)
    )
);

insert into cart_items(id, cart_id, item_type, start_at, end_at, package_hours, created_at)
select id, cart_id, 'BOOKING', start_at, end_at, package_hours, created_at
from cart_booking_lines;

insert into cart_items(id, cart_id, item_type, product_id, qty, price_rub_snapshot, title_snapshot, created_at)
select
    case
        when exists (select 1 from cart_booking_lines b where b.id = p.id)
            then p.id + (select coalesce(max(id), 0) from cart_booking_lines) + (select coalesce(max(id), 0) from cart_product_lines)
        else p.id
    end,
    p.cart_id,
    'PRODUCT',
    p.product_id,
    p.qty,
    p.price_rub_snapshot,
    p.title_snapshot,
    c.updated_at
from cart_product_lines p
join carts c on c.id = p.cart_id;

select setval(
    pg_get_serial_sequence('cart_items', 'id'),
    coalesce((select max(id) + 1 from cart_items), 1),
    false
);

create table cart_item_seats (
    cart_item_id bigint not null references cart_items(id) on delete cascade,
    seat_id bigint not null references seats(id),
    primary key (cart_item_id, seat_id)
);

insert into cart_item_seats(cart_item_id, seat_id)
select cart_booking_line_id, seat_id
from cart_booking_seats;

create index idx_cart_items_cart_type
    on cart_items(cart_id, item_type, id);

create unique index uq_cart_items_cart_product
    on cart_items(cart_id, product_id)
    where item_type = 'PRODUCT';

drop table cart_booking_seats;
drop table cart_booking_lines;
drop table cart_product_lines;

create table club_messages (
    id bigserial primary key,
    club_id bigint not null references clubs(id) on delete cascade,
    message_type varchar(32) not null check (message_type in ('USER_REPORT', 'PLATFORM_WARNING')),
    author_user_id bigint references users(id) on delete set null,
    message text not null,
    status varchar(32),
    created_at timestamptz not null default now(),
    check (
        (message_type = 'USER_REPORT' and author_user_id is not null and status is not null)
        or
        (message_type = 'PLATFORM_WARNING' and author_user_id is not null and status is null)
    )
);

insert into club_messages(id, club_id, message_type, author_user_id, message, status, created_at)
select id, club_id, 'USER_REPORT', user_id, message, status, created_at
from club_user_reports;

insert into club_messages(id, club_id, message_type, author_user_id, message, status, created_at)
select
    id + (select coalesce(max(id), 0) from club_user_reports),
    club_id,
    'PLATFORM_WARNING',
    created_by,
    message,
    null,
    created_at
from club_warning;

select setval(
    pg_get_serial_sequence('club_messages', 'id'),
    coalesce((select max(id) + 1 from club_messages), 1),
    false
);

create index idx_club_messages_club_type_created
    on club_messages(club_id, message_type, created_at desc);

create index idx_club_messages_author_created
    on club_messages(author_user_id, created_at desc);

drop table club_user_reports;
drop table club_warning;

create table club_permission_rules (
    id bigserial primary key,
    rule_type varchar(16) not null check (rule_type in ('ROLE_DEFAULT', 'USER_OVERRIDE')),
    club_id bigint references clubs(id) on delete cascade,
    user_id bigint references users(id) on delete cascade,
    role varchar(16),
    permission varchar(64) not null,
    granted boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    check (
        (rule_type = 'ROLE_DEFAULT'
            and club_id is null
            and user_id is null
            and role is not null
            and granted = true)
        or
        (rule_type = 'USER_OVERRIDE'
            and club_id is not null
            and user_id is not null
            and role is null)
    )
);

insert into club_permission_rules(rule_type, role, permission, granted)
select 'ROLE_DEFAULT', role, permission, true
from club_role_permissions;

insert into club_permission_rules(rule_type, club_id, user_id, permission, granted, created_at, updated_at)
select 'USER_OVERRIDE', club_id, user_id, permission, granted, created_at, updated_at
from club_user_permission_overrides;

create unique index uq_club_permission_rules_role_default
    on club_permission_rules(role, permission)
    where rule_type = 'ROLE_DEFAULT';

create unique index uq_club_permission_rules_user_override
    on club_permission_rules(club_id, user_id, permission)
    where rule_type = 'USER_OVERRIDE';

create index idx_club_permission_rules_user
    on club_permission_rules(club_id, user_id)
    where rule_type = 'USER_OVERRIDE';

drop table club_user_permission_overrides;
drop table club_role_permissions;
