create table if not exists otp_challenges (
    id bigserial primary key,
    phone varchar(32) not null,
    code_hash varchar(128) not null,
    expires_at timestamp not null,
    attempts_left int not null default 5,
    resend_available_at timestamp not null,
    status varchar(16) not null default 'PENDING',
    created_at timestamp not null default now(),
    verified_at timestamp
);

create index if not exists idx_otp_phone_created
    on otp_challenges(phone, created_at desc);

create index if not exists idx_otp_expires
    on otp_challenges(expires_at);

create table if not exists auth_sessions (
    id bigserial primary key,
    user_id bigint not null references users(id) on delete cascade,
    refresh_token_hash varchar(128) not null unique,
    user_agent varchar(255),
    ip varchar(64),
    expires_at timestamp not null,
    created_at timestamp not null default now(),
    revoked_at timestamp
);

create index if not exists idx_auth_sessions_user
    on auth_sessions(user_id);

create index if not exists idx_auth_sessions_expires
    on auth_sessions(expires_at);
