CREATE TABLE club_seat_spec (
    id         BIGSERIAL PRIMARY KEY,
    club_id    BIGINT       NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    seat_type  VARCHAR(20)  NOT NULL CHECK (seat_type IN ('REGULAR', 'VIP')),
    title      VARCHAR(100) NOT NULL DEFAULT '',
    specs_json TEXT         NOT NULL DEFAULT '[]',
    CONSTRAINT uq_club_seat_spec UNIQUE (club_id, seat_type)
);
