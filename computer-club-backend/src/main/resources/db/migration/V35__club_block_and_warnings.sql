ALTER TABLE clubs
    ADD COLUMN is_blocked   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN block_reason TEXT;

CREATE TABLE club_warning (
    id             BIGSERIAL PRIMARY KEY,
    club_id        BIGINT       NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    message        TEXT         NOT NULL,
    created_by     BIGINT       NOT NULL REFERENCES users(id),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
