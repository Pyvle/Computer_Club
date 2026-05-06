UPDATE clubs
SET is_active = true,
    is_blocked = false,
    block_reason = null,
    updated_at = localtimestamp
WHERE name IN ('Пробный Клуб', 'Северный Портал', 'Линия Пикселей');

WITH owner_clubs AS (
    SELECT id, name
    FROM clubs
    WHERE name IN ('Северный Портал', 'Линия Пикселей')
),
extra_seats(label, seat_type, sort_order) AS (
    VALUES
        ('C1', 'REGULAR', 7),
        ('C2', 'REGULAR', 8),
        ('C3', 'REGULAR', 9),
        ('C4', 'REGULAR', 10),
        ('V3', 'VIP', 103),
        ('V4', 'VIP', 104)
)
INSERT INTO seats (club_id, label, type, is_active, sort_order)
SELECT c.id, s.label, s.seat_type, true, s.sort_order
FROM owner_clubs c
CROSS JOIN extra_seats s
ON CONFLICT (club_id, label) DO UPDATE
SET type = excluded.type,
    is_active = true,
    sort_order = excluded.sort_order;

WITH floorplans(club_name, data) AS (
    VALUES
        ('Северный Портал', '{"version":2,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":1000,"h":20},{"id":"wall-south","type":"wall","x":0,"y":620,"w":1000,"h":20},{"id":"desk","type":"furniture","x":40,"y":470,"w":160,"h":70,"label":"Стойка"},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":80,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":80,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":270,"y":80,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":360,"y":80,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":210,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":210,"w":70,"h":70},{"id":"seat-c1","type":"seat","seatLabel":"C1","x":90,"y":340,"w":70,"h":70},{"id":"seat-c2","type":"seat","seatLabel":"C2","x":180,"y":340,"w":70,"h":70},{"id":"seat-c3","type":"seat","seatLabel":"C3","x":270,"y":340,"w":70,"h":70},{"id":"seat-c4","type":"seat","seatLabel":"C4","x":360,"y":340,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":680,"y":100,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":800,"y":100,"w":90,"h":90},{"id":"seat-v3","type":"seat","seatLabel":"V3","x":680,"y":260,"w":90,"h":90},{"id":"seat-v4","type":"seat","seatLabel":"V4","x":800,"y":260,"w":90,"h":90}]}'::jsonb),
        ('Линия Пикселей', '{"version":2,"items":[{"id":"wall-left","type":"wall","x":0,"y":0,"w":20,"h":620},{"id":"wall-right","type":"wall","x":980,"y":0,"w":20,"h":620},{"id":"desk","type":"furniture","x":740,"y":470,"w":180,"h":70,"label":"Ресепшн"},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":90,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":90,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":270,"y":90,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":360,"y":90,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":220,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":220,"w":70,"h":70},{"id":"seat-c1","type":"seat","seatLabel":"C1","x":90,"y":350,"w":70,"h":70},{"id":"seat-c2","type":"seat","seatLabel":"C2","x":180,"y":350,"w":70,"h":70},{"id":"seat-c3","type":"seat","seatLabel":"C3","x":270,"y":350,"w":70,"h":70},{"id":"seat-c4","type":"seat","seatLabel":"C4","x":360,"y":350,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":650,"y":90,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":780,"y":90,"w":90,"h":90},{"id":"seat-v3","type":"seat","seatLabel":"V3","x":650,"y":250,"w":90,"h":90},{"id":"seat-v4","type":"seat","seatLabel":"V4","x":780,"y":250,"w":90,"h":90}]}'::jsonb)
)
UPDATE club_floorplans fp
SET width = 1000,
    height = 640,
    grid_size = 10,
    status = 'PUBLISHED',
    version = 2,
    data = f.data,
    updated_at = localtimestamp
FROM floorplans f
JOIN clubs c ON c.name = f.club_name
WHERE fp.club_id = c.id
  AND fp.name = 'Основная схема';

WITH floorplans(club_name, data) AS (
    VALUES
        ('Северный Портал', '{"version":2,"items":[]}'::jsonb),
        ('Линия Пикселей', '{"version":2,"items":[]}'::jsonb)
)
INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT c.id, 'Черновик турнира', 1000, 640, 10, 'DRAFT', 1, f.data, localtimestamp - interval '3 days', localtimestamp - interval '1 day'
FROM floorplans f
JOIN clubs c ON c.name = f.club_name
WHERE NOT EXISTS (
    SELECT 1 FROM club_floorplans existing
    WHERE existing.club_id = c.id AND existing.name = 'Черновик турнира'
);

