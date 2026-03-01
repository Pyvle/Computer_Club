-- Club floorplans (multiple per club) stored as JSONB for flexible editor
create table if not exists club_floorplans (
  id bigserial primary key,
  club_id bigint not null references clubs(id) on delete cascade,
  name varchar(200) not null,
  width int not null,
  height int not null,
  grid_size int not null default 10,
  status varchar(20) not null, -- DRAFT / PUBLISHED / ARCHIVED
  version int not null default 1,
  data jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists idx_club_floorplans_club on club_floorplans(club_id);
create index if not exists idx_club_floorplans_status on club_floorplans(status);

-- At most one published floorplan per club (partial unique index)
create unique index if not exists ux_club_floorplans_one_published_per_club
on club_floorplans(club_id)
where status = 'PUBLISHED';
