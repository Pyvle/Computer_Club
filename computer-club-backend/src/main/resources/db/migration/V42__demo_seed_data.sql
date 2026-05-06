-- Демонстрационные данные для показа диплома.
-- Данные завязаны на естественные ключи (телефон пользователя, название клуба, название товара),
-- чтобы не полагаться на заранее известные значения sequence.

CREATE TEMP TABLE demo_club_seed (
    name          text,
    address_full  text,
    address_short text,
    location_text text,
    description   text,
    latitude      double precision,
    longitude     double precision,
    image_url     text
) ON COMMIT DROP;

INSERT INTO demo_club_seed (name, address_full, address_short, location_text, description, latitude, longitude, image_url) VALUES
('Cyber Arena Москва',
 'Москва, Пресненская наб., 10, стр. 2, башня IQ-квартал, 2 этаж',
 'Москва, Пресненская наб., 10',
 'Москва-Сити',
 'Флагманский клуб в деловом центре: 24 игровых станции, отдельная VIP-зона и турнирная комната. Подходит для вечерних сессий после работы, студенческих команд и небольших киберспортивных мероприятий.',
 55.748989, 37.539548,
 'https://images.unsplash.com/photo-1542751371-adc38448a05e?auto=format&fit=crop&w=1200&q=80'),
('GameZone Санкт-Петербург',
 'Санкт-Петербург, Невский проспект, 88, вход со стороны ул. Маяковского',
 'Санкт-Петербург, Невский проспект, 88',
 'Невский проспект',
 'Светлый клуб рядом с метро, где удобно бронировать места на несколько часов или брать ночной пакет. В зале тихая акустика, быстрый Wi-Fi и отдельная стойка с напитками.',
 59.934280, 30.335099,
 'https://images.unsplash.com/photo-1511512578047-dfb367046420?auto=format&fit=crop&w=1200&q=80'),
('Pixel Club Казань',
 'Казань, ул. Баумана, 51, ТЦ "Свита Холл", 3 этаж',
 'Казань, ул. Баумана, 51',
 'Центр Казани',
 'Клуб для регулярных игроков и командных тренировок: стабильный FPS, удобные кресла и внимательные администраторы. В выходные проходят мини-турниры по популярным дисциплинам.',
 55.790278, 49.134722,
 'https://images.unsplash.com/photo-1598550476439-6847785fcea6?auto=format&fit=crop&w=1200&q=80'),
