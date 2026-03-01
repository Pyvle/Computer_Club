create table if not exists users (
    id bigserial primary key,
    phone varchar(32) not null unique,
    username varchar(64) not null,
    password_hash varchar(255),
    is_active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table if not exists clubs (
    id bigserial primary key,
    name varchar(120) not null,
    address varchar(255) not null,
    location_text varchar(255),
    description text,
    is_active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);