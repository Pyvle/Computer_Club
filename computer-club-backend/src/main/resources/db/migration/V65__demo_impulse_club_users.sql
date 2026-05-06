CREATE TEMP TABLE demo_impulse_purchase_plan (
    code               text primary key,
    user_phone         text not null,
    created_at         timestamp not null,
    payment_status     text not null,
    booking_total_rub  int not null,
    products_total_rub int not null,
    purchase_id        bigint
) ON COMMIT DROP;

INSERT INTO demo_impulse_purchase_plan (code, user_phone, created_at, payment_status, booking_total_rub, products_total_rub) VALUES
    ('I101', '+79030000003', localtimestamp - interval '11 days', 'PAID', 240, 260),
    ('I102', '+79030000012', localtimestamp - interval '8 days',  'PAID', 720, 280),
    ('I103', '+79030000018', localtimestamp - interval '4 days',  'PAID', 1000, 130),
    ('I104', '+79030000021', localtimestamp - interval '1 day',   'PAID', 240, 220);

INSERT INTO purchases (user_id, club_id, created_at, booking_total_rub, products_total_rub, total_rub, payment_status)
SELECT u.id,
       c.id,
       p.created_at,
       p.booking_total_rub,
       p.products_total_rub,
       p.booking_total_rub + p.products_total_rub,
       p.payment_status
FROM demo_impulse_purchase_plan p
JOIN users u ON u.phone = p.user_phone
JOIN clubs c ON c.name = 'Импульс';

UPDATE demo_impulse_purchase_plan plan
SET purchase_id = p.id
FROM users u, clubs c, purchases p
WHERE c.name = 'Импульс'
  AND u.phone = plan.user_phone
  AND p.user_id = u.id
  AND p.club_id = c.id
  AND p.created_at = plan.created_at
  AND p.payment_status = plan.payment_status
  AND p.booking_total_rub = plan.booking_total_rub
  AND p.products_total_rub = plan.products_total_rub;

CREATE TEMP TABLE demo_impulse_booking_plan (
    code               text primary key,
    purchase_code      text,
    user_phone         text not null,
    seat_labels        text[] not null,
    start_at           timestamp not null,
    duration_hours     int not null,
    rate_rub_per_hour  int not null,
    status             text not null,
    created_offset     interval not null,
    booking_id         bigint
) ON COMMIT DROP;

INSERT INTO demo_impulse_booking_plan (code, purchase_code, user_phone, seat_labels, start_at, duration_hours, rate_rub_per_hour, status, created_offset) VALUES
    ('IB101', 'I101', '+79030000003', ARRAY['1'],     date_trunc('day', localtimestamp) - interval '11 days' + interval '19 hours', 2, 120, 'DONE',     interval '12 days'),
    ('IB102', 'I102', '+79030000012', ARRAY['2','3'], date_trunc('day', localtimestamp) - interval '8 days'  + interval '18 hours', 3, 120, 'DONE',     interval '9 days'),
    ('IB103', 'I103', '+79030000018', ARRAY['V-1'],   date_trunc('day', localtimestamp) - interval '4 days'  + interval '20 hours', 4, 250, 'DONE',     interval '5 days'),
    ('IB104', 'I104', '+79030000021', ARRAY['4'],     date_trunc('day', localtimestamp) - interval '1 day'   + interval '17 hours', 2, 120, 'DONE',     interval '2 days'),
    ('IB105', null,   '+79030000014', ARRAY['1','2'], date_trunc('day', localtimestamp) + interval '3 days'  + interval '18 hours', 2, 120, 'UPCOMING', interval '2 days'),
    ('IB106', null,   '+79030000015', ARRAY['V-1'],   date_trunc('day', localtimestamp) - interval '2 days'  + interval '21 hours', 2, 250, 'CANCELED', interval '3 days'),
    ('IB107', null,   '+79030000022', ARRAY['3'],     date_trunc('day', localtimestamp) + interval '6 days'  + interval '16 hours', 1, 120, 'UPCOMING', interval '1 day');

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
       b.start_at,
       b.start_at + (b.duration_hours || ' hours')::interval,
       b.duration_hours,
       b.rate_rub_per_hour,
       (array_length(b.seat_labels, 1) * b.duration_hours * b.rate_rub_per_hour)::int,
       b.status,
       p.purchase_id,
       b.start_at - b.created_offset,
       localtimestamp
FROM demo_impulse_booking_plan b
JOIN users u ON u.phone = b.user_phone
JOIN clubs c ON c.name = 'Импульс'
LEFT JOIN demo_impulse_purchase_plan p ON p.code = b.purchase_code;

UPDATE demo_impulse_booking_plan plan
SET booking_id = b.id
FROM users u, clubs c, bookings b
WHERE c.name = 'Импульс'
  AND u.phone = plan.user_phone
  AND b.user_id = u.id
  AND b.club_id = c.id
  AND b.start_at = plan.start_at
  AND b.end_at = plan.start_at + (plan.duration_hours || ' hours')::interval;

INSERT INTO booking_seats (booking_id, seat_id)
SELECT plan.booking_id, s.id
FROM demo_impulse_booking_plan plan
JOIN clubs c ON c.name = 'Импульс'
JOIN LATERAL unnest(plan.seat_labels) AS seat_label(label) ON true
JOIN seats s ON s.club_id = c.id AND s.label = seat_label.label
WHERE plan.booking_id IS NOT NULL
ON CONFLICT DO NOTHING;

WITH reports(user_phone, message, status, created_at) AS (
    VALUES
        ('+79030000012', 'Когда приходим компанией, хотелось бы видеть ещё пару соседних мест в обычном зале.', 'IN_PROGRESS', localtimestamp - interval '3 days'),
        ('+79030000021', 'На месте 4 мышь начала даблкликать, лучше проверить периферию в обычной зоне.', 'NEW', localtimestamp - interval '22 hours')
)
INSERT INTO club_user_reports (club_id, user_id, message, status, created_at)
SELECT c.id, u.id, r.message, r.status, r.created_at
FROM reports r
JOIN users u ON u.phone = r.user_phone
JOIN clubs c ON c.name = 'Импульс'
WHERE NOT EXISTS (
    SELECT 1
    FROM club_user_reports existing
    WHERE existing.club_id = c.id
      AND existing.user_id = u.id
      AND existing.message = r.message
);

WITH block_data AS (
    SELECT
        c.id AS club_id,
        u.id AS user_id,
        blocker.id AS blocked_by_user_id
    FROM clubs c
    JOIN users u ON u.phone = '+79030000015'
    LEFT JOIN users blocker ON blocker.phone = '+79991234567'
    WHERE c.name = 'Импульс'
)
INSERT INTO club_user_blocks (club_id, user_id, is_blocked, reason, blocked_until, blocked_by_user_id, created_at, updated_at)
SELECT
    d.club_id,
    d.user_id,
    true,
    'Две отмены подряд без предупреждения, доступ временно ограничен.',
    localtimestamp + interval '7 days',
    d.blocked_by_user_id,
    localtimestamp - interval '1 day',
    localtimestamp
FROM block_data d
ON CONFLICT (club_id, user_id) DO UPDATE
SET is_blocked = true,
    reason = excluded.reason,
    blocked_until = excluded.blocked_until,
    blocked_by_user_id = excluded.blocked_by_user_id,
    updated_at = localtimestamp;
