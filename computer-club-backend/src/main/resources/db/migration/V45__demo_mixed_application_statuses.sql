WITH status_updates(club_name, status, decision_comment, decided_by_phone, created_club_name, decided_offset) AS (
    VALUES
        ('Игровой Горизонт', 'PENDING', null, null, null, null::interval),
        ('Точка Победы', 'APPROVED', 'Документы проверены, клуб можно публиковать на платформе.', '+79030000002', 'Точка Победы', interval '20 minutes'),
        ('Арена Легенд', 'REJECTED', 'Заявка отменена: не приложены документы по помещению и пожарной безопасности.', '+79030000002', null, interval '25 minutes'),
        ('Респаун', 'PENDING', null, null, null, null::interval),
        ('Красный Джойстик', 'APPROVED', 'Заявка одобрена после проверки адреса и контактных данных владельца.', '+79030000002', 'Красный Джойстик', interval '30 minutes'),
        ('Пауза', 'REJECTED', 'Заявка отменена: выбранное помещение требует доработки по вентиляции.', '+79030000002', null, interval '35 minutes'),
        ('Турбо Зал', 'PENDING', null, null, null, null::interval),
        ('Новая Катка', 'APPROVED', 'Клуб одобрен, владелец может заполнить каталог и схему зала.', '+79030000002', 'Новая Катка', interval '40 minutes'),
        ('Кибер Смена', 'REJECTED', 'Заявка отменена по просьбе заявителя до повторной подачи документов.', '+79030000002', null, interval '45 minutes'),
        ('Северный Рейд', 'PENDING', null, null, null, null::interval),
        ('Лига Компьютеров', 'APPROVED', 'Все обязательные сведения заполнены, клуб добавлен в систему.', '+79030000002', 'Лига Компьютеров', interval '50 minutes'),
        ('Тихий Рейд', 'REJECTED', 'Заявка отменена: требуется уточнить юридический адрес и договор аренды.', '+79030000002', null, interval '55 minutes')
),
approved_clubs AS (
    INSERT INTO clubs (name, address_full, address_short, location_text, description, latitude, longitude, image_url, is_active, is_blocked, created_at, updated_at)
    SELECT a.club_name,
           a.address,
           left(a.address, 255),
           a.location_text,
           a.description,
           null,
           null,
           null,
           true,
           false,
           localtimestamp,
           localtimestamp
    FROM club_applications a
    JOIN status_updates s ON s.club_name = a.club_name
    WHERE s.status = 'APPROVED'
      AND NOT EXISTS (SELECT 1 FROM clubs c WHERE c.name = a.club_name)
    RETURNING id, name
)
UPDATE club_applications a
SET status = s.status,
    decision_comment = s.decision_comment,
    decided_by_user_id = decided_by.id,
    decided_at = CASE WHEN s.decided_by_phone IS NULL THEN null ELSE a.created_at + s.decided_offset END,
    created_club_id = CASE WHEN s.status = 'APPROVED' THEN c.id ELSE null END,
    updated_at = localtimestamp
FROM status_updates s
LEFT JOIN users decided_by ON decided_by.phone = s.decided_by_phone
LEFT JOIN clubs c ON c.name = s.created_club_name
WHERE a.club_name = s.club_name;