WITH product_prices(club_name, title, price_rub, is_available) AS (
    VALUES
        ('Северный Портал', 'Добрый Кола 0.5 л', 130, true),
        ('Северный Портал', 'Добрый Кола без сахара 0.5 л', 130, true),
        ('Северный Портал', 'Сок яблочный 0.5 л', 120, true),
        ('Северный Портал', 'Вода негазированная 0.5 л', 90, true),
        ('Северный Портал', 'Пицца пепперони', 410, true),
        ('Северный Портал', 'Наггетсы 8 шт', 270, true),
        ('Северный Портал', 'Аренда геймпада', 170, true),
        ('Северный Портал', 'Аренда веб-камеры', 200, false),
        ('Линия Пикселей', 'Добрый Кола 0.5 л', 125, true),
        ('Линия Пикселей', 'Сок апельсиновый 0.5 л', 120, true),
        ('Линия Пикселей', 'Морс клюквенный 0.3 л', 115, true),
        ('Линия Пикселей', 'Чипсы 140 г', 150, true),
        ('Линия Пикселей', 'Бургер с говядиной', 340, true),
        ('Линия Пикселей', 'Попкорн сырный', 145, true),
        ('Линия Пикселей', 'Аренда игровых наушников', 130, true),
        ('Линия Пикселей', 'Игровой коврик', 95, false),
        ('Пробный Клуб', 'Добрый Кола 0.5 л', 120, true),
        ('Пробный Клуб', 'Вода негазированная 0.5 л', 80, true),
        ('Пробный Клуб', 'Чипсы 140 г', 140, true),
        ('Пробный Клуб', 'Хот-дог', 220, true)
)
INSERT INTO club_products (club_id, product_id, price_rub, is_available)
SELECT c.id, p.id, pp.price_rub, pp.is_available
FROM product_prices pp
JOIN clubs c ON c.name = pp.club_name
JOIN products p ON p.title = pp.title
ON CONFLICT (club_id, product_id) DO UPDATE
SET price_rub = excluded.price_rub,
    is_available = excluded.is_available;

CREATE TEMP TABLE demo_owner_purchase_plan (
    code              text primary key,
    user_phone        text,
    club_name         text,
    created_at        timestamp,
    payment_status    text,
    booking_total_rub int,
    purchase_id       bigint
) ON COMMIT DROP;

INSERT INTO demo_owner_purchase_plan (code, user_phone, club_name, created_at, payment_status, booking_total_rub) VALUES
('O001', '+79030000003', 'Северный Портал', localtimestamp - interval '18 days', 'PAID', 720),
('O002', '+79030000004', 'Северный Портал', localtimestamp - interval '12 days', 'PAID', 500),
('O003', '+79030000005', 'Северный Портал', localtimestamp - interval '6 days', 'PAID', 1440),
('O004', '+79030000006', 'Северный Портал', localtimestamp - interval '2 days', 'CREATED', 240),
('O005', '+79030000007', 'Линия Пикселей', localtimestamp - interval '16 days', 'PAID', 750),
('O006', '+79030000008', 'Линия Пикселей', localtimestamp - interval '9 days', 'REFUND', 500),
('O007', '+79030000009', 'Линия Пикселей', localtimestamp - interval '3 days', 'PAID', 360),
('O008', '+79030000010', 'Пробный Клуб', localtimestamp - interval '5 days', 'PAID', 240),
('O009', '+79030000011', 'Пробный Клуб', localtimestamp - interval '1 day', 'FAILED', 250);

INSERT INTO purchases (user_id, club_id, created_at, booking_total_rub, products_total_rub, total_rub, payment_status)
SELECT u.id, c.id, d.created_at, d.booking_total_rub, 0, d.booking_total_rub, d.payment_status
FROM demo_owner_purchase_plan d
JOIN users u ON u.phone = d.user_phone
JOIN clubs c ON c.name = d.club_name;

UPDATE demo_owner_purchase_plan d
SET purchase_id = p.id
FROM users u, clubs c, purchases p
WHERE u.phone = d.user_phone
  AND c.name = d.club_name
  AND p.user_id = u.id
  AND p.club_id = c.id
  AND p.created_at = d.created_at
  AND p.payment_status = d.payment_status
  AND p.booking_total_rub = d.booking_total_rub;

CREATE TEMP TABLE demo_owner_booking_plan (
    code               text primary key,
    purchase_code      text,
    user_phone         text,
    club_name          text,
    seat_labels        text[],
    start_at           timestamp,
    duration_hours     int,
    rate_rub_per_hour  int,
    status             text,
    booking_id         bigint
) ON COMMIT DROP;

