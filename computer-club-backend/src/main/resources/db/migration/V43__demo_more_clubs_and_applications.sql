CREATE TEMP TABLE demo_club_name_map (
    old_name text,
    new_name text
) ON COMMIT DROP;

INSERT INTO demo_club_name_map (old_name, new_name) VALUES
    ('Cybersport Arena Central', 'Кибер Арена'),
    ('Cyber Arena Москва', 'Кибер Арена'),
    ('GG Hub North', 'Игровая Зона'),
    ('GameZone Санкт-Петербург', 'Игровая Зона'),
    ('Pixel Zone West', 'Пиксель'),
    ('Pixel Club Казань', 'Пиксель'),
    ('NightByte Екатеринбург', 'Ночной Байт'),
    ('Hardcore Gaming Новосибирск', 'Жесткий Режим'),
    ('Respawn Center Нижний Новгород', 'Возрождение'),
    ('Cyber Lounge Самара', 'Кибер Лаунж'),
    ('Level Up Краснодар', 'Новый Уровень'),
    ('SteamHall Воронеж', 'Паровой Зал'),
    ('Arena Lite Пермь', 'Легкая Арена');

UPDATE club_applications a
SET club_name = m.new_name,
    updated_at = localtimestamp
FROM demo_club_name_map m
WHERE a.club_name = m.old_name;

UPDATE clubs c
SET name = m.new_name,
    updated_at = localtimestamp
FROM demo_club_name_map m
WHERE c.name = m.old_name
  AND NOT EXISTS (
      SELECT 1
      FROM clubs existing
      WHERE existing.name = m.new_name
        AND existing.id <> c.id
  );

WITH demo_users(phone, is_active, global_role) AS (
    VALUES
        ('+79991234567', true, 'USER'),
        ('+79030000025', true, 'USER'),
        ('+79030000026', true, 'USER'),
        ('+79030000027', true, 'USER'),
        ('+79030000028', true, 'USER'),
        ('+79030000029', true, 'USER'),
        ('+79030000030', true, 'USER'),
        ('+79030000031', true, 'USER'),
        ('+79030000032', true, 'USER'),
        ('+79030000033', true, 'USER'),
        ('+79030000034', true, 'USER'),
        ('+79030000035', true, 'USER'),
        ('+79030000036', true, 'USER'),
        ('+79030000037', true, 'USER'),
        ('+79030000038', true, 'USER'),
        ('+79030000039', true, 'USER'),
        ('+79030000040', true, 'USER')
)
INSERT INTO users (phone, password_hash, is_active, global_role, created_at, updated_at)
SELECT phone, null, is_active, global_role, localtimestamp - interval '3 days', localtimestamp
FROM demo_users
ON CONFLICT (phone) DO UPDATE
SET is_active = excluded.is_active,
    global_role = excluded.global_role,
    updated_at = localtimestamp;

CREATE TEMP TABLE demo_extra_club_seed (
    name          text,
    address_full  text,
    address_short text,
    location_text text,
    description   text,
    latitude      double precision,
    longitude     double precision,
    image_url     text
) ON COMMIT DROP;

INSERT INTO demo_extra_club_seed (name, address_full, address_short, location_text, description, latitude, longitude, image_url) VALUES
    ('Северный Портал',
     'Тюмень, ул. Республики, 92, 2 этаж',
     'Тюмень, ул. Республики, 92',
     'Центр Тюмени',
     'Клуб для вечерних командных тренировок: просторный общий зал, отдельные VIP-места и стойка администратора у входа.',
     57.152985, 65.541227,
     'https://images.unsplash.com/photo-1511882150382-421056c89033?auto=format&fit=crop&w=1200&q=80'),
    ('Линия Пикселей',
     'Омск, проспект Карла Маркса, 18, 1 этаж',
     'Омск, проспект Карла Маркса, 18',
     'Площадь Ленина',
     'Небольшой клуб с быстрым бронированием, регулярными турнирами выходного дня и несколькими тихими местами для стримов.',
     54.984856, 73.367452,
     'https://images.unsplash.com/photo-1542751110-97427bbecf20?auto=format&fit=crop&w=1200&q=80');

UPDATE clubs c
SET address_full  = d.address_full,
    address_short = d.address_short,
    location_text = d.location_text,
    description   = d.description,
    latitude      = d.latitude,
    longitude     = d.longitude,
    image_url     = d.image_url,
    is_active     = true,
    is_blocked    = false,
    block_reason  = null,
    updated_at    = localtimestamp
