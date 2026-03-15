ALTER TABLE club_time_packages
    ADD COLUMN price_per_hour_rub INT,
    ADD COLUMN available_from     TIME,
    ADD COLUMN available_to       TIME;

UPDATE club_time_packages
    SET price_per_hour_rub = GREATEST(1, price_rub / NULLIF(hours, 0));

ALTER TABLE club_time_packages
    ALTER COLUMN price_per_hour_rub SET NOT NULL,
    DROP COLUMN price_rub;
