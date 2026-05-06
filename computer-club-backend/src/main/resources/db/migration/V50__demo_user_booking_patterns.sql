CREATE TEMP TABLE demo_user_booking_patterns (
    user_phone        text,
    club_name         text,
    seat_labels       text[],
    start_at          timestamp,
    duration_hours    int,
    rate_rub_per_hour int,
    status            text,
    created_offset    interval,
    booking_id        bigint
) ON COMMIT DROP;

INSERT INTO demo_user_booking_patterns (user_phone, club_name, seat_labels, start_at, duration_hours, rate_rub_per_hour, status, created_offset) VALUES
-- +79030000003: постоянник одного клуба, почти всегда вечером в "Кибер Арена"
('+79030000003', 'Кибер Арена', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '70 days' + interval '19 hours', 2, 120, 'DONE', interval '72 days'),
('+79030000003', 'Кибер Арена', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '56 days' + interval '19 hours', 2, 120, 'DONE', interval '58 days'),
('+79030000003', 'Кибер Арена', ARRAY['A2'], date_trunc('day', localtimestamp) - interval '42 days' + interval '19 hours', 2, 120, 'DONE', interval '44 days'),
('+79030000003', 'Кибер Арена', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '28 days' + interval '19 hours', 2, 120, 'DONE', interval '30 days'),
('+79030000003', 'Кибер Арена', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '14 days' + interval '19 hours', 2, 120, 'DONE', interval '16 days'),
('+79030000003', 'Кибер Арена', ARRAY['A2'], date_trunc('day', localtimestamp) + interval '7 days' + interval '19 hours', 2, 120, 'UPCOMING', interval '1 day'),

-- +79030000004: пробует разные клубы, выбирает ближайший свободный
('+79030000004', 'Кибер Арена', ARRAY['B1'], date_trunc('day', localtimestamp) - interval '63 days' + interval '18 hours', 2, 120, 'DONE', interval '64 days'),
('+79030000004', 'Игровая Зона', ARRAY['B2'], date_trunc('day', localtimestamp) - interval '47 days' + interval '20 hours', 3, 120, 'DONE', interval '48 days'),
('+79030000004', 'Пиксель', ARRAY['A3'], date_trunc('day', localtimestamp) - interval '31 days' + interval '17 hours', 2, 120, 'DONE', interval '32 days'),
('+79030000004', 'Ночной Байт', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '16 days' + interval '21 hours', 2, 250, 'DONE', interval '17 days'),
('+79030000004', 'Возрождение', ARRAY['A4'], date_trunc('day', localtimestamp) + interval '10 days' + interval '18 hours', 2, 120, 'UPCOMING', interval '2 days'),

-- +79030000005: ежемесячный "праздничный" поход компанией
('+79030000005', 'Пиксель', ARRAY['A1','A2','A3','A4'], date_trunc('day', localtimestamp) - interval '120 days' + interval '18 hours', 4, 120, 'DONE', interval '125 days'),
('+79030000005', 'Пиксель', ARRAY['A1','A2','A3','A4'], date_trunc('day', localtimestamp) - interval '90 days' + interval '18 hours', 4, 120, 'DONE', interval '95 days'),
('+79030000005', 'Пиксель', ARRAY['A5','A6','B1','B2'], date_trunc('day', localtimestamp) - interval '60 days' + interval '18 hours', 4, 120, 'DONE', interval '65 days'),
('+79030000005', 'Пиксель', ARRAY['A1','A2','A3','A4'], date_trunc('day', localtimestamp) - interval '30 days' + interval '18 hours', 4, 120, 'DONE', interval '35 days'),
('+79030000005', 'Пиксель', ARRAY['A5','A6','B1','B2'], date_trunc('day', localtimestamp) + interval '12 days' + interval '18 hours', 4, 120, 'UPCOMING', interval '5 days'),

