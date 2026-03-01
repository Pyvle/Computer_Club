alter table users
    add column if not exists global_role varchar(32) not null default 'USER';

create table if not exists club_staff (
    club_id bigint not null references clubs(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    role varchar(16) not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    primary key (club_id, user_id)
);

create index if not exists idx_club_staff_club
    on club_staff(club_id);

create index if not exists idx_club_staff_user
    on club_staff(user_id);