FROM demo_extra_club_seed d
WHERE c.name = d.name;

INSERT INTO clubs (name, address_full, address_short, location_text, description, latitude, longitude, image_url, is_active, is_blocked, created_at, updated_at)
SELECT d.name, d.address_full, d.address_short, d.location_text, d.description, d.latitude, d.longitude, d.image_url, true, false, localtimestamp, localtimestamp
FROM demo_extra_club_seed d
WHERE NOT EXISTS (SELECT 1 FROM clubs c WHERE c.name = d.name);

WITH staff_rows(club_name, phone, role, added_by_phone) AS (
    VALUES
        ('Северный Портал', '+79991234567', 'OWNER', '+79030000002'),
        ('Северный Портал', '+79030000025', 'ADMIN', '+79991234567'),
        ('Линия Пикселей', '+79991234567', 'OWNER', '+79030000002'),
        ('Линия Пикселей', '+79030000026', 'ADMIN', '+79991234567')
)
INSERT INTO club_staff (club_id, user_id, role, added_by_user_id, created_at, updated_at)
SELECT c.id, u.id, s.role, added_by.id, localtimestamp - interval '2 days', localtimestamp
FROM staff_rows s
JOIN clubs c ON c.name = s.club_name
JOIN users u ON u.phone = s.phone
LEFT JOIN users added_by ON added_by.phone = s.added_by_phone
ON CONFLICT (club_id, user_id) DO UPDATE
SET role = excluded.role,
    added_by_user_id = excluded.added_by_user_id,
    updated_at = localtimestamp;

WITH seat_seed(label, seat_type, is_active, sort_order) AS (
    VALUES
        ('A1', 'REGULAR', true, 1),
        ('A2', 'REGULAR', true, 2),
        ('A3', 'REGULAR', true, 3),
        ('A4', 'REGULAR', true, 4),
        ('B1', 'REGULAR', true, 5),
        ('B2', 'REGULAR', true, 6),
        ('V1', 'VIP', true, 101),
        ('V2', 'VIP', true, 102)
)
INSERT INTO seats (club_id, label, type, is_active, sort_order)
SELECT c.id, s.label, s.seat_type, s.is_active, s.sort_order
FROM clubs c
CROSS JOIN seat_seed s
WHERE c.name IN (SELECT name FROM demo_extra_club_seed)
ON CONFLICT (club_id, label) DO UPDATE
SET type = excluded.type,
    is_active = excluded.is_active,
    sort_order = excluded.sort_order;

INSERT INTO club_seat_prices (club_id, seat_type, price_per_hour_rub)
SELECT c.id, v.seat_type, v.price_per_hour_rub
FROM clubs c
CROSS JOIN (
    VALUES
        ('REGULAR', 120),
        ('VIP', 240)
) AS v(seat_type, price_per_hour_rub)
WHERE c.name IN (SELECT name FROM demo_extra_club_seed)
ON CONFLICT (club_id, seat_type) DO UPDATE
SET price_per_hour_rub = excluded.price_per_hour_rub;

INSERT INTO club_seat_spec (club_id, seat_type, title, specs_json)
SELECT c.id, v.seat_type, v.title, v.specs_json
FROM clubs c
CROSS JOIN (
    VALUES
        ('REGULAR', 'Стандартное место', '[{"name":"Процессор","value":"Intel Core i5-12400F"},{"name":"Видеокарта","value":"GeForce RTX 3060"},{"name":"Монитор","value":"24\", 144 Гц"}]'),
        ('VIP', 'VIP-место', '[{"name":"Процессор","value":"Intel Core i7-13700F"},{"name":"Видеокарта","value":"GeForce RTX 4070"},{"name":"Монитор","value":"27\", 240 Гц"}]')
) AS v(seat_type, title, specs_json)
WHERE c.name IN (SELECT name FROM demo_extra_club_seed)
ON CONFLICT (club_id, seat_type) DO UPDATE
SET title = excluded.title,
    specs_json = excluded.specs_json;

