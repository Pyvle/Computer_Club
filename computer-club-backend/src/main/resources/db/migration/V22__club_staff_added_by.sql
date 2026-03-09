ALTER TABLE club_staff
    ADD COLUMN added_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