('NightByte Екатеринбург',
 'Екатеринбург, ул. Малышева, 36, цокольный этаж',
 'Екатеринбург, ул. Малышева, 36',
 'Площадь 1905 года',
 'Ночной компьютерный клуб с акцентом на длинные игровые сессии. Есть стандартный зал, камерная VIP-зона и меню с горячими закусками до утра.',
 56.836945, 60.596389,
 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=1200&q=80'),
('Hardcore Gaming Новосибирск',
 'Новосибирск, Красный проспект, 50, 2 этаж',
 'Новосибирск, Красный проспект, 50',
 'Площадь Ленина',
 'Практичный клуб для тренировок, стримов и командных матчей. В основном зале много стандартных мест, а VIP-сектор отделён перегородкой и подходит для дуо-сессий.',
 55.030199, 82.920430,
 'https://images.unsplash.com/photo-1560253023-3ec5d502959f?auto=format&fit=crop&w=1200&q=80'),
('Respawn Center Нижний Новгород',
 'Нижний Новгород, ул. Большая Покровская, 35, 2 этаж',
 'Нижний Новгород, Большая Покровская, 35',
 'Большая Покровская',
 'Городской клуб в пешеходной зоне: удобен для спонтанных бронирований, встреч с друзьями и небольших локальных турниров. В меню много быстрых перекусов и напитков.',
 56.322781, 44.006516,
 'https://images.unsplash.com/photo-1600861195091-690c92f1d2cc?auto=format&fit=crop&w=1200&q=80');

UPDATE clubs
SET name = 'Cyber Arena Москва'
WHERE name = 'Cybersport Arena Central';

UPDATE clubs
SET name = 'GameZone Санкт-Петербург'
WHERE name = 'GG Hub North';

UPDATE clubs
SET name = 'Pixel Club Казань'
WHERE name = 'Pixel Zone West';

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
FROM demo_club_seed d
WHERE c.name = d.name;

INSERT INTO clubs (name, address_full, address_short, location_text, description, latitude, longitude, image_url, is_active, is_blocked, created_at, updated_at)
SELECT d.name, d.address_full, d.address_short, d.location_text, d.description, d.latitude, d.longitude, d.image_url, true, false, localtimestamp, localtimestamp
FROM demo_club_seed d
WHERE NOT EXISTS (SELECT 1 FROM clubs c WHERE c.name = d.name);

WITH demo_users(phone, is_active, global_role) AS (
    VALUES
        ('+70000000000', true,  'GLOBAL_ADMIN'), -- Александр Соколов
        ('+79030000002', true,  'GLOBAL_ADMIN'), -- Мария Волкова
        ('+79990000001', true,  'USER'),         -- Иван Петров
        ('+79030000003', true,  'USER'),         -- Анна Смирнова
        ('+79030000004', true,  'USER'),         -- Дмитрий Кузнецов
        ('+79030000005', true,  'USER'),         -- Екатерина Орлова
        ('+79030000006', true,  'USER'),         -- Сергей Никитин
        ('+79030000007', true,  'USER'),         -- Ольга Федорова
        ('+79030000008', true,  'USER'),         -- Павел Морозов
        ('+79030000009', false, 'USER'),         -- Виктория Белова
        ('+79030000010', false, 'USER'),         -- Максим Егоров
        ('+79030000011', true,  'USER'),         -- Юлия Захарова
        ('+79030000012', true,  'USER'),         -- Роман Лебедев
        ('+79030000013', true,  'USER'),         -- Наталья Крылова
        ('+79030000014', true,  'USER'),         -- Артем Васильев
        ('+79030000015', true,  'USER'),         -- Полина Соловьева
        ('+79030000016', true,  'USER'),         -- Андрей Михайлов
        ('+79030000017', true,  'USER'),         -- Ксения Новикова
        ('+79030000018', true,  'USER'),         -- Илья Громов
        ('+79030000019', true,  'USER'),         -- Светлана Романова
        ('+79030000020', true,  'USER'),         -- Денис Макаров
        ('+79030000021', true,  'USER'),         -- Алиса Котова
        ('+79030000022', true,  'USER'),         -- Тимур Сафин
        ('+79030000023', true,  'USER'),         -- Варвара Алексеева
        ('+79030000024', true,  'USER')          -- Глеб Фролов
)
INSERT INTO users (phone, password_hash, is_active, global_role, created_at, updated_at)
SELECT phone, null, is_active, global_role, localtimestamp - interval '45 days', localtimestamp
FROM demo_users
ON CONFLICT (phone) DO UPDATE
SET is_active   = excluded.is_active,
    global_role = excluded.global_role,
    updated_at  = localtimestamp;

UPDATE product_categories SET title = 'Еда', sort_order = 2, is_active = true WHERE title = 'Горячее';
UPDATE product_categories SET sort_order = 1, is_active = true WHERE title = 'Напитки';
UPDATE product_categories SET sort_order = 3, is_active = true WHERE title = 'Снеки';

INSERT INTO product_categories (title, sort_order, is_active)
SELECT 'Дополнительно', 4, true
WHERE NOT EXISTS (SELECT 1 FROM product_categories WHERE title = 'Дополнительно');

CREATE TEMP TABLE demo_product_seed (
    category_title text,
    title          text,
    description    text,
    image_url      text,
    base_price     int,
    sort_order     int
) ON COMMIT DROP;

INSERT INTO demo_product_seed (category_title, title, description, image_url, base_price, sort_order) VALUES
('Напитки', 'Кола 0.5 л', 'Охлажденная классическая кола в пластиковой бутылке.', 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=600&q=80', 120, 1),
('Напитки', 'Кола Zero 0.5 л', 'Кола без сахара для длинной игровой сессии.', 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?auto=format&fit=crop&w=600&q=80', 120, 2),
('Напитки', 'Вода негазированная 0.5 л', 'Питьевая вода комнатной температуры или из холодильника.', 'https://images.unsplash.com/photo-1523362628745-0c100150b504?auto=format&fit=crop&w=600&q=80', 80, 3),
('Напитки', 'Энергетик Drive 0.45 л', 'Безалкогольный энергетический напиток, подается охлажденным.', 'https://images.unsplash.com/photo-1622543925917-763c34d1a86e?auto=format&fit=crop&w=600&q=80', 180, 4),
('Напитки', 'Энергетик Adrenaline 0.449 л', 'Энергетик с ярким вкусом для ночных пакетов.', 'https://images.unsplash.com/photo-1622543925917-763c34d1a86e?auto=format&fit=crop&w=600&q=80', 190, 5),
('Напитки', 'Морс клюквенный 0.3 л', 'Домашний морс в закрытой бутылке, умеренно сладкий.', 'https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?auto=format&fit=crop&w=600&q=80', 110, 6),
('Еда', 'Пицца пепперони', 'Порционная пицца с пепперони, сыром и томатным соусом.', 'https://images.unsplash.com/photo-1628840042765-356cda07504e?auto=format&fit=crop&w=600&q=80', 390, 7),
('Еда', 'Пицца маргарита', 'Порционная пицца с моцареллой, базиликом и томатным соусом.', 'https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?auto=format&fit=crop&w=600&q=80', 350, 8),
('Еда', 'Бургер с говядиной', 'Бургер с котлетой из говядины, сыром, салатом и соусом.', 'https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=600&q=80', 330, 9),
('Еда', 'Чикенбургер', 'Бургер с куриной котлетой, маринованными огурцами и сливочным соусом.', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=600&q=80', 290, 10),
('Еда', 'Сэндвич с курицей', 'Сэндвич с куриной грудкой, сыром и свежими овощами.', 'https://images.unsplash.com/photo-1528735602780-2552fd46c7af?auto=format&fit=crop&w=600&q=80', 240, 11),
('Еда', 'Хот-дог', 'Горячий хот-дог с сосиской, хрустящим луком и двумя соусами.', 'https://images.unsplash.com/photo-1612392062631-94dd858cba88?auto=format&fit=crop&w=600&q=80', 220, 12),
('Еда', 'Наггетсы 8 шт', 'Куриные наггетсы с соусом на выбор.', 'https://images.unsplash.com/photo-1562967916-eb82221dfb92?auto=format&fit=crop&w=600&q=80', 260, 13),
('Снеки', 'Чипсы 140 г', 'Картофельные чипсы с солью или сыром.', 'https://images.unsplash.com/photo-1566478989037-eec170784d0b?auto=format&fit=crop&w=600&q=80', 150, 14),
('Снеки', 'Сухарики 90 г', 'Ржаные сухарики со вкусом сыра или бекона.', 'https://images.unsplash.com/photo-1621939514649-280e2ee25f60?auto=format&fit=crop&w=600&q=80', 95, 15),
('Снеки', 'Арахис соленый 90 г', 'Жареный соленый арахис в порционной упаковке.', 'https://images.unsplash.com/photo-1567892737950-30c4db37cd89?auto=format&fit=crop&w=600&q=80', 110, 16),
('Снеки', 'Шоколадный батончик', 'Батончик с карамелью и арахисом.', 'https://images.unsplash.com/photo-1621939514649-280e2ee25f60?auto=format&fit=crop&w=600&q=80', 85, 17),
('Снеки', 'Попкорн сырный', 'Порция сырного попкорна для компании.', 'https://images.unsplash.com/photo-1578849278619-e73505e9610f?auto=format&fit=crop&w=600&q=80', 140, 18),
('Снеки', 'Лапша быстрого приготовления', 'Лапша с куриным вкусом, готовится администратором на стойке.', 'https://images.unsplash.com/photo-1612927601601-6638404737ce?auto=format&fit=crop&w=600&q=80', 130, 19),
('Дополнительно', 'Аренда игровых наушников', 'Закрытые игровые наушники с микрофоном на время бронирования.', 'https://images.unsplash.com/photo-1599669454699-248893623440?auto=format&fit=crop&w=600&q=80', 120, 20),
('Дополнительно', 'Игровой коврик', 'Большой тканевый коврик для мыши, выдается на стойке администратора.', 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?auto=format&fit=crop&w=600&q=80', 90, 21),
('Дополнительно', 'Аренда геймпада', 'Геймпад для файтингов, гонок и локального кооператива.', 'https://images.unsplash.com/photo-1600080972464-8e5f35f63d08?auto=format&fit=crop&w=600&q=80', 160, 22),
('Дополнительно', 'Зарядка телефона', 'Кабель Type-C, Lightning или microUSB на время посещения.', 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80', 60, 23),
('Дополнительно', 'Аренда веб-камеры', 'Веб-камера Full HD для стримов и видеосвязи.', 'https://images.unsplash.com/photo-1587825140708-dfaf72ae4b04?auto=format&fit=crop&w=600&q=80', 180, 24);

UPDATE products SET title = 'Кола 0.5 л' WHERE title = 'Кола 0.5';
UPDATE products SET title = 'Энергетик Drive 0.45 л' WHERE title = 'Энергетик 0.45';
UPDATE products SET title = 'Чипсы 140 г' WHERE title = 'Чипсы 140г';
UPDATE products SET title = 'Арахис соленый 90 г' WHERE title = 'Арахис 90г';

INSERT INTO products (category_id, title, description, image_url, is_active)
SELECT c.id, d.title, d.description, d.image_url, true
FROM demo_product_seed d
JOIN (
    SELECT title, MIN(id) AS id
    FROM product_categories
    GROUP BY title
) c ON c.title = d.category_title
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.title = d.title);

UPDATE products p
SET category_id  = c.id,
    description  = d.description,
    image_url    = d.image_url,
    is_active    = true
FROM demo_product_seed d
JOIN (
    SELECT title, MIN(id) AS id
    FROM product_categories
    GROUP BY title
) c ON c.title = d.category_title
WHERE p.title = d.title;

INSERT INTO club_products (club_id, product_id, price_rub, is_available)
SELECT cl.id,
       p.id,
       d.base_price +
       CASE cl.name
           WHEN 'Cyber Arena Москва' THEN 20
           WHEN 'GameZone Санкт-Петербург' THEN 10
           WHEN 'Pixel Club Казань' THEN 0
           WHEN 'NightByte Екатеринбург' THEN 15
           WHEN 'Hardcore Gaming Новосибирск' THEN -5
           ELSE 0
       END,
       NOT (
           (cl.name = 'NightByte Екатеринбург' AND d.title = 'Пицца маргарита') OR
           (cl.name = 'Hardcore Gaming Новосибирск' AND d.title = 'Аренда веб-камеры') OR
           (cl.name = 'Respawn Center Нижний Новгород' AND d.title = 'Энергетик Adrenaline 0.449 л')
       )
FROM demo_product_seed d
JOIN (
    SELECT title, MIN(id) AS id
    FROM products
    GROUP BY title
) p ON p.title = d.title
JOIN clubs cl ON cl.name IN (SELECT name FROM demo_club_seed)
ON CONFLICT (club_id, product_id) DO UPDATE
SET price_rub    = excluded.price_rub,
    is_available = excluded.is_available;

CREATE TEMP TABLE demo_seat_seed (
    label      text,
    seat_type  text,
    is_active  boolean,
    sort_order int
) ON COMMIT DROP;

INSERT INTO demo_seat_seed (label, seat_type, is_active, sort_order) VALUES
('A1', 'REGULAR', true, 1), ('A2', 'REGULAR', true, 2), ('A3', 'REGULAR', true, 3),
('A4', 'REGULAR', true, 4), ('A5', 'REGULAR', true, 5), ('A6', 'REGULAR', true, 6),
('A7', 'REGULAR', true, 7), ('A8', 'REGULAR', true, 8), ('A9', 'REGULAR', true, 9),
('A10', 'REGULAR', true, 10),
('B1', 'REGULAR', true, 21), ('B2', 'REGULAR', true, 22), ('B3', 'REGULAR', true, 23),
('B4', 'REGULAR', true, 24), ('B5', 'REGULAR', true, 25), ('B6', 'REGULAR', true, 26),
('V1', 'VIP', true, 101), ('V2', 'VIP', true, 102), ('V3', 'VIP', true, 103),
('V4', 'VIP', true, 104), ('V5', 'VIP', true, 105),
('X1', 'REGULAR', false, 901), ('X2', 'VIP', false, 902);

INSERT INTO seats (club_id, label, type, is_active, sort_order)
SELECT c.id, s.label, s.seat_type, s.is_active, s.sort_order
FROM clubs c
CROSS JOIN demo_seat_seed s
WHERE c.name IN (SELECT name FROM demo_club_seed)
ON CONFLICT (club_id, label) DO UPDATE
SET type       = excluded.type,
    is_active  = excluded.is_active,
    sort_order = excluded.sort_order;

INSERT INTO club_seat_prices (club_id, seat_type, price_per_hour_rub)
SELECT c.id, v.seat_type, v.price_per_hour_rub
FROM clubs c
CROSS JOIN (VALUES ('REGULAR', 120), ('VIP', 250)) AS v(seat_type, price_per_hour_rub)
WHERE c.name IN (SELECT name FROM demo_club_seed)
ON CONFLICT (club_id, seat_type) DO UPDATE
SET price_per_hour_rub = excluded.price_per_hour_rub;

INSERT INTO club_seat_spec (club_id, seat_type, title, specs_json)
SELECT c.id,
       v.seat_type,
       v.title,
       v.specs_json
FROM clubs c
CROSS JOIN (
    VALUES
        ('REGULAR', 'Стандартное место', '[{"name":"Процессор","value":"Intel Core i5-12400F"},{"name":"Видеокарта","value":"GeForce RTX 3060"},{"name":"Монитор","value":"24\", 144 Гц"},{"name":"Периферия","value":"механическая клавиатура, игровая мышь"}]'),
        ('VIP', 'VIP-место', '[{"name":"Процессор","value":"Intel Core i7-13700F"},{"name":"Видеокарта","value":"GeForce RTX 4070"},{"name":"Монитор","value":"27\", 240 Гц"},{"name":"Комфорт","value":"широкое кресло, больше пространства, гарнитура"}]')
) AS v(seat_type, title, specs_json)
WHERE c.name IN (SELECT name FROM demo_club_seed)
ON CONFLICT (club_id, seat_type) DO UPDATE
SET title      = excluded.title,
    specs_json = excluded.specs_json;

CREATE TEMP TABLE demo_time_package_seed (
    name               text,
    hours              int,
    price_per_hour_rub int,
    available_from     time,
    available_to       time,
    sort_order         int
) ON COMMIT DROP;

INSERT INTO demo_time_package_seed (name, hours, price_per_hour_rub, available_from, available_to, sort_order) VALUES
('Стандарт 1 час', 1, 120, null, null, 1),
('VIP 1 час', 1, 250, null, null, 2),
('Дневной пакет (5 часов)', 5, 80, time '10:00', time '18:00', 3),
('Ночной пакет (23:00-08:00)', 9, 56, time '23:00', time '08:00', 4);

UPDATE club_time_packages tp
SET hours              = d.hours,
    price_per_hour_rub = d.price_per_hour_rub,
    available_from     = d.available_from,
    available_to       = d.available_to,
    is_active          = true,
    sort_order         = d.sort_order
FROM demo_time_package_seed d
JOIN clubs c ON c.name IN (SELECT name FROM demo_club_seed)
WHERE c.name IN (SELECT name FROM demo_club_seed)
  AND c.id = tp.club_id
  AND tp.name = d.name;

INSERT INTO club_time_packages (club_id, name, hours, price_per_hour_rub, available_from, available_to, is_active, sort_order)
SELECT c.id, d.name, d.hours, d.price_per_hour_rub, d.available_from, d.available_to, true, d.sort_order
FROM clubs c
CROSS JOIN demo_time_package_seed d
WHERE c.name IN (SELECT name FROM demo_club_seed)
  AND NOT EXISTS (
      SELECT 1
      FROM club_time_packages tp
      WHERE tp.club_id = c.id AND tp.name = d.name
  );

CREATE TEMP TABLE demo_floorplan_data ON COMMIT DROP AS
WITH demo_clubs AS (
    SELECT c.id AS club_id
    FROM clubs c
    WHERE c.name IN (SELECT name FROM demo_club_seed)
),
floor_items AS (
    SELECT dc.club_id,
           row_no * 15 + col_no AS sort_key,
           jsonb_build_object(
               'type', 'FLOOR',
               'col', col_no,
               'row', row_no,
               'roomType', CASE WHEN col_no >= 10 THEN 'VIP' ELSE 'REGULAR' END
           ) AS item
    FROM demo_clubs dc
    CROSS JOIN generate_series(0, 14) AS cols(col_no)
    CROSS JOIN generate_series(0, 6) AS rows(row_no)
    WHERE (col_no < 10 AND row_no <= 6)
       OR (col_no >= 10 AND row_no <= 5)
),
wall_items AS (
    SELECT dc.club_id,
           1000 + row_no AS sort_key,
           jsonb_build_object('type', 'WALL', 'orientation', 'V', 'col', 10, 'row', row_no, 'auto', true) AS item
    FROM demo_clubs dc
    CROSS JOIN generate_series(0, 5) AS rows(row_no)
    UNION ALL
    SELECT dc.club_id,
           1100 + col_no AS sort_key,
           jsonb_build_object('type', 'WALL', 'orientation', 'H', 'col', col_no, 'row', 0, 'auto', true) AS item
    FROM demo_clubs dc
    CROSS JOIN generate_series(0, 14) AS cols(col_no)
),
seat_items AS (
    SELECT s.club_id,
           2000 + s.sort_order AS sort_key,
           jsonb_build_object(
               'type', 'SEAT',
               'seatId', s.id,
               'col',
                   CASE
                       WHEN s.label LIKE 'A%' THEN substring(s.label from 2)::int - 1
                       WHEN s.label LIKE 'B%' THEN substring(s.label from 2)::int - 1
                       WHEN s.label LIKE 'V%' THEN 10 + ((substring(s.label from 2)::int - 1) % 3)
                       ELSE 0
                   END,
               'row',
                   CASE
                       WHEN s.label LIKE 'A%' THEN 1
                       WHEN s.label LIKE 'B%' THEN 4
                       WHEN s.label LIKE 'V%' THEN 1 + ((substring(s.label from 2)::int - 1) / 3) * 2
                       ELSE 0
                   END
           ) AS item
    FROM seats s
    JOIN demo_clubs dc ON dc.club_id = s.club_id
    WHERE s.is_active = true
      AND (s.label LIKE 'A%' OR s.label LIKE 'B%' OR s.label LIKE 'V%')
),
all_items AS (
    SELECT * FROM floor_items
    UNION ALL
    SELECT * FROM wall_items
    UNION ALL
    SELECT * FROM seat_items
)
SELECT club_id,
       jsonb_build_object('items', jsonb_agg(item ORDER BY sort_key)) AS data
FROM all_items
GROUP BY club_id;

UPDATE club_floorplans fp
SET name       = 'Основной зал и VIP-зал',
    width      = 300,
    height     = 180,
    grid_size  = 20,
    version    = fp.version + 1,
    data       = d.data,
    updated_at = current_timestamp
FROM demo_floorplan_data d
WHERE fp.club_id = d.club_id
  AND fp.status = 'PUBLISHED';

INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT d.club_id, 'Основной зал и VIP-зал', 300, 180, 20, 'PUBLISHED', 1, d.data, current_timestamp, current_timestamp
FROM demo_floorplan_data d
WHERE NOT EXISTS (
    SELECT 1
    FROM club_floorplans fp
    WHERE fp.club_id = d.club_id AND fp.status = 'PUBLISHED'
);

CREATE TEMP TABLE demo_purchase_plan (
    code              text primary key,
    user_phone        text,
    club_name         text,
    created_at        timestamp,
    payment_status    text,
    booking_total_rub int,
    purchase_id       bigint
) ON COMMIT DROP;

INSERT INTO demo_purchase_plan (code, user_phone, club_name, created_at, payment_status, booking_total_rub) VALUES
('P001', '+79030000003', 'Cyber Arena Москва', localtimestamp - interval '1 hour',  'PAID',     480),
('P002', '+79030000004', 'GameZone Санкт-Петербург', localtimestamp - interval '55 minutes', 'CREATED', 360),
('P003', '+79030000005', 'Pixel Club Казань', localtimestamp - interval '50 minutes', 'PAID', 500),
('P004', '+79030000006', 'NightByte Екатеринбург', localtimestamp - interval '45 minutes', 'PAID', 480),
('P005', '+79030000007', 'Hardcore Gaming Новосибирск', localtimestamp - interval '40 minutes', 'PAID', 500),
('P006', '+79030000003', 'Cyber Arena Москва', localtimestamp - interval '2 hours', 'CREATED', 240),
('P007', '+79030000008', 'GameZone Санкт-Петербург', localtimestamp - interval '3 hours', 'PAID', 500),
('P008', '+79030000014', 'Respawn Center Нижний Новгород', localtimestamp - interval '4 hours', 'CREATED', 240),
('P009', '+79030000003', 'Cyber Arena Москва', localtimestamp - interval '3 days', 'PAID', 720),
('P010', '+79030000004', 'GameZone Санкт-Петербург', localtimestamp - interval '4 days', 'PAID', 250),
('P011', '+79030000005', 'Pixel Club Казань', localtimestamp - interval '5 days', 'PAID', 240),
('P012', '+79030000006', 'NightByte Екатеринбург', localtimestamp - interval '6 days', 'PAID', 480),
('P013', '+79030000007', 'Hardcore Gaming Новосибирск', localtimestamp - interval '7 days', 'REFUND', 500),
('P014', '+79030000009', 'Cyber Arena Москва', localtimestamp - interval '8 hours', 'CANCELED', 240),
('P015', '+79030000010', 'Pixel Club Казань', localtimestamp - interval '9 hours', 'CANCELED', 250),
('P016', '+79030000015', 'Respawn Center Нижний Новгород', localtimestamp - interval '10 hours', 'CANCELED', 480),
('P017', '+79030000008', 'GameZone Санкт-Петербург', localtimestamp - interval '2 days', 'CANCELED', 360),
('P018', '+79030000011', 'Respawn Center Нижний Новгород', localtimestamp - interval '2 days 2 hours', 'PAID', 480),
('P019', '+79030000012', 'NightByte Екатеринбург', localtimestamp - interval '30 minutes', 'CREATED', 250),
('P020', '+79030000013', 'Pixel Club Казань', localtimestamp - interval '10 days', 'PAID', 240),
('P021', '+79030000003', 'Cyber Arena Москва', localtimestamp - interval '1 day 2 hours', 'PAID', 0),
('P022', '+79030000004', 'GameZone Санкт-Петербург', localtimestamp - interval '1 day 1 hour', 'CREATED', 0),
('P023', '+79030000005', 'Hardcore Gaming Новосибирск', localtimestamp - interval '12 hours', 'PAID', 0),
('P024', '+79030000006', 'NightByte Екатеринбург', localtimestamp - interval '11 hours', 'PAID', 0),
('P025', '+79030000008', 'Cyber Arena Москва', localtimestamp - interval '6 hours', 'FAILED', 0);

INSERT INTO purchases (user_id, club_id, created_at, booking_total_rub, products_total_rub, total_rub, payment_status)
SELECT u.id, c.id, d.created_at, d.booking_total_rub, 0, d.booking_total_rub, d.payment_status
FROM demo_purchase_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name;

UPDATE demo_purchase_plan d
SET purchase_id = p.id
FROM users u, clubs c, purchases p
WHERE u.phone = d.user_phone
  AND c.name = d.club_name
  AND p.user_id = u.id
  AND p.club_id = c.id
  AND p.created_at = d.created_at
  AND p.payment_status = d.payment_status
  AND p.booking_total_rub = d.booking_total_rub;

CREATE TEMP TABLE demo_order_item_plan (
    purchase_code text,
    product_title text,
    qty           int
) ON COMMIT DROP;

INSERT INTO demo_order_item_plan (purchase_code, product_title, qty) VALUES
('P001', 'Кола 0.5 л', 2),
('P001', 'Чипсы 140 г', 1),
('P002', 'Энергетик Drive 0.45 л', 1),
('P003', 'Пицца пепперони', 1),
('P003', 'Вода негазированная 0.5 л', 2),
('P005', 'Аренда игровых наушников', 1),
('P007', 'Бургер с говядиной', 1),
('P007', 'Морс клюквенный 0.3 л', 1),
('P009', 'Кола Zero 0.5 л', 2),
('P009', 'Наггетсы 8 шт', 1),
('P009', 'Арахис соленый 90 г', 1),
('P011', 'Чипсы 140 г', 1),
('P011', 'Вода негазированная 0.5 л', 1),
('P018', 'Сэндвич с курицей', 1),
('P018', 'Энергетик Drive 0.45 л', 1),
('P018', 'Игровой коврик', 1),
('P019', 'Энергетик Adrenaline 0.449 л', 1),
('P021', 'Пицца маргарита', 1),
('P021', 'Кола 0.5 л', 2),
('P021', 'Попкорн сырный', 1),
('P022', 'Сухарики 90 г', 2),
('P022', 'Вода негазированная 0.5 л', 1),
('P023', 'Аренда геймпада', 1),
('P023', 'Бургер с говядиной', 1),
('P024', 'Хот-дог', 2),
('P024', 'Морс клюквенный 0.3 л', 2),
('P025', 'Аренда веб-камеры', 1),
('P025', 'Кола Zero 0.5 л', 1);

INSERT INTO product_orders (purchase_id, user_id, club_id, created_at, total_rub_snapshot)
SELECT d.purchase_id, u.id, c.id, d.created_at, 0
FROM demo_purchase_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name
WHERE EXISTS (SELECT 1 FROM demo_order_item_plan i WHERE i.purchase_code = d.code);

INSERT INTO product_order_items (product_order_id, product_id, title_snapshot, price_rub_snapshot, qty)
SELECT po.id, p.id, p.title, cp.price_rub, i.qty
FROM demo_order_item_plan i
JOIN demo_purchase_plan d ON d.code = i.purchase_code
JOIN product_orders po ON po.purchase_id = d.purchase_id
JOIN (
    SELECT title, MIN(id) AS id
    FROM products
    GROUP BY title
) product_key ON product_key.title = i.product_title
JOIN products p ON p.id = product_key.id
JOIN clubs c ON c.name = d.club_name
JOIN club_products cp ON cp.club_id = c.id AND cp.product_id = p.id;

WITH order_totals AS (
    SELECT po.id AS order_id, SUM(i.price_rub_snapshot * i.qty)::int AS total_rub
    FROM product_orders po
    JOIN product_order_items i ON i.product_order_id = po.id
    WHERE po.purchase_id IN (SELECT purchase_id FROM demo_purchase_plan)
    GROUP BY po.id
)
UPDATE product_orders po
SET total_rub_snapshot = t.total_rub
FROM order_totals t
WHERE po.id = t.order_id;

WITH product_totals AS (
    SELECT po.purchase_id, SUM(i.price_rub_snapshot * i.qty)::int AS products_total
    FROM product_orders po
    JOIN product_order_items i ON i.product_order_id = po.id
    WHERE po.purchase_id IN (SELECT purchase_id FROM demo_purchase_plan)
    GROUP BY po.purchase_id
)
UPDATE purchases p
SET products_total_rub = t.products_total,
    total_rub          = p.booking_total_rub + t.products_total
FROM product_totals t
WHERE p.id = t.purchase_id;

CREATE TEMP TABLE demo_booking_plan (
    code               text primary key,
    purchase_code      text,
    user_phone         text,
    club_name          text,
    seat_labels        text[],
    start_at           timestamp,
    end_at             timestamp,
    package_hours      int,
    rate_rub_per_hour  int,
    total_rub          int,
    status             text,
    booking_id         bigint
) ON COMMIT DROP;

INSERT INTO demo_booking_plan (code, purchase_code, user_phone, club_name, seat_labels, start_at, end_at, package_hours, rate_rub_per_hour, total_rub, status) VALUES
('B001', 'P001', '+79030000003', 'Cyber Arena Москва', ARRAY['A1','A2'], localtimestamp - interval '30 minutes', localtimestamp + interval '90 minutes', 2, 120, 480, 'ACTIVE'),
('B002', 'P002', '+79030000004', 'GameZone Санкт-Петербург', ARRAY['A1'], localtimestamp - interval '30 minutes', localtimestamp + interval '150 minutes', 3, 120, 360, 'ACTIVE'),
('B003', 'P003', '+79030000005', 'Pixel Club Казань', ARRAY['V1'], localtimestamp - interval '20 minutes', localtimestamp + interval '100 minutes', 2, 250, 500, 'ACTIVE'),
('B004', 'P004', '+79030000006', 'NightByte Екатеринбург', ARRAY['B1','B2'], localtimestamp - interval '15 minutes', localtimestamp + interval '105 minutes', 2, 120, 480, 'ACTIVE'),
('B005', 'P005', '+79030000007', 'Hardcore Gaming Новосибирск', ARRAY['V1'], localtimestamp - interval '10 minutes', localtimestamp + interval '110 minutes', 2, 250, 500, 'ACTIVE'),
('B006', 'P006', '+79030000003', 'Cyber Arena Москва', ARRAY['A3'], date_trunc('day', localtimestamp) + interval '1 day' + interval '18 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '20 hours', 2, 120, 240, 'UPCOMING'),
('B007', 'P007', '+79030000008', 'GameZone Санкт-Петербург', ARRAY['V2'], date_trunc('day', localtimestamp) + interval '1 day' + interval '21 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '23 hours', 2, 250, 500, 'UPCOMING'),
('B008', 'P008', '+79030000014', 'Respawn Center Нижний Новгород', ARRAY['A1'], date_trunc('day', localtimestamp) + interval '2 days' + interval '16 hours', date_trunc('day', localtimestamp) + interval '2 days' + interval '18 hours', 2, 120, 240, 'UPCOMING'),
('B009', 'P009', '+79030000003', 'Cyber Arena Москва', ARRAY['A4','A5','A6'], date_trunc('day', localtimestamp) - interval '3 days' + interval '18 hours', date_trunc('day', localtimestamp) - interval '3 days' + interval '20 hours', 2, 120, 720, 'DONE'),
('B010', 'P010', '+79030000004', 'GameZone Санкт-Петербург', ARRAY['V3'], date_trunc('day', localtimestamp) - interval '4 days' + interval '19 hours', date_trunc('day', localtimestamp) - interval '4 days' + interval '20 hours', 1, 250, 250, 'DONE'),
('B011', 'P011', '+79030000005', 'Pixel Club Казань', ARRAY['B3','B4'], date_trunc('day', localtimestamp) - interval '5 days' + interval '17 hours', date_trunc('day', localtimestamp) - interval '5 days' + interval '18 hours', 1, 120, 240, 'DONE'),
('B012', 'P012', '+79030000006', 'NightByte Екатеринбург', ARRAY['A7','A8'], date_trunc('day', localtimestamp) - interval '6 days' + interval '20 hours', date_trunc('day', localtimestamp) - interval '6 days' + interval '22 hours', 2, 120, 480, 'DONE'),
('B013', 'P013', '+79030000007', 'Hardcore Gaming Новосибирск', ARRAY['V4'], date_trunc('day', localtimestamp) - interval '7 days' + interval '21 hours', date_trunc('day', localtimestamp) - interval '7 days' + interval '23 hours', 2, 250, 500, 'DONE'),
('B014', 'P014', '+79030000009', 'Cyber Arena Москва', ARRAY['A9'], date_trunc('day', localtimestamp) + interval '1 day' + interval '12 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '14 hours', 2, 120, 240, 'CANCELED'),
('B015', 'P015', '+79030000010', 'Pixel Club Казань', ARRAY['V5'], date_trunc('day', localtimestamp) - interval '1 day' + interval '15 hours', date_trunc('day', localtimestamp) - interval '1 day' + interval '16 hours', 1, 250, 250, 'CANCELED'),
('B016', 'P016', '+79030000015', 'Respawn Center Нижний Новгород', ARRAY['B5','B6'], date_trunc('day', localtimestamp) + interval '3 days' + interval '18 hours', date_trunc('day', localtimestamp) + interval '3 days' + interval '20 hours', 2, 120, 480, 'CANCELED'),
('B017', 'P017', '+79030000008', 'GameZone Санкт-Петербург', ARRAY['A2'], date_trunc('day', localtimestamp) - interval '2 days' + interval '13 hours', date_trunc('day', localtimestamp) - interval '2 days' + interval '16 hours', 3, 120, 360, 'CANCELED'),
('B018', 'P018', '+79030000011', 'Respawn Center Нижний Новгород', ARRAY['A2','A3'], date_trunc('day', localtimestamp) - interval '2 days' + interval '18 hours', date_trunc('day', localtimestamp) - interval '2 days' + interval '20 hours', 2, 120, 480, 'DONE'),
('B019', 'P019', '+79030000012', 'NightByte Екатеринбург', ARRAY['V2'], date_trunc('day', localtimestamp) + interval '1 day' + interval '22 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '23 hours', 1, 250, 250, 'UPCOMING'),
('B020', 'P020', '+79030000013', 'Pixel Club Казань', ARRAY['B5','B6'], date_trunc('day', localtimestamp) - interval '10 days' + interval '16 hours', date_trunc('day', localtimestamp) - interval '10 days' + interval '17 hours', 1, 120, 240, 'DONE');

INSERT INTO bookings (user_id, club_id, start_at, end_at, package_hours, rate_rub_per_hour_snapshot, total_rub_snapshot, status, purchase_id, created_at, updated_at)
SELECT u.id,
       c.id,
       d.start_at,
       d.end_at,
       d.package_hours,
       d.rate_rub_per_hour,
       d.total_rub,
       d.status,
       pp.purchase_id,
       COALESCE(pp.created_at, localtimestamp),
       localtimestamp
FROM demo_booking_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name
LEFT JOIN demo_purchase_plan pp ON pp.code = d.purchase_code;

UPDATE demo_booking_plan d
SET booking_id = b.id
FROM users u, clubs c, bookings b
WHERE u.phone = d.user_phone
  AND c.name = d.club_name
  AND b.user_id = u.id
  AND b.club_id = c.id
  AND b.start_at = d.start_at
  AND b.end_at = d.end_at
  AND b.status = d.status
  AND b.total_rub_snapshot = d.total_rub;

INSERT INTO booking_seats (booking_id, seat_id)
SELECT d.booking_id, s.id
FROM demo_booking_plan d
JOIN clubs c ON c.name = d.club_name
JOIN LATERAL unnest(d.seat_labels) AS seat_label(label) ON true
JOIN seats s ON s.club_id = c.id AND s.label = seat_label.label
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE demo_cart_plan (
    user_phone text,
    club_name  text
) ON COMMIT DROP;

INSERT INTO demo_cart_plan (user_phone, club_name) VALUES
('+79030000012', 'Cyber Arena Москва'),
('+79030000013', 'GameZone Санкт-Петербург'),
('+79030000014', 'Pixel Club Казань'),
('+79030000015', 'Respawn Center Нижний Новгород');

INSERT INTO carts (user_id, club_id, updated_at)
SELECT u.id, c.id, localtimestamp - interval '12 minutes'
FROM demo_cart_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name
ON CONFLICT (user_id, club_id) DO UPDATE
SET updated_at = excluded.updated_at;

DELETE FROM cart_booking_seats cbs
USING cart_booking_lines cbl, carts ca, demo_cart_plan d, users u, clubs c
WHERE cbs.cart_booking_line_id = cbl.id
  AND cbl.cart_id = ca.id
  AND ca.user_id = u.id
  AND ca.club_id = c.id
  AND u.phone = d.user_phone
  AND c.name = d.club_name;

DELETE FROM cart_booking_lines cbl
USING carts ca, demo_cart_plan d, users u, clubs c
WHERE cbl.cart_id = ca.id
  AND ca.user_id = u.id
  AND ca.club_id = c.id
  AND u.phone = d.user_phone
  AND c.name = d.club_name;

DELETE FROM cart_product_lines cpl
USING carts ca, demo_cart_plan d, users u, clubs c
WHERE cpl.cart_id = ca.id
  AND ca.user_id = u.id
  AND ca.club_id = c.id
  AND u.phone = d.user_phone
  AND c.name = d.club_name;

CREATE TEMP TABLE demo_cart_booking_plan (
    user_phone    text,
    club_name     text,
    seat_labels   text[],
    start_at      timestamp,
    end_at        timestamp,
    package_hours int,
    line_id       bigint
) ON COMMIT DROP;

INSERT INTO demo_cart_booking_plan (user_phone, club_name, seat_labels, start_at, end_at, package_hours) VALUES
('+79030000012', 'Cyber Arena Москва', ARRAY['B5','B6'], date_trunc('day', localtimestamp) + interval '1 day' + interval '14 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '16 hours', 2),
('+79030000014', 'Pixel Club Казань', ARRAY['V3'], date_trunc('day', localtimestamp) + interval '1 day' + interval '19 hours', date_trunc('day', localtimestamp) + interval '1 day' + interval '21 hours', 2),
('+79030000015', 'Respawn Center Нижний Новгород', ARRAY['A4'], date_trunc('day', localtimestamp) + interval '2 days' + interval '15 hours', date_trunc('day', localtimestamp) + interval '2 days' + interval '17 hours', 2);

INSERT INTO cart_booking_lines (cart_id, start_at, end_at, package_hours, created_at)
SELECT ca.id, d.start_at, d.end_at, d.package_hours, localtimestamp - interval '12 minutes'
FROM demo_cart_booking_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name
JOIN carts ca ON ca.user_id = u.id AND ca.club_id = c.id;

UPDATE demo_cart_booking_plan d
SET line_id = cbl.id
FROM users u, clubs c, carts ca, cart_booking_lines cbl
WHERE u.phone = d.user_phone
  AND c.name = d.club_name
  AND ca.user_id = u.id
  AND ca.club_id = c.id
  AND cbl.cart_id = ca.id
  AND cbl.start_at = d.start_at
  AND cbl.end_at = d.end_at;

INSERT INTO cart_booking_seats (cart_booking_line_id, seat_id)
SELECT d.line_id, s.id
FROM demo_cart_booking_plan d
JOIN clubs c ON c.name = d.club_name
JOIN LATERAL unnest(d.seat_labels) AS seat_label(label) ON true
JOIN seats s ON s.club_id = c.id AND s.label = seat_label.label
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE demo_cart_product_plan (
    user_phone    text,
    club_name     text,
    product_title text,
    qty           int
) ON COMMIT DROP;

INSERT INTO demo_cart_product_plan (user_phone, club_name, product_title, qty) VALUES
('+79030000012', 'Cyber Arena Москва', 'Кола 0.5 л', 2),
('+79030000012', 'Cyber Arena Москва', 'Наггетсы 8 шт', 1),
('+79030000013', 'GameZone Санкт-Петербург', 'Энергетик Drive 0.45 л', 1),
('+79030000013', 'GameZone Санкт-Петербург', 'Сухарики 90 г', 2),
('+79030000014', 'Pixel Club Казань', 'Пицца пепперони', 1),
('+79030000015', 'Respawn Center Нижний Новгород', 'Аренда геймпада', 1),
('+79030000015', 'Respawn Center Нижний Новгород', 'Вода негазированная 0.5 л', 2);

INSERT INTO cart_product_lines (cart_id, product_id, qty, price_rub_snapshot, title_snapshot)
SELECT ca.id, p.id, d.qty, cp.price_rub, p.title
FROM demo_cart_product_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name
JOIN carts ca ON ca.user_id = u.id AND ca.club_id = c.id
JOIN (
    SELECT title, MIN(id) AS id
    FROM products
    GROUP BY title
) product_key ON product_key.title = d.product_title
JOIN products p ON p.id = product_key.id
JOIN club_products cp ON cp.club_id = c.id AND cp.product_id = p.id;

WITH staff_rows(club_name, phone, role, added_by_phone) AS (
    VALUES
        ('Cyber Arena Москва', '+79030000016', 'OWNER', '+79030000002'),
        ('Cyber Arena Москва', '+79030000022', 'ADMIN', '+79030000016'),
        ('Cyber Arena Москва', '+79030000023', 'ADMIN', '+79030000016'),
        ('GameZone Санкт-Петербург', '+79030000017', 'OWNER', '+79030000002'),
        ('GameZone Санкт-Петербург', '+79030000023', 'ADMIN', '+79030000017'),
        ('GameZone Санкт-Петербург', '+79030000024', 'ADMIN', '+79030000017'),
        ('Pixel Club Казань', '+79030000018', 'OWNER', '+79030000002'),
        ('Pixel Club Казань', '+79030000024', 'ADMIN', '+79030000018'),
        ('Pixel Club Казань', '+79030000022', 'ADMIN', '+79030000018'),
        ('NightByte Екатеринбург', '+79030000019', 'OWNER', '+79030000002'),
        ('NightByte Екатеринбург', '+79030000011', 'ADMIN', '+79030000019'),
        ('NightByte Екатеринбург', '+79030000012', 'ADMIN', '+79030000019'),
        ('Hardcore Gaming Новосибирск', '+79030000020', 'OWNER', '+79030000002'),
        ('Hardcore Gaming Новосибирск', '+79030000013', 'ADMIN', '+79030000020'),
        ('Hardcore Gaming Новосибирск', '+79030000022', 'ADMIN', '+79030000020'),
        ('Respawn Center Нижний Новгород', '+79030000021', 'OWNER', '+79030000002'),
        ('Respawn Center Нижний Новгород', '+79030000023', 'ADMIN', '+79030000021'),
        ('Respawn Center Нижний Новгород', '+79030000024', 'ADMIN', '+79030000021')
)
INSERT INTO club_staff (club_id, user_id, role, added_by_user_id, created_at, updated_at)
SELECT c.id, u.id, s.role, added_by.id, localtimestamp - interval '20 days', localtimestamp
FROM staff_rows s
JOIN clubs c ON c.name = s.club_name
JOIN users u ON u.phone = s.phone
LEFT JOIN users added_by ON added_by.phone = s.added_by_phone
ON CONFLICT (club_id, user_id) DO UPDATE
SET role             = excluded.role,
    added_by_user_id = excluded.added_by_user_id,
    updated_at       = localtimestamp;

WITH overrides(club_name, phone, permission, granted) AS (
    VALUES
        ('Cyber Arena Москва', '+79030000022', 'CLUB_AUDIT_VIEW', true),
        ('Cyber Arena Москва', '+79030000023', 'CLUB_USER_BLOCKS_MANAGE', false),
        ('GameZone Санкт-Петербург', '+79030000024', 'CLUB_CATALOG_MANAGE', false),
        ('Pixel Club Казань', '+79030000022', 'CLUB_FLOORPLANS_MANAGE', false),
        ('NightByte Екатеринбург', '+79030000011', 'CLUB_AUDIT_VIEW', true),
        ('NightByte Екатеринбург', '+79030000012', 'CLUB_USER_BLOCKS_MANAGE', false),
        ('Hardcore Gaming Новосибирск', '+79030000013', 'CLUB_REPORTS_VIEW', false),
        ('Respawn Center Нижний Новгород', '+79030000024', 'CLUB_SEATS_MANAGE', false)
)
INSERT INTO club_user_permission_overrides (club_id, user_id, permission, granted, created_at, updated_at)
SELECT c.id, u.id, o.permission, o.granted, localtimestamp - interval '12 days', localtimestamp
FROM overrides o
JOIN clubs c ON c.name = o.club_name
JOIN users u ON u.phone = o.phone
ON CONFLICT (club_id, user_id, permission) DO UPDATE
SET granted    = excluded.granted,
    updated_at = localtimestamp;

WITH reports(club_name, phone, message, status, created_at) AS (
    VALUES
        ('Cyber Arena Москва', '+79030000003', 'На месте A4 периодически пропадал звук в наушниках, администратор заменил гарнитуру через 10 минут.', 'RESOLVED', localtimestamp - interval '9 days'),
        ('Cyber Arena Москва', '+79030000009', 'Не работал USB-порт на передней панели системного блока.', 'IN_PROGRESS', localtimestamp - interval '2 days'),
        ('GameZone Санкт-Петербург', '+79030000004', 'Долгое ожидание администратора у стойки вечером в пятницу.', 'NEW', localtimestamp - interval '5 hours'),
        ('GameZone Санкт-Петербург', '+79030000008', 'На VIP-месте V2 был залипающий пробел на клавиатуре.', 'RESOLVED', localtimestamp - interval '4 days'),
        ('Pixel Club Казань', '+79030000005', 'В зоне B было грязно после предыдущей компании.', 'IN_PROGRESS', localtimestamp - interval '1 day'),
        ('NightByte Екатеринбург', '+79030000006', 'Ночной пакет оформился, но напиток из заказа выдали не сразу.', 'RESOLVED', localtimestamp - interval '3 days'),
        ('Hardcore Gaming Новосибирск', '+79030000007', 'На V1 проседал FPS в игре, нужна проверка драйверов.', 'NEW', localtimestamp - interval '6 hours'),
        ('Respawn Center Нижний Новгород', '+79030000014', 'На сайте не сразу было понятно, где выбрать места для брони.', 'NEW', localtimestamp - interval '8 hours'),
        ('Respawn Center Нижний Новгород', '+79030000015', 'Отмененное бронирование осталось в истории, хочу уточнить статус оплаты.', 'IN_PROGRESS', localtimestamp - interval '10 hours'),
        ('Pixel Club Казань', '+79030000013', 'Администратор помог перенести заказ, вопрос решен.', 'RESOLVED', localtimestamp - interval '11 days')
)
INSERT INTO club_user_reports (club_id, user_id, message, status, created_at)
SELECT c.id, u.id, r.message, r.status, r.created_at
FROM reports r
JOIN clubs c ON c.name = r.club_name
JOIN users u ON u.phone = r.phone
WHERE NOT EXISTS (
    SELECT 1
    FROM club_user_reports existing
    WHERE existing.club_id = c.id
      AND existing.user_id = u.id
      AND existing.message = r.message
);

WITH blocks(club_name, phone, reason, blocked_until, blocked_by_phone) AS (
    VALUES
        ('Cyber Arena Москва', '+79030000009', 'Нарушение правил клуба: установка стороннего ПО без согласования.', localtimestamp + interval '14 days', '+79030000016'),
        ('Pixel Club Казань', '+79030000010', 'Агрессивное поведение в зале и конфликт с администратором.', localtimestamp + interval '30 days', '+79030000018'),
        ('NightByte Екатеринбург', '+79030000015', 'Порча оборудования: поврежден кабель гарнитуры.', localtimestamp + interval '21 days', '+79030000019'),
        ('Hardcore Gaming Новосибирск', '+79030000008', 'Повторное нарушение правил ночного тарифа.', null::timestamp, '+79030000020')
)
INSERT INTO club_user_blocks (club_id, user_id, is_blocked, reason, blocked_until, blocked_by_user_id, created_at, updated_at)
SELECT c.id, u.id, true, b.reason, b.blocked_until, blocker.id, localtimestamp - interval '1 day', localtimestamp
FROM blocks b
JOIN clubs c ON c.name = b.club_name
JOIN users u ON u.phone = b.phone
LEFT JOIN users blocker ON blocker.phone = b.blocked_by_phone
ON CONFLICT (club_id, user_id) DO UPDATE
SET is_blocked        = true,
    reason            = excluded.reason,
    blocked_until     = excluded.blocked_until,
    blocked_by_user_id = excluded.blocked_by_user_id,
    updated_at        = localtimestamp;

WITH favorites(phone, club_name) AS (
    VALUES
        ('+79030000003', 'Cyber Arena Москва'), ('+79030000003', 'Pixel Club Казань'), ('+79030000003', 'NightByte Екатеринбург'),
        ('+79030000004', 'GameZone Санкт-Петербург'), ('+79030000004', 'Cyber Arena Москва'),
        ('+79030000005', 'Pixel Club Казань'), ('+79030000005', 'Hardcore Gaming Новосибирск'), ('+79030000005', 'Respawn Center Нижний Новгород'),
        ('+79030000006', 'NightByte Екатеринбург'), ('+79030000006', 'GameZone Санкт-Петербург'),
        ('+79030000007', 'Hardcore Gaming Новосибирск'), ('+79030000007', 'Cyber Arena Москва'),
        ('+79030000008', 'GameZone Санкт-Петербург'), ('+79030000008', 'Pixel Club Казань'),
        ('+79030000011', 'Respawn Center Нижний Новгород'), ('+79030000011', 'NightByte Екатеринбург'),
        ('+79030000012', 'NightByte Екатеринбург'), ('+79030000012', 'Cyber Arena Москва'),
        ('+79030000013', 'Pixel Club Казань'), ('+79030000013', 'Hardcore Gaming Новосибирск'),
        ('+79030000014', 'Respawn Center Нижний Новгород'), ('+79030000014', 'GameZone Санкт-Петербург'),
        ('+79030000015', 'Respawn Center Нижний Новгород'), ('+79030000015', 'NightByte Екатеринбург')
)
INSERT INTO user_favorite_clubs (user_id, club_id, created_at)
SELECT u.id, c.id, localtimestamp - interval '6 days'
FROM favorites f
JOIN users u ON u.phone = f.phone
JOIN clubs c ON c.name = f.club_name
ON CONFLICT DO NOTHING;

WITH apps(applicant_phone, club_name, address, location_text, description, status, decision_comment, decided_by_phone, created_club_name, created_at) AS (
    VALUES
        ('+79030000022', 'Cyber Lounge Самара', 'Самара, Московское шоссе, 4', 'Московское шоссе', 'Планируется клуб на 18 мест рядом с университетским корпусом.', 'PENDING', null, null, null, localtimestamp - interval '2 days'),
        ('+79030000023', 'Level Up Краснодар', 'Краснодар, ул. Красная, 109', 'Центр Краснодара', 'Клуб с отдельной комнатой для тренировок команд и мини-баром.', 'REVISION_REQUESTED', 'Добавьте договор аренды помещения и фотографии входной группы.', '+79030000002', null, localtimestamp - interval '7 days'),
        ('+79030000024', 'SteamHall Воронеж', 'Воронеж, пр-т Революции, 38', 'Проспект Революции', 'Небольшой клуб на 10 мест в подвальном помещении.', 'REJECTED', 'Помещение не соответствует требованиям по вентиляции и эвакуационному выходу.', '+79030000002', null, localtimestamp - interval '12 days'),
        ('+79030000018', 'Pixel Club Казань', 'Казань, ул. Баумана, 51', 'Центр Казани', 'Заявка одобрена, клуб создан на платформе.', 'APPROVED', 'Документы проверены, клуб опубликован.', '+79030000002', 'Pixel Club Казань', localtimestamp - interval '25 days'),
        ('+79030000021', 'Arena Lite Пермь', 'Пермь, Комсомольский проспект, 33', 'Комсомольский проспект', 'Черновик заявки на клуб с 12 стандартными местами.', 'DRAFT', null, null, null, localtimestamp - interval '1 day')
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
      AND existing.club_name = a.club_name
      AND existing.status = a.status
)
ON CONFLICT DO NOTHING;

WITH warnings(club_name, message, created_by_phone, created_at) AS (
    VALUES
        ('Cyber Arena Москва', 'Проверьте актуальность документов по пожарной безопасности до конца месяца.', '+79030000002', localtimestamp - interval '9 days'),
        ('NightByte Екатеринбург', 'Поступило несколько обращений по ночным заказам, требуется ответ владельца.', '+79030000002', localtimestamp - interval '3 days'),
        ('Hardcore Gaming Новосибирск', 'Рекомендуется обновить фотографии VIP-зоны в карточке клуба.', '+79030000002', localtimestamp - interval '5 days')
)
INSERT INTO club_warning (club_id, message, created_by, created_at)
SELECT c.id, w.message, u.id, w.created_at
FROM warnings w
JOIN clubs c ON c.name = w.club_name
JOIN users u ON u.phone = w.created_by_phone
WHERE NOT EXISTS (
    SELECT 1
    FROM club_warning existing
    WHERE existing.club_id = c.id AND existing.message = w.message
);

WITH audit_rows(actor_phone, club_name, action, entity_type, entity_id, before_data, after_data, created_at) AS (
    VALUES
        ('+79030000016', 'Cyber Arena Москва', 'CLUB_PRODUCT_UPSERT', 'ClubProduct', 'cola', '{"priceRub":130}'::jsonb, '{"priceRub":140}'::jsonb, localtimestamp - interval '8 days'),
        ('+79030000016', 'Cyber Arena Москва', 'SEAT_UPDATE', 'Seat', 'A1', '{"isActive":false}'::jsonb, '{"isActive":true}'::jsonb, localtimestamp - interval '7 days'),
        ('+79030000017', 'GameZone Санкт-Петербург', 'TIME_PACKAGE_CREATE', 'ClubTimePackage', 'night', null::jsonb, '{"name":"Ночной пакет (23:00-08:00)"}'::jsonb, localtimestamp - interval '6 days'),
        ('+79030000018', 'Pixel Club Казань', 'FLOORPLAN_PUBLISH', 'ClubFloorplan', 'main', '{"status":"DRAFT"}'::jsonb, '{"status":"PUBLISHED"}'::jsonb, localtimestamp - interval '5 days'),
        ('+79030000019', 'NightByte Екатеринбург', 'REPORT_STATUS_UPDATE', 'ClubUserReport', 'night-order', '{"status":"IN_PROGRESS"}'::jsonb, '{"status":"RESOLVED"}'::jsonb, localtimestamp - interval '4 days'),
        ('+79030000020', 'Hardcore Gaming Новосибирск', 'USER_BLOCK_CREATE', 'ClubUserBlock', '+79030000008', null::jsonb, '{"reason":"Повторное нарушение правил ночного тарифа"}'::jsonb, localtimestamp - interval '3 days'),
        ('+79030000021', 'Respawn Center Нижний Новгород', 'CLUB_SETTINGS_UPDATE', 'Club', 'respawn', '{"isActive":false}'::jsonb, '{"isActive":true}'::jsonb, localtimestamp - interval '2 days'),
        ('+79030000022', 'Cyber Arena Москва', 'BOOKING_STATUS_CHANGE', 'Booking', 'B001', '{"status":"UPCOMING"}'::jsonb, '{"status":"ACTIVE"}'::jsonb, localtimestamp - interval '30 minutes'),
        ('+79030000023', 'GameZone Санкт-Петербург', 'PURCHASE_STATUS_CHANGE', 'Purchase', 'P002', '{"paymentStatus":"CREATED"}'::jsonb, '{"paymentStatus":"CREATED"}'::jsonb, localtimestamp - interval '25 minutes'),
        ('+79030000024', 'Pixel Club Казань', 'CLUB_PRODUCT_UPSERT', 'ClubProduct', 'pizza', '{"isAvailable":false}'::jsonb, '{"isAvailable":true}'::jsonb, localtimestamp - interval '20 minutes')
)
INSERT INTO audit_log (actor_user_id, club_id, action, entity_type, entity_id, before_data, after_data, created_at)
SELECT actor.id, c.id, a.action, a.entity_type, a.entity_id, a.before_data, a.after_data, a.created_at
FROM audit_rows a
JOIN users actor ON actor.phone = a.actor_phone
JOIN clubs c ON c.name = a.club_name
WHERE NOT EXISTS (
    SELECT 1
    FROM audit_log existing
    WHERE existing.actor_user_id = actor.id
      AND existing.club_id = c.id
      AND existing.action = a.action
      AND existing.entity_type = a.entity_type
      AND existing.entity_id = a.entity_id
      AND existing.created_at = a.created_at
);
