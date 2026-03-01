-- Club permissions layer (role -> permissions + per-user overrides)

create table if not exists club_role_permissions (
    role varchar(16) not null,
    permission varchar(64) not null,
    primary key (role, permission)
);

create table if not exists club_user_permission_overrides (
    club_id bigint not null references clubs(id) on delete cascade,
    user_id bigint not null references users(id) on delete cascade,
    permission varchar(64) not null,
    granted boolean not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (club_id, user_id, permission)
);

create index if not exists idx_cupo_club_user on club_user_permission_overrides(club_id, user_id);

-- Default mapping for current roles.
-- OWNER: full club permissions
insert into club_role_permissions(role, permission) values
    ('OWNER', 'CLUB_ADMINS_MANAGE'),
    ('OWNER', 'CLUB_CATALOG_MANAGE'),
    ('OWNER', 'CLUB_SEATS_MANAGE'),
    ('OWNER', 'CLUB_USER_BLOCKS_MANAGE'),
    ('OWNER', 'CLUB_FLOORPLANS_MANAGE'),
    ('OWNER', 'CLUB_REPORTS_VIEW')
on conflict do nothing;

-- ADMIN: everything except managing admins
insert into club_role_permissions(role, permission) values
    ('ADMIN', 'CLUB_CATALOG_MANAGE'),
    ('ADMIN', 'CLUB_SEATS_MANAGE'),
    ('ADMIN', 'CLUB_USER_BLOCKS_MANAGE'),
    ('ADMIN', 'CLUB_FLOORPLANS_MANAGE'),
    ('ADMIN', 'CLUB_REPORTS_VIEW')
on conflict do nothing;
