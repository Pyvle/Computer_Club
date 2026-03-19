create table if not exists club_user_reports (
    id         bigserial primary key,
    club_id    bigint    not null references clubs(id) on delete cascade,
    user_id    bigint    not null references users(id) on delete cascade,
    message    text      not null,
    created_at timestamp not null default now()
);

create index if not exists idx_club_user_reports_club on club_user_reports(club_id, created_at desc);