INSERT INTO demo_owner_booking_plan (code, purchase_code, user_phone, club_name, seat_labels, start_at, duration_hours, rate_rub_per_hour, status) VALUES
('OB001', 'O001', '+79030000003', 'Северный Портал', ARRAY['A1','A2','A3'], date_trunc('day', localtimestamp) - interval '18 days' + interval '18 hours', 2, 120, 'DONE'),
('OB002', 'O002', '+79030000004', 'Северный Портал', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '12 days' + interval '21 hours', 2, 250, 'DONE'),
('OB003', 'O003', '+79030000005', 'Северный Портал', ARRAY['A1','A2','A3','A4','B1','B2'], date_trunc('day', localtimestamp) - interval '6 days' + interval '17 hours', 2, 120, 'DONE'),
('OB004', 'O004', '+79030000006', 'Северный Портал', ARRAY['C1'], date_trunc('day', localtimestamp) + interval '1 day' + interval '19 hours', 2, 120, 'UPCOMING'),
('OB005', null, '+79030000012', 'Северный Портал', ARRAY['V2','V3'], date_trunc('day', localtimestamp) + interval '4 days' + interval '20 hours', 3, 250, 'UPCOMING'),
('OB006', null, '+79030000013', 'Северный Портал', ARRAY['C2'], date_trunc('day', localtimestamp) - interval '1 day' + interval '16 hours', 1, 120, 'CANCELED'),
('OB007', 'O005', '+79030000007', 'Линия Пикселей', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '16 days' + interval '22 hours', 3, 250, 'DONE'),
('OB008', 'O006', '+79030000008', 'Линия Пикселей', ARRAY['V2'], date_trunc('day', localtimestamp) - interval '9 days' + interval '21 hours', 2, 250, 'CANCELED'),
('OB009', 'O007', '+79030000009', 'Линия Пикселей', ARRAY['B1','B2','C1'], date_trunc('day', localtimestamp) - interval '3 days' + interval '18 hours', 1, 120, 'DONE'),
('OB010', null, '+79030000014', 'Линия Пикселей', ARRAY['A1','A2'], date_trunc('day', localtimestamp) + interval '2 days' + interval '18 hours', 2, 120, 'UPCOMING'),
('OB011', null, '+79030000015', 'Линия Пикселей', ARRAY['V3','V4'], date_trunc('day', localtimestamp) + interval '8 days' + interval '20 hours', 4, 250, 'UPCOMING'),
('OB012', 'O008', '+79030000010', 'Пробный Клуб', ARRAY['1','2'], date_trunc('day', localtimestamp) - interval '5 days' + interval '18 hours', 1, 120, 'DONE'),
('OB013', 'O009', '+79030000011', 'Пробный Клуб', ARRAY['V-1'], date_trunc('day', localtimestamp) + interval '2 days' + interval '21 hours', 1, 250, 'UPCOMING');

INSERT INTO bookings (user_id, club_id, start_at, end_at, package_hours, rate_rub_per_hour_snapshot, total_rub_snapshot, status, purchase_id, created_at, updated_at)
SELECT u.id,
       c.id,
       b.start_at,
       b.start_at + (b.duration_hours || ' hours')::interval,
       b.duration_hours,
       b.rate_rub_per_hour,
       (array_length(b.seat_labels, 1) * b.duration_hours * b.rate_rub_per_hour)::int,
       b.status,
       p.purchase_id,
       b.start_at - interval '2 days',
       localtimestamp
FROM demo_owner_booking_plan b
JOIN users u ON u.phone = b.user_phone
JOIN clubs c ON c.name = b.club_name
LEFT JOIN demo_owner_purchase_plan p ON p.code = b.purchase_code;

UPDATE demo_owner_booking_plan d
SET booking_id = b.id
FROM users u, clubs c, bookings b
WHERE u.phone = d.user_phone
  AND c.name = d.club_name
  AND b.user_id = u.id
  AND b.club_id = c.id
  AND b.start_at = d.start_at
  AND b.end_at = d.start_at + (d.duration_hours || ' hours')::interval;

INSERT INTO booking_seats (booking_id, seat_id)
SELECT d.booking_id, s.id
FROM demo_owner_booking_plan d
JOIN clubs c ON c.name = d.club_name
JOIN LATERAL unnest(d.seat_labels) AS seat_label(label) ON true
JOIN seats s ON s.club_id = c.id AND s.label = seat_label.label
WHERE d.booking_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE TEMP TABLE demo_owner_order_item_plan (
    purchase_code text,
    product_title text,
    qty           int
) ON COMMIT DROP;