WITH time_package_seed(name, hours, price_per_hour_rub, available_from, available_to, sort_order) AS (
    VALUES
        ('Стандарт 1 час', 1, 120, null::time, null::time, 1),
        ('VIP 1 час', 1, 240, null::time, null::time, 2),
        ('Дневной пакет', 5, 90, time '10:00', time '18:00', 3),
        ('Ночной пакет', 9, 60, time '23:00', time '08:00', 4)
)
INSERT INTO club_time_packages (club_id, name, hours, price_per_hour_rub, available_from, available_to, is_active, sort_order)
SELECT c.id, d.name, d.hours, d.price_per_hour_rub, d.available_from, d.available_to, true, d.sort_order
FROM clubs c
CROSS JOIN time_package_seed d
WHERE c.name IN (SELECT name FROM demo_extra_club_seed)
  AND NOT EXISTS (
      SELECT 1
      FROM club_time_packages tp
      WHERE tp.club_id = c.id
        AND tp.name = d.name
  );

INSERT INTO club_products (club_id, product_id, price_rub, is_available)
SELECT c.id,
       p.id,
       CASE
           WHEN pc.title = 'Напитки' THEN 140
           WHEN pc.title = 'Снеки' THEN 150
           WHEN pc.title = 'Еда' THEN 260
           ELSE 180
       END,
       true
FROM clubs c
CROSS JOIN products p
JOIN product_categories pc ON pc.id = p.category_id
WHERE c.name IN (SELECT name FROM demo_extra_club_seed)
ON CONFLICT (club_id, product_id) DO UPDATE
SET price_rub = excluded.price_rub,
    is_available = true;

WITH floorplans(club_name, data) AS (
    VALUES
        ('Северный Портал', '{"version":1,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":900,"h":20},{"id":"wall-south","type":"wall","x":0,"y":520,"w":900,"h":20},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":80,"y":80,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":170,"y":80,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":260,"y":80,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":350,"y":80,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":610,"y":290,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":720,"y":290,"w":90,"h":90}]}'::jsonb),
        ('Линия Пикселей', '{"version":1,"items":[{"id":"wall-left","type":"wall","x":0,"y":0,"w":20,"h":540},{"id":"wall-right","type":"wall","x":880,"y":0,"w":20,"h":540},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":90,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":90,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":220,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":220,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":650,"y":120,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":650,"y":240,"w":90,"h":90}]}'::jsonb)
)
INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT c.id, 'Основная схема', 900, 540, 10, 'PUBLISHED', 1, f.data, localtimestamp, localtimestamp
FROM floorplans f
JOIN clubs c ON c.name = f.club_name
ON CONFLICT DO NOTHING;

