-- Step 10: Idempotency keys (checkout and other POST actions)
create table if not exists idempotency_keys (
    id varchar(128) primary key,
    user_id bigint not null references users(id),
    endpoint varchar(200) not null,
    request_hash varchar(64) not null,
    status_code int not null,
    response_body jsonb not null,
    created_at timestamptz not null default now(),
    expires_at timestamptz not null
);

create index if not exists idx_idempotency_keys_user_endpoint on idempotency_keys(user_id, endpoint);

-- Step 12: Audit log
create table if not exists audit_log (
    id bigserial primary key,
    actor_user_id bigint not null references users(id),
    club_id bigint null references clubs(id),
    action varchar(80) not null,
    entity_type varchar(80) not null,
    entity_id varchar(80) null,
    before_data jsonb null,
    after_data jsonb null,
    created_at timestamptz not null default now()
);

create index if not exists idx_audit_log_club_created_at on audit_log(club_id, created_at desc);
create index if not exists idx_audit_log_actor_created_at on audit_log(actor_user_id, created_at desc);