INSERT INTO demo_owner_order_item_plan (purchase_code, product_title, qty) VALUES
('O001', 'Добрый Кола 0.5 л', 3),
('O001', 'Наггетсы 8 шт', 1),
('O002', 'Пицца пепперони', 1),
('O002', 'Вода негазированная 0.5 л', 2),
('O003', 'Добрый Кола без сахара 0.5 л', 4),
('O003', 'Аренда геймпада', 2),
('O004', 'Сок яблочный 0.5 л', 1),
('O005', 'Бургер с говядиной', 1),
('O005', 'Морс клюквенный 0.3 л', 1),
('O006', 'Попкорн сырный', 2),
('O007', 'Чипсы 140 г', 2),
('O007', 'Добрый Кола 0.5 л', 2),
('O008', 'Хот-дог', 1),
('O008', 'Вода негазированная 0.5 л', 1),
('O009', 'Добрый Кола 0.5 л', 1);

INSERT INTO product_orders (purchase_id, user_id, club_id, created_at, total_rub_snapshot)
SELECT p.purchase_id, u.id, c.id, p.created_at, 0
FROM demo_owner_purchase_plan p
JOIN users u ON u.phone = p.user_phone
JOIN clubs c ON c.name = p.club_name
WHERE EXISTS (SELECT 1 FROM demo_owner_order_item_plan i WHERE i.purchase_code = p.code);

INSERT INTO product_order_items (product_order_id, product_id, title_snapshot, price_rub_snapshot, qty)
SELECT po.id, pr.id, pr.title, cp.price_rub, i.qty
FROM demo_owner_order_item_plan i
JOIN demo_owner_purchase_plan p ON p.code = i.purchase_code
JOIN product_orders po ON po.purchase_id = p.purchase_id
JOIN products pr ON pr.title = i.product_title
JOIN clubs c ON c.name = p.club_name
JOIN club_products cp ON cp.club_id = c.id AND cp.product_id = pr.id;

WITH order_totals AS (
    SELECT po.id AS order_id, SUM(i.price_rub_snapshot * i.qty)::int AS total_rub
    FROM product_orders po
    JOIN product_order_items i ON i.product_order_id = po.id
    WHERE po.purchase_id IN (SELECT purchase_id FROM demo_owner_purchase_plan)
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
    WHERE po.purchase_id IN (SELECT purchase_id FROM demo_owner_purchase_plan)
    GROUP BY po.purchase_id
)
UPDATE purchases p
SET products_total_rub = COALESCE(t.products_total, 0),
    total_rub = p.booking_total_rub + COALESCE(t.products_total, 0)
FROM product_totals t
WHERE p.id = t.purchase_id;

WITH reports(club_name, phone, message, status, created_at) AS (
    VALUES
        ('Северный Портал', '+79030000003', 'На A2 периодически пропадает звук в наушниках.', 'NEW', localtimestamp - interval '3 hours'),
        ('Северный Портал', '+79030000005', 'После групповой брони не сразу выдали заказ с напитками.', 'IN_PROGRESS', localtimestamp - interval '1 day'),
        ('Северный Портал', '+79030000013', 'Отмененная бронь отображается как активная в уведомлении.', 'RESOLVED', localtimestamp - interval '6 days'),
        ('Линия Пикселей', '+79030000008', 'Прошу уточнить возврат по отмененному VIP-бронированию.', 'IN_PROGRESS', localtimestamp - interval '2 days'),
        ('Линия Пикселей', '+79030000009', 'На B2 залипает клавиша Shift, нужна проверка.', 'NEW', localtimestamp - interval '5 hours'),
        ('Пробный Клуб', '+79030000011', 'Не получилось оплатить будущую бронь картой.', 'NEW', localtimestamp - interval '4 hours')
)
INSERT INTO club_user_reports (club_id, user_id, message, status, created_at)
SELECT c.id, u.id, r.message, r.status, r.created_at
FROM reports r
JOIN clubs c ON c.name = r.club_name
JOIN users u ON u.phone = r.phone
WHERE NOT EXISTS (
    SELECT 1 FROM club_user_reports existing
    WHERE existing.club_id = c.id AND existing.user_id = u.id AND existing.message = r.message
);

