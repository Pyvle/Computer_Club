create table if not exists club_user_blocks (
    club_id bigint not null references clubs(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    is_blocked boolean not null default true,
    reason varchar(255),
    blocked_until timestamp,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    primary key (club_id, user_id)
);

create index if not exists idx_club_user_blocks_user
    on club_user_blocks(user_id);

create index if not exists idx_club_user_blocks_club
    on club_user_blocks(club_id);