-- +79030000006: короткие дневные брони после учебы
('+79030000006', 'Ночной Байт', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '25 days' + interval '15 hours', 1, 120, 'DONE', interval '26 days'),
('+79030000006', 'Ночной Байт', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '18 days' + interval '15 hours', 1, 120, 'DONE', interval '19 days'),
('+79030000006', 'Ночной Байт', ARRAY['A2'], date_trunc('day', localtimestamp) - interval '11 days' + interval '15 hours', 1, 120, 'DONE', interval '12 days'),
('+79030000006', 'Ночной Байт', ARRAY['A2'], date_trunc('day', localtimestamp) - interval '4 days' + interval '15 hours', 1, 120, 'DONE', interval '5 days'),

-- +79030000007: ночные VIP-марафоны раз в несколько недель
('+79030000007', 'Жесткий Режим', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '84 days' + interval '22 hours', 5, 250, 'DONE', interval '86 days'),
('+79030000007', 'Жесткий Режим', ARRAY['V2'], date_trunc('day', localtimestamp) - interval '55 days' + interval '22 hours', 5, 250, 'DONE', interval '57 days'),
('+79030000007', 'Жесткий Режим', ARRAY['V3'], date_trunc('day', localtimestamp) - interval '26 days' + interval '22 hours', 5, 250, 'DONE', interval '28 days'),
('+79030000007', 'Жесткий Режим', ARRAY['V1'], date_trunc('day', localtimestamp) + interval '15 days' + interval '22 hours', 5, 250, 'UPCOMING', interval '4 days'),

-- +79030000008: часто планирует, но иногда отменяет
('+79030000008', 'Игровая Зона', ARRAY['A3'], date_trunc('day', localtimestamp) - interval '35 days' + interval '18 hours', 2, 120, 'CANCELED', interval '36 days'),
('+79030000008', 'Игровая Зона', ARRAY['A4'], date_trunc('day', localtimestamp) - interval '21 days' + interval '18 hours', 2, 120, 'DONE', interval '22 days'),
('+79030000008', 'Игровая Зона', ARRAY['A5'], date_trunc('day', localtimestamp) - interval '8 days' + interval '18 hours', 2, 120, 'CANCELED', interval '9 days'),
('+79030000008', 'Игровая Зона', ARRAY['A6'], date_trunc('day', localtimestamp) + interval '4 days' + interval '18 hours', 2, 120, 'UPCOMING', interval '1 day'),

-- +79030000009: одиночные редкие визиты только в один клуб
('+79030000009', 'Кибер Арена', ARRAY['B3'], date_trunc('day', localtimestamp) - interval '100 days' + interval '16 hours', 2, 120, 'DONE', interval '101 days'),
('+79030000009', 'Кибер Арена', ARRAY['B3'], date_trunc('day', localtimestamp) - interval '12 days' + interval '16 hours', 2, 120, 'DONE', interval '13 days'),

-- +79030000010: сравнивает клубы и почти не повторяется
('+79030000010', 'Кибер Арена', ARRAY['A7'], date_trunc('day', localtimestamp) - interval '72 days' + interval '17 hours', 2, 120, 'DONE', interval '73 days'),
('+79030000010', 'Игровая Зона', ARRAY['A7'], date_trunc('day', localtimestamp) - interval '58 days' + interval '17 hours', 2, 120, 'DONE', interval '59 days'),
('+79030000010', 'Пиксель', ARRAY['A7'], date_trunc('day', localtimestamp) - interval '44 days' + interval '17 hours', 2, 120, 'DONE', interval '45 days'),
('+79030000010', 'Возрождение', ARRAY['A7'], date_trunc('day', localtimestamp) - interval '30 days' + interval '17 hours', 2, 120, 'DONE', interval '31 days'),
('+79030000010', 'Ночной Байт', ARRAY['A7'], date_trunc('day', localtimestamp) - interval '16 days' + interval '17 hours', 2, 120, 'DONE', interval '17 days'),

-- +79030000011: раз в месяц в один и тот же день, как личный ритуал
('+79030000011', 'Возрождение', ARRAY['V1'], date_trunc('month', localtimestamp) - interval '4 months' + interval '14 days' + interval '20 hours', 3, 250, 'DONE', interval '5 months'),
('+79030000011', 'Возрождение', ARRAY['V1'], date_trunc('month', localtimestamp) - interval '3 months' + interval '14 days' + interval '20 hours', 3, 250, 'DONE', interval '4 months'),
('+79030000011', 'Возрождение', ARRAY['V1'], date_trunc('month', localtimestamp) - interval '2 months' + interval '14 days' + interval '20 hours', 3, 250, 'DONE', interval '3 months'),
('+79030000011', 'Возрождение', ARRAY['V1'], date_trunc('month', localtimestamp) - interval '1 month' + interval '14 days' + interval '20 hours', 3, 250, 'DONE', interval '2 months'),

