CREATE TEMP TABLE demo_owner_accounts (
    phone     text,
    club_name text
) ON COMMIT DROP;

INSERT INTO demo_owner_accounts (phone, club_name) VALUES
    ('+79031000001', 'Точка Победы'),
    ('+79031000002', 'Красный Джойстик'),
    ('+79031000003', 'Новая Катка'),
    ('+79031000004', 'Лига Компьютеров');

INSERT INTO users (phone, password_hash, is_active, global_role, created_at, updated_at)
SELECT phone,
       '$2a$10$Wl619CvxomkriIr2hXGOb.x6tpOYwGMdEYLmcUUzHGHBdBz.Wua.C',
       true,
       'USER',
       localtimestamp,
       localtimestamp
FROM demo_owner_accounts
ON CONFLICT (phone) DO UPDATE
SET password_hash = excluded.password_hash,
    is_active = true,
    updated_at = localtimestamp;

INSERT INTO club_staff (club_id, user_id, role, added_by_user_id, created_at, updated_at)
SELECT c.id, u.id, 'OWNER', added_by.id, localtimestamp, localtimestamp
FROM demo_owner_accounts o
JOIN clubs c ON c.name = o.club_name
JOIN users u ON u.phone = o.phone
LEFT JOIN users added_by ON added_by.phone = '+79030000002'
WHERE NOT EXISTS (
    SELECT 1
    FROM club_staff existing
    WHERE existing.club_id = c.id
      AND existing.role = 'OWNER'
)
ON CONFLICT (club_id, user_id) DO UPDATE
SET role = 'OWNER',
    added_by_user_id = excluded.added_by_user_id,
    updated_at = localtimestamp;

UPDATE users u
SET password_hash = '$2a$10$Wl619CvxomkriIr2hXGOb.x6tpOYwGMdEYLmcUUzHGHBdBz.Wua.C',
    is_active = true,
    updated_at = localtimestamp
WHERE EXISTS (
    SELECT 1
    FROM club_staff cs
    WHERE cs.user_id = u.id
      AND cs.role = 'OWNER'
);
