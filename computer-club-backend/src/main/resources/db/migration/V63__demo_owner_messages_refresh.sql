UPDATE clubs
SET name = 'Импульс',
    updated_at = localtimestamp
WHERE name = 'Пробный Клуб';

WITH report_updates(club_name, user_phone, old_message, new_message, new_status, new_created_at) AS (
    VALUES
        ('Импульс', '+79030000011', 'Не получилось оплатить будущую бронь картой.', 'Не проходит оплата будущей брони картой, заказ остается в ожидании.', 'NEW', localtimestamp - interval '4 hours'),
        ('Импульс', '+79991234567', 'Не нравишься ты мне', 'Администратор на стойке грубо ответил и не помог с переносом брони.', 'NEW', localtimestamp - interval '1 day'),
        ('Северный Портал', '+79030000003', 'На A2 периодически пропадает звук в наушниках.', 'На месте A2 периодически пропадает звук в гарнитуре.', 'NEW', localtimestamp - interval '3 hours'),
        ('Северный Портал', '+79030000005', 'После групповой брони не сразу выдали заказ с напитками.', 'После групповой брони заказ с напитками выдали только через 20 минут.', 'IN_PROGRESS', localtimestamp - interval '1 day'),
        ('Северный Портал', '+79030000013', 'Отмененная бронь отображается как активная в уведомлении.', 'После отмены брони уведомление еще несколько часов показывало ее как активную.', 'RESOLVED', localtimestamp - interval '6 days'),
        ('Линия Пикселей', '+79030000008', 'Прошу уточнить возврат по отмененному VIP-бронированию.', 'Нужна помощь с возвратом за отмененное VIP-бронирование.', 'IN_PROGRESS', localtimestamp - interval '2 days'),
        ('Линия Пикселей', '+79030000009', 'На B2 залипает клавиша Shift, нужна проверка.', 'На месте B2 залипает клавиша Shift, нужна замена клавиатуры.', 'NEW', localtimestamp - interval '5 hours')
)
UPDATE club_user_reports AS r
SET message = u.new_message,
    status = u.new_status,
    created_at = u.new_created_at
FROM report_updates AS u
JOIN clubs AS c ON c.name = u.club_name
JOIN users AS usr ON usr.phone = u.user_phone
WHERE r.club_id = c.id
  AND r.user_id = usr.id
  AND r.message = u.old_message;

WITH extra_reports(club_name, user_phone, message, status, created_at) AS (
    VALUES
        ('Импульс', '+79030000010', 'В VIP-комнате вечером было душно, проверьте кондиционер.', 'IN_PROGRESS', localtimestamp - interval '9 hours'),
        ('Северный Портал', '+79030000004', 'На стойке долго ждали администратора для подтверждения ночной брони.', 'NEW', localtimestamp - interval '6 hours'),
        ('Линия Пикселей', '+79030000006', 'Сотрудник долго не мог помочь с заменой мыши на месте B4.', 'NEW', localtimestamp - interval '7 hours')
)
INSERT INTO club_user_reports (club_id, user_id, message, status, created_at)
SELECT c.id, u.id, r.message, r.status, r.created_at
FROM extra_reports AS r
JOIN clubs AS c ON c.name = r.club_name
JOIN users AS u ON u.phone = r.user_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM club_user_reports existing
    WHERE existing.club_id = c.id
      AND existing.user_id = u.id
      AND existing.message = r.message
);

WITH warning_updates(club_name, old_message, new_message, new_created_at) AS (
    VALUES
        ('Импульс', 'Ты кто?', 'Проверьте карточку клуба: обновите описание и добавьте актуальные фото входной группы.', localtimestamp - interval '3 days'),
        ('Северный Портал', 'Демо-предупреждение платформы: проверьте документы по новому залу.', 'После перестановки мест обновите схему зала и подписи VIP-секции.', localtimestamp - interval '4 days'),
        ('Линия Пикселей', 'Демо-предупреждение платформы: обновите фотографии входной группы.', 'В каталоге есть позиции без актуальных фото. Обновите карточки товаров.', localtimestamp - interval '2 days')
)
UPDATE club_warning AS w
SET message = u.new_message,
    created_at = u.new_created_at
FROM warning_updates AS u
JOIN clubs AS c ON c.name = u.club_name
WHERE w.club_id = c.id
  AND w.message = u.old_message;

WITH extra_warnings(club_name, message, created_by_phone, created_at) AS (
    VALUES
        ('Импульс', 'По клубу поступило несколько жалоб на вечернюю смену администраторов. Проведите внутреннюю проверку.', '+79030000002', localtimestamp - interval '8 hours'),
        ('Северный Портал', 'Проверьте, что новые позиции в каталоге отмечены как доступные и с актуальными ценами.', '+79030000002', localtimestamp - interval '12 hours'),
        ('Линия Пикселей', 'После обновления расстановки проверьте схему зала в приложении и доступность мест.', '+79030000002', localtimestamp - interval '1 day')
)
INSERT INTO club_warning (club_id, message, created_by, created_at)
SELECT c.id, w.message, u.id, w.created_at
FROM extra_warnings AS w
JOIN clubs AS c ON c.name = w.club_name
JOIN users AS u ON u.phone = w.created_by_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM club_warning existing
    WHERE existing.club_id = c.id
      AND existing.message = w.message
);
