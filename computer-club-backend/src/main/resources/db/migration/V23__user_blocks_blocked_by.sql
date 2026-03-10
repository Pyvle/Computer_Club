alter table club_user_blocks
    add column if not exists blocked_by_user_id bigint references users(id) on delete set null;
