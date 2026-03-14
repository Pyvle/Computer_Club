create table if not exists user_favorite_clubs (
    user_id  bigint not null references users(id) on delete cascade,
    club_id  bigint not null references clubs(id) on delete cascade,
    created_at timestamp not null default now(),
    primary key (user_id, club_id)
);
