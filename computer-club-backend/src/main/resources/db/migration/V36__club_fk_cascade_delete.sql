-- Fix missing ON DELETE CASCADE on club_id foreign keys so that deleting a club
-- automatically removes all dependent data (carts, bookings, purchases, etc.)

-- carts
alter table carts
    drop constraint carts_club_id_fkey,
    add constraint carts_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;

-- bookings
alter table bookings
    drop constraint bookings_club_id_fkey,
    add constraint bookings_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;

-- purchases
alter table purchases
    drop constraint purchases_club_id_fkey,
    add constraint purchases_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;

-- product_orders
alter table product_orders
    drop constraint product_orders_club_id_fkey,
    add constraint product_orders_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;

-- club_time_packages
alter table club_time_packages
    drop constraint club_time_packages_club_id_fkey,
    add constraint club_time_packages_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;

-- club_seat_prices
alter table club_seat_prices
    drop constraint club_seat_prices_club_id_fkey,
    add constraint club_seat_prices_club_id_fkey
        foreign key (club_id) references clubs(id) on delete cascade;
