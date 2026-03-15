CREATE TABLE club_time_packages (
    id         BIGSERIAL    PRIMARY KEY,
    club_id    BIGINT       NOT NULL REFERENCES clubs(id),
    name       VARCHAR(100) NOT NULL,
    hours      INT          NOT NULL CHECK (hours > 0),
    price_rub  INT          NOT NULL CHECK (price_rub >= 0),
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_time_packages_club ON club_time_packages(club_id);