WITH blocks(club_name, phone, reason, blocked_until, blocked_by_phone) AS (
    VALUES
        ('Северный Портал', '+79030000013', 'Повторная неявка на бронь без отмены.', localtimestamp + interval '10 days', '+79030000025'),
        ('Линия Пикселей', '+79030000008', 'Спор по возврату до решения администратора.', localtimestamp + interval '3 days', '+79030000026'),
        ('Пробный Клуб', '+79030000015', 'Проверка после жалобы персонала.', null::timestamp, '+79991234568')
)
INSERT INTO club_user_blocks (club_id, user_id, is_blocked, reason, blocked_until, blocked_by_user_id, created_at, updated_at)
SELECT c.id, u.id, true, b.reason, b.blocked_until, blocker.id, localtimestamp - interval '2 days', localtimestamp
FROM blocks b
JOIN clubs c ON c.name = b.club_name
JOIN users u ON u.phone = b.phone
LEFT JOIN users blocker ON blocker.phone = b.blocked_by_phone
ON CONFLICT (club_id, user_id) DO UPDATE
SET is_blocked = true,
    reason = excluded.reason,
    blocked_until = excluded.blocked_until,
    blocked_by_user_id = excluded.blocked_by_user_id,
    updated_at = localtimestamp;

WITH warnings(club_name, message, created_by_phone, created_at) AS (
    VALUES
        ('Северный Портал', 'Демо-предупреждение платформы: проверьте документы по новому залу.', '+79030000002', localtimestamp - interval '4 days'),
        ('Линия Пикселей', 'Демо-предупреждение платформы: обновите фотографии входной группы.', '+79030000002', localtimestamp - interval '2 days')
)
INSERT INTO club_warning (club_id, message, created_by, created_at)
SELECT c.id, w.message, u.id, w.created_at
FROM warnings w
JOIN clubs c ON c.name = w.club_name
JOIN users u ON u.phone = w.created_by_phone
WHERE NOT EXISTS (
    SELECT 1 FROM club_warning existing
    WHERE existing.club_id = c.id AND existing.message = w.message
);

WITH audit_rows(actor_phone, club_name, action, entity_type, entity_id, before_data, after_data, created_at) AS (
    VALUES
        ('+79991234567', 'Северный Портал', 'CLUB_SETTINGS_UPDATE', 'Club', 'settings', '{"isActive":false}'::jsonb, '{"isActive":true}'::jsonb, localtimestamp - interval '9 days'),
        ('+79030000025', 'Северный Портал', 'SEAT_CREATE', 'Seat', 'C1', null::jsonb, '{"label":"C1","type":"REGULAR"}'::jsonb, localtimestamp - interval '8 days'),
        ('+79030000025', 'Северный Портал', 'FLOORPLAN_PUBLISH', 'ClubFloorplan', 'main', '{"version":1}'::jsonb, '{"version":2,"status":"PUBLISHED"}'::jsonb, localtimestamp - interval '7 days'),
        ('+79991234567', 'Северный Портал', 'TIME_PACKAGE_UPDATE', 'ClubTimePackage', 'evening', '{"pricePerHourRub":130}'::jsonb, '{"pricePerHourRub":120}'::jsonb, localtimestamp - interval '6 days'),
        ('+79030000026', 'Линия Пикселей', 'CLUB_PRODUCT_UPSERT', 'ClubProduct', 'burger', '{"isAvailable":false}'::jsonb, '{"isAvailable":true,"priceRub":340}'::jsonb, localtimestamp - interval '5 days'),
        ('+79030000026', 'Линия Пикселей', 'REPORT_STATUS_UPDATE', 'ClubUserReport', 'refund', '{"status":"NEW"}'::jsonb, '{"status":"IN_PROGRESS"}'::jsonb, localtimestamp - interval '2 days'),
        ('+79991234568', 'Пробный Клуб', 'USER_BLOCK_CREATE', 'ClubUserBlock', '+79030000015', null::jsonb, '{"reason":"Проверка после жалобы персонала"}'::jsonb, localtimestamp - interval '1 day')
)
INSERT INTO audit_log (actor_user_id, club_id, action, entity_type, entity_id, before_data, after_data, created_at)
SELECT actor.id, c.id, a.action, a.entity_type, a.entity_id, a.before_data, a.after_data, a.created_at
FROM audit_rows a
JOIN users actor ON actor.phone = a.actor_phone
JOIN clubs c ON c.name = a.club_name
WHERE NOT EXISTS (
    SELECT 1 FROM audit_log existing
    WHERE existing.actor_user_id = actor.id
      AND existing.club_id = c.id
      AND existing.action = a.action
      AND existing.entity_type = a.entity_type
      AND existing.entity_id = a.entity_id
);
