create table if not exists seats (
    id bigserial primary key,
    club_id bigint not null references clubs(id) on delete cascade,
    label varchar(32) not null,
    type varchar(16) not null, -- REGULAR / VIP
    is_active boolean not null default true,
    sort_order int not null default 0
);

create table if not exists bookings (
    id bigserial primary key,
    user_id bigint not null references users(id),
    club_id bigint not null references clubs(id),
    start_at timestamp not null,
    end_at timestamp not null,
    package_hours int,
    rate_rub_per_hour_snapshot int not null default 0,
    total_rub_snapshot int not null default 0,
    status varchar(16) not null, -- UPCOMING / ACTIVE / DONE / CANCELED
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint chk_booking_time check (end_at > start_at)
);

create table if not exists booking_seats (
    booking_id bigint not null references bookings(id) on delete cascade,
    seat_id bigint not null references seats(id),
    primary key (booking_id, seat_id)
);

create index if not exists idx_seats_club_active_sort
    on seats (club_id, is_active, sort_order, id);

create index if not exists idx_bookings_club_time_status
    on bookings (club_id, start_at, end_at, status);

create index if not exists idx_booking_seats_seat
    on booking_seats (seat_id);

-- уникальность названий мест внутри клуба (A1, A2 и т.п.)
create unique index if not exists uq_seats_club_label
    on seats (club_id, label);

-- seed мест (для существующих клубов)
insert into seats (club_id, label, type, is_active, sort_order)
select c.id, s.label, s.type, true, s.sort_order
from clubs c
cross join (
    values
      ('A1', 'REGULAR', 1),
      ('A2', 'REGULAR', 2),
      ('A3', 'REGULAR', 3),
      ('A4', 'REGULAR', 4),
      ('V1', 'VIP', 101),
      ('V2', 'VIP', 102)
) as s(label, type, sort_order)
where not exists (
    select 1 from seats x
    where x.club_id = c.id and x.label = s.label
);