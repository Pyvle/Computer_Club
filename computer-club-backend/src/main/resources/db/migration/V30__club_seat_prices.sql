CREATE TABLE club_seat_prices (
    id               BIGSERIAL    PRIMARY KEY,
    club_id          BIGINT       NOT NULL REFERENCES clubs(id),
    seat_type        VARCHAR(20)  NOT NULL,
    price_per_hour_rub INT        NOT NULL CHECK (price_per_hour_rub >= 0),
    UNIQUE (club_id, seat_type)
);

CREATE INDEX idx_seat_prices_club ON club_seat_prices(club_id);