-- +79030000012: ходит вдвоем по выходным
('+79030000012', 'Ночной Байт', ARRAY['B1','B2'], date_trunc('day', localtimestamp) - interval '38 days' + interval '19 hours', 3, 120, 'DONE', interval '40 days'),
('+79030000012', 'Ночной Байт', ARRAY['B1','B2'], date_trunc('day', localtimestamp) - interval '24 days' + interval '19 hours', 3, 120, 'DONE', interval '26 days'),
('+79030000012', 'Ночной Байт', ARRAY['B1','B2'], date_trunc('day', localtimestamp) - interval '10 days' + interval '19 hours', 3, 120, 'DONE', interval '12 days'),
('+79030000012', 'Ночной Байт', ARRAY['B1','B2'], date_trunc('day', localtimestamp) + interval '11 days' + interval '19 hours', 3, 120, 'UPCOMING', interval '2 days'),

-- +79030000013: экономные короткие брони без покупок
('+79030000013', 'Пиксель', ARRAY['A8'], date_trunc('day', localtimestamp) - interval '20 days' + interval '12 hours', 1, 120, 'DONE', interval '21 days'),
('+79030000013', 'Пиксель', ARRAY['A9'], date_trunc('day', localtimestamp) - interval '13 days' + interval '12 hours', 1, 120, 'DONE', interval '14 days'),
('+79030000013', 'Пиксель', ARRAY['A10'], date_trunc('day', localtimestamp) - interval '6 days' + interval '12 hours', 1, 120, 'DONE', interval '7 days'),

-- +79030000014: заранее бронирует будущие игры
('+79030000014', 'Возрождение', ARRAY['A1'], date_trunc('day', localtimestamp) + interval '3 days' + interval '18 hours', 2, 120, 'UPCOMING', interval '10 days'),
('+79030000014', 'Возрождение', ARRAY['A2'], date_trunc('day', localtimestamp) + interval '17 days' + interval '18 hours', 2, 120, 'UPCOMING', interval '9 days'),
('+79030000014', 'Возрождение', ARRAY['A3'], date_trunc('day', localtimestamp) + interval '31 days' + interval '18 hours', 2, 120, 'UPCOMING', interval '8 days'),

-- +79030000015: пробовал ходить, но часто отменял
('+79030000015', 'Игровая Зона', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '50 days' + interval '21 hours', 2, 250, 'CANCELED', interval '51 days'),
('+79030000015', 'Игровая Зона', ARRAY['V2'], date_trunc('day', localtimestamp) - interval '36 days' + interval '21 hours', 2, 250, 'CANCELED', interval '37 days'),
('+79030000015', 'Игровая Зона', ARRAY['V3'], date_trunc('day', localtimestamp) - interval '19 days' + interval '21 hours', 2, 250, 'DONE', interval '20 days'),

-- +79030000016: командные тренировки по 5 мест
('+79030000016', 'Кибер Арена', ARRAY['A3','A4','A5','A6','B1'], date_trunc('day', localtimestamp) - interval '45 days' + interval '18 hours', 3, 120, 'DONE', interval '47 days'),
('+79030000016', 'Кибер Арена', ARRAY['A3','A4','A5','A6','B1'], date_trunc('day', localtimestamp) - interval '22 days' + interval '18 hours', 3, 120, 'DONE', interval '24 days'),
('+79030000016', 'Кибер Арена', ARRAY['A3','A4','A5','A6','B1'], date_trunc('day', localtimestamp) + interval '8 days' + interval '18 hours', 3, 120, 'UPCOMING', interval '3 days'),