WITH apps(applicant_phone, club_name, address, location_text, description, status, decision_comment, decided_by_phone, created_club_name, created_at) AS (
    VALUES
        ('+79030000025', 'Игровой Горизонт', 'Самара, Московское шоссе, 4', 'Московское шоссе', 'Клуб на 18 мест рядом с университетским корпусом, с отдельной зоной для командных тренировок.', 'PENDING', null, null, null, localtimestamp - interval '5 minutes'),
        ('+79030000026', 'Точка Победы', 'Краснодар, ул. Красная, 109', 'Центр Краснодара', 'Планируется клуб с мини-баром, VIP-комнатой и местами для регулярных турниров.', 'PENDING', null, null, null, localtimestamp - interval '10 minutes'),
        ('+79030000027', 'Арена Легенд', 'Воронеж, пр-т Революции, 38', 'Проспект Революции', 'Заявка на просторный клуб с 24 игровыми станциями и отдельной стойкой администратора.', 'PENDING', null, null, null, localtimestamp - interval '15 minutes'),
        ('+79030000028', 'Респаун', 'Пермь, Комсомольский проспект, 33', 'Комсомольский проспект', 'Новый клуб для ночных пакетов, быстрых бронирований и небольших локальных турниров.', 'PENDING', null, null, null, localtimestamp - interval '20 minutes'),
        ('+79030000029', 'Красный Джойстик', 'Уфа, ул. Ленина, 70', 'Гостиный двор', 'Клуб на первом этаже торгового центра с 16 стандартными местами и 4 VIP-местами.', 'PENDING', null, null, null, localtimestamp - interval '25 minutes'),
        ('+79030000030', 'Пауза', 'Челябинск, проспект Ленина, 61', 'Площадь Революции', 'Компактный клуб рядом с остановками, рассчитанный на школьные и студенческие команды.', 'PENDING', null, null, null, localtimestamp - interval '30 minutes'),
        ('+79030000031', 'Турбо Зал', 'Ростов-на-Дону, ул. Большая Садовая, 46', 'Большая Садовая', 'Заявка на клуб с акцентом на соревновательные игры, стримы и вечерние мероприятия.', 'PENDING', null, null, null, localtimestamp - interval '35 minutes'),
        ('+79030000032', 'Новая Катка', 'Волгоград, проспект Ленина, 15', 'Центральная набережная', 'Планируется клуб на 20 мест с зоной ожидания, напитками и быстрым интернетом.', 'PENDING', null, null, null, localtimestamp - interval '40 minutes'),
        ('+79030000033', 'Кибер Смена', 'Саратов, ул. Московская, 122', 'Московская улица', 'Клуб для дневных тарифов и командных тренировок, помещение уже подготовлено к ремонту.', 'PENDING', null, null, null, localtimestamp - interval '45 minutes'),
        ('+79030000034', 'Северный Рейд', 'Ярославль, ул. Свободы, 12', 'Центр города', 'Заявка на клуб с тихой VIP-зоной, отдельным входом и круглосуточным графиком.', 'PENDING', null, null, null, localtimestamp - interval '50 minutes'),
        ('+79030000035', 'Лига Компьютеров', 'Тула, проспект Ленина, 85', 'Проспект Ленина', 'Клуб на 14 мест с гибкими тарифами и готовым договором аренды.', 'PENDING', null, null, null, localtimestamp - interval '55 minutes'),
        ('+79030000036', 'Тихий Рейд', 'Ижевск, ул. Пушкинская, 163', 'Центральная площадь', 'Небольшой клуб с хорошей шумоизоляцией, рассчитанный на постоянных игроков района.', 'PENDING', null, null, null, localtimestamp - interval '60 minutes'),
        ('+79030000037', 'Быстрый Старт', 'Киров, ул. Воровского, 43', 'Театральная площадь', 'Заявка на клуб рядом с учебными корпусами, основной упор на дневные пакеты.', 'PENDING', null, null, null, localtimestamp - interval '65 minutes'),
        ('+79030000038', 'Пятый Раунд', 'Барнаул, проспект Ленина, 54', 'Центр Барнаула', 'Планируется клуб с 12 стандартными местами и двумя зонами отдыха.', 'PENDING', null, null, null, localtimestamp - interval '70 minutes'),
        ('+79030000023', 'Новый Уровень', 'Краснодар, ул. Красная, 109', 'Центр Краснодара', 'Клуб с отдельной комнатой для тренировок команд и мини-баром.', 'REVISION_REQUESTED', 'Добавьте договор аренды помещения и фотографии входной группы.', '+79030000002', null, localtimestamp - interval '7 days'),
        ('+79030000024', 'Паровой Зал', 'Воронеж, пр-т Революции, 38', 'Проспект Революции', 'Небольшой клуб на 10 мест в подвальном помещении.', 'REJECTED', 'Помещение не соответствует требованиям по вентиляции и эвакуационному выходу.', '+79030000002', null, localtimestamp - interval '12 days'),
        ('+79030000018', 'Пиксель', 'Казань, ул. Баумана, 51', 'Центр Казани', 'Заявка одобрена, клуб создан на платформе.', 'APPROVED', 'Документы проверены, клуб опубликован.', '+79030000002', 'Пиксель', localtimestamp - interval '25 days'),
        ('+79030000021', 'Легкая Арена', 'Пермь, Комсомольский проспект, 33', 'Комсомольский проспект', 'Черновик заявки на клуб с 12 стандартными местами.', 'DRAFT', null, null, null, localtimestamp - interval '1 day')
)
INSERT INTO club_applications (applicant_user_id, club_name, address, location_text, description, status, decision_comment, decided_by_user_id, decided_at, created_club_id, created_at, updated_at)
SELECT applicant.id,
       a.club_name,
       a.address,
       a.location_text,
       a.description,
       a.status,
       a.decision_comment,
       decided_by.id,
       CASE WHEN a.decided_by_phone IS NULL THEN null ELSE a.created_at + interval '1 day' END,
       created_club.id,
       a.created_at,
       a.created_at + interval '1 day'
FROM apps a
JOIN users applicant ON applicant.phone = a.applicant_phone
LEFT JOIN users decided_by ON decided_by.phone = a.decided_by_phone
LEFT JOIN clubs created_club ON created_club.name = a.created_club_name
WHERE NOT EXISTS (
    SELECT 1
    FROM club_applications existing
    WHERE existing.applicant_user_id = applicant.id
      AND existing.status = a.status
)
ON CONFLICT DO NOTHING;
