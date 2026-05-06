WITH demo_states(club_name, is_active, is_blocked, block_reason) AS (
    VALUES
        ('Лига Компьютеров', false, true, 'Проверка документов владельца и условий работы клуба.'),
        ('Новая Катка', false, false, null),
        ('Красный Джойстик', true, true, 'Временная блокировка до устранения замечаний платформы.'),
        ('Точка Победы', false, true, 'Клуб скрыт и заблокирован на время повторной модерации.'),
        ('Линия Пикселей', true, true, 'Ожидается подтверждение контактных данных.'),
        ('Северный Портал', false, false, null)
)
UPDATE clubs c
SET is_active = s.is_active,
    is_blocked = s.is_blocked,
    block_reason = s.block_reason,
    updated_at = localtimestamp
FROM demo_states s
WHERE c.name = s.club_name;