-- +79030000017: почти новый пользователь, только пара визитов
('+79030000017', 'Жесткий Режим', ARRAY['A1'], date_trunc('day', localtimestamp) - interval '9 days' + interval '19 hours', 2, 120, 'DONE', interval '10 days'),
('+79030000017', 'Жесткий Режим', ARRAY['A2'], date_trunc('day', localtimestamp) + interval '6 days' + interval '19 hours', 2, 120, 'UPCOMING', interval '1 day'),

-- +79030000018: каждую пятницу, один и тот же любимый VIP
('+79030000018', 'Ночной Байт', ARRAY['V4'], date_trunc('day', localtimestamp) - interval '49 days' + interval '20 hours', 3, 250, 'DONE', interval '50 days'),
('+79030000018', 'Ночной Байт', ARRAY['V4'], date_trunc('day', localtimestamp) - interval '35 days' + interval '20 hours', 3, 250, 'DONE', interval '36 days'),
('+79030000018', 'Ночной Байт', ARRAY['V4'], date_trunc('day', localtimestamp) - interval '21 days' + interval '20 hours', 3, 250, 'DONE', interval '22 days'),
('+79030000018', 'Ночной Байт', ARRAY['V4'], date_trunc('day', localtimestamp) - interval '7 days' + interval '20 hours', 3, 250, 'DONE', interval '8 days'),

-- +79030000019: выбирает только VIP, но в разных клубах
('+79030000019', 'Кибер Арена', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '40 days' + interval '21 hours', 2, 250, 'DONE', interval '41 days'),
('+79030000019', 'Игровая Зона', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '29 days' + interval '21 hours', 2, 250, 'DONE', interval '30 days'),
('+79030000019', 'Пиксель', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '18 days' + interval '21 hours', 2, 250, 'DONE', interval '19 days'),
('+79030000019', 'Возрождение', ARRAY['V1'], date_trunc('day', localtimestamp) - interval '7 days' + interval '21 hours', 2, 250, 'DONE', interval '8 days'),

-- +79030000020: семейные дневные походы по выходным, несколько мест
('+79030000020', 'Игровая Зона', ARRAY['A8','A9','A10'], date_trunc('day', localtimestamp) - interval '60 days' + interval '13 hours', 2, 120, 'DONE', interval '62 days'),
('+79030000020', 'Игровая Зона', ARRAY['A8','A9','A10'], date_trunc('day', localtimestamp) - interval '32 days' + interval '13 hours', 2, 120, 'DONE', interval '34 days'),
('+79030000020', 'Игровая Зона', ARRAY['A8','A9','A10'], date_trunc('day', localtimestamp) - interval '4 days' + interval '13 hours', 2, 120, 'DONE', interval '6 days');

INSERT INTO bookings (
    user_id,
    club_id,
    start_at,
    end_at,
    package_hours,
    rate_rub_per_hour_snapshot,
    total_rub_snapshot,
    status,
    purchase_id,
    created_at,
    updated_at
)
SELECT u.id,
       c.id,
       p.start_at,
       p.start_at + (p.duration_hours || ' hours')::interval,
       p.duration_hours,
       p.rate_rub_per_hour,
       (array_length(p.seat_labels, 1) * p.duration_hours * p.rate_rub_per_hour)::int,
       p.status,
       null,
       p.start_at - p.created_offset,
       localtimestamp
FROM demo_user_booking_patterns p
JOIN users u ON u.phone = p.user_phone
JOIN clubs c ON c.name = p.club_name;

UPDATE demo_user_booking_patterns p
SET booking_id = b.id
FROM users u, clubs c, bookings b
WHERE u.phone = p.user_phone
  AND c.name = p.club_name
  AND b.user_id = u.id
  AND b.club_id = c.id
  AND b.start_at = p.start_at
  AND b.end_at = p.start_at + (p.duration_hours || ' hours')::interval
  AND b.purchase_id IS NULL;

INSERT INTO booking_seats (booking_id, seat_id)
SELECT p.booking_id, s.id
FROM demo_user_booking_patterns p
JOIN clubs c ON c.name = p.club_name
JOIN LATERAL unnest(p.seat_labels) AS seat_label(label) ON true
JOIN seats s ON s.club_id = c.id AND s.label = seat_label.label
WHERE p.booking_id IS NOT NULL
ON CONFLICT DO NOTHING;
