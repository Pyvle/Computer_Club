-- Разбиваем address на address_full (детальный) и address_short (для пользователя)
ALTER TABLE clubs RENAME COLUMN address TO address_full;

-- Начальное значение: копируем существующий адрес в short
ALTER TABLE clubs ADD COLUMN address_short VARCHAR(255) NOT NULL DEFAULT '';
UPDATE clubs SET address_short = LEFT(address_full, 255);
ALTER TABLE clubs ALTER COLUMN address_short DROP DEFAULT;
