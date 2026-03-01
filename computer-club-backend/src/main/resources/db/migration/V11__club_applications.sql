create table if not exists club_applications (
    id bigserial primary key,
    applicant_user_id bigint not null references users(id) on delete restrict,

    club_name varchar(120) not null,
    address varchar(255) not null,
    location_text varchar(255),
    description text,

    status varchar(16) not null,

    decision_comment text,
    decided_by_user_id bigint references users(id) on delete set null,
    decided_at timestamp,

    created_club_id bigint references clubs(id) on delete set null,

    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create index if not exists idx_club_applications_applicant on club_applications(applicant_user_id);
create index if not exists idx_club_applications_status on club_applications(status);

-- 1 pending application per user (PostgreSQL partial unique index)
create unique index if not exists uq_club_applications_pending_per_user
    on club_applications(applicant_user_id)
    where status = 'PENDING';
