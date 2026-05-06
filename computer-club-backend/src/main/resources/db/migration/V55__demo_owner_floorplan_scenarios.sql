CREATE TEMP TABLE demo_owner_floorplan_defs (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    status text NOT NULL,
    sort_no int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_floorplan_defs (club_name, floorplan_name, status, sort_no) VALUES
    ('Пробный Клуб', 'Основная схема', 'PUBLISHED', 10),
    ('Пробный Клуб', 'Черновик расширения', 'DRAFT', 20),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'DRAFT', 30),
    ('Пробный Клуб', 'Архив: большая схема', 'ARCHIVED', 40),
    ('Пробный Клуб', 'Архив: компактная расстановка', 'ARCHIVED', 50),
    ('Пробный Клуб', 'Архив: линейная расстановка', 'ARCHIVED', 60),
    ('Пробный Клуб', 'Старая схема зала', 'ARCHIVED', 70),
    ('Северный Портал', 'Основная схема', 'PUBLISHED', 110),
    ('Северный Портал', 'Черновик турнира', 'DRAFT', 120),
    ('Северный Портал', 'Черновик стрим-зоны', 'DRAFT', 130),
    ('Северный Портал', 'Архив: старая расстановка', 'ARCHIVED', 140),
    ('Линия Пикселей', 'Основная схема', 'PUBLISHED', 210),
    ('Линия Пикселей', 'Черновик турнира', 'DRAFT', 220),
    ('Линия Пикселей', 'Черновик ночного формата', 'DRAFT', 230),
    ('Линия Пикселей', 'Архив: старая расстановка', 'ARCHIVED', 240);

INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT c.id, d.floorplan_name, 320, 240, 32, d.status, 1, '{"version":1,"items":[]}'::jsonb, localtimestamp, localtimestamp
FROM demo_owner_floorplan_defs d
JOIN clubs c ON c.name = d.club_name
JOIN club_staff cs ON cs.club_id = c.id AND cs.role = 'OWNER'
JOIN users u ON u.id = cs.user_id AND u.phone = '+79991234567'
WHERE NOT EXISTS (
    SELECT 1
    FROM club_floorplans existing
    WHERE existing.club_id = c.id
      AND existing.name = d.floorplan_name
)
AND (
    d.status <> 'PUBLISHED'
    OR NOT EXISTS (
        SELECT 1
        FROM club_floorplans published
        WHERE published.club_id = c.id
          AND published.status = 'PUBLISHED'
    )
);

UPDATE club_floorplans fp
SET status = d.status,
    updated_at = localtimestamp
FROM demo_owner_floorplan_defs d
JOIN clubs c ON c.name = d.club_name
JOIN club_staff cs ON cs.club_id = c.id AND cs.role = 'OWNER'
JOIN users u ON u.id = cs.user_id AND u.phone = '+79991234567'
WHERE fp.club_id = c.id
  AND fp.name = d.floorplan_name
  AND fp.status <> d.status;

CREATE TEMP TABLE demo_owner_floorplan_zones (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    col_from int NOT NULL,
    col_to int NOT NULL,
    row_from int NOT NULL,
    row_to int NOT NULL,
    room_type text NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_floorplan_zones (club_name, floorplan_name, col_from, col_to, row_from, row_to, room_type) VALUES
    ('Пробный Клуб', 'Основная схема', 1, 8, 1, 5, 'REGULAR'),
    ('Пробный Клуб', 'Основная схема', 10, 13, 1, 5, 'VIP'),
    ('Пробный Клуб', 'Черновик расширения', 1, 10, 1, 7, 'REGULAR'),
    ('Пробный Клуб', 'Черновик расширения', 12, 16, 1, 7, 'VIP'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 1, 6, 1, 6, 'REGULAR'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 8, 14, 1, 6, 'VIP'),
    ('Пробный Клуб', 'Архив: большая схема', 1, 11, 1, 8, 'REGULAR'),
    ('Пробный Клуб', 'Архив: большая схема', 13, 17, 2, 7, 'VIP'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 1, 6, 1, 5, 'REGULAR'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 8, 10, 2, 5, 'VIP'),
    ('Пробный Клуб', 'Архив: линейная расстановка', 1, 9, 2, 4, 'REGULAR'),
    ('Пробный Клуб', 'Архив: линейная расстановка', 11, 13, 2, 4, 'VIP'),
    ('Пробный Клуб', 'Старая схема зала', 1, 5, 1, 4, 'REGULAR'),
    ('Пробный Клуб', 'Старая схема зала', 7, 9, 1, 4, 'VIP'),
    ('Северный Портал', 'Основная схема', 1, 10, 1, 8, 'REGULAR'),
    ('Северный Портал', 'Основная схема', 12, 16, 1, 8, 'VIP'),
    ('Северный Портал', 'Черновик турнира', 1, 12, 1, 9, 'REGULAR'),
    ('Северный Портал', 'Черновик турнира', 14, 17, 3, 7, 'VIP'),
    ('Северный Портал', 'Черновик стрим-зоны', 1, 9, 1, 8, 'REGULAR'),
    ('Северный Портал', 'Черновик стрим-зоны', 11, 17, 1, 8, 'VIP'),
    ('Северный Портал', 'Архив: старая расстановка', 1, 7, 1, 6, 'REGULAR'),
    ('Северный Портал', 'Архив: старая расстановка', 9, 12, 2, 5, 'VIP'),
    ('Линия Пикселей', 'Основная схема', 1, 11, 1, 8, 'REGULAR'),
    ('Линия Пикселей', 'Основная схема', 13, 17, 2, 7, 'VIP'),
    ('Линия Пикселей', 'Черновик турнира', 1, 12, 1, 9, 'REGULAR'),
    ('Линия Пикселей', 'Черновик турнира', 14, 17, 2, 8, 'VIP'),
    ('Линия Пикселей', 'Черновик ночного формата', 1, 9, 1, 8, 'REGULAR'),
    ('Линия Пикселей', 'Черновик ночного формата', 11, 17, 1, 8, 'VIP'),
    ('Линия Пикселей', 'Архив: старая расстановка', 1, 7, 1, 6, 'REGULAR'),
    ('Линия Пикселей', 'Архив: старая расстановка', 9, 12, 1, 5, 'VIP');

CREATE TEMP TABLE demo_owner_floorplan_items (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    item_order int NOT NULL,
    item_type text NOT NULL,
    seat_label text,
    col_no int NOT NULL,
    row_no int NOT NULL,
    room_type text,
    orientation text,
    auto_wall boolean DEFAULT false
) ON COMMIT DROP;

INSERT INTO demo_owner_floorplan_items (club_name, floorplan_name, item_order, item_type, seat_label, col_no, row_no, room_type)
SELECT z.club_name, z.floorplan_name, 10000 + rows.row_no * 100 + cols.col_no, 'FLOOR', NULL, cols.col_no, rows.row_no, z.room_type
FROM demo_owner_floorplan_zones z
CROSS JOIN LATERAL generate_series(z.col_from, z.col_to) AS cols(col_no)
CROSS JOIN LATERAL generate_series(z.row_from, z.row_to) AS rows(row_no);

INSERT INTO demo_owner_floorplan_items (club_name, floorplan_name, item_order, item_type, seat_label, col_no, row_no) VALUES
    ('Пробный Клуб', 'Основная схема', 20001, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Основная схема', 20002, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Основная схема', 20003, 'SEAT', '3', 6, 3),
    ('Пробный Клуб', 'Основная схема', 20004, 'SEAT', '4', 11, 2),
    ('Пробный Клуб', 'Основная схема', 20005, 'SEAT', 'V-1', 12, 4),
    ('Пробный Клуб', 'Черновик расширения', 20101, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Черновик расширения', 20102, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Черновик расширения', 20103, 'SEAT', '3', 6, 2),
    ('Пробный Клуб', 'Черновик расширения', 20104, 'SEAT', '4', 13, 3),
    ('Пробный Клуб', 'Черновик расширения', 20105, 'SEAT', 'V-1', 15, 5),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20201, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20202, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20203, 'SEAT', '3', 3, 5),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20204, 'SEAT', '4', 10, 2),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20205, 'SEAT', 'V-1', 12, 4),
    ('Пробный Клуб', 'Архив: большая схема', 20301, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Архив: большая схема', 20302, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Архив: большая схема', 20303, 'SEAT', '3', 6, 5),
    ('Пробный Клуб', 'Архив: большая схема', 20304, 'SEAT', '4', 14, 3),
    ('Пробный Клуб', 'Архив: большая схема', 20305, 'SEAT', 'V-1', 16, 6),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20401, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20402, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20403, 'SEAT', '3', 2, 4),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20404, 'SEAT', '4', 8, 2),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20405, 'SEAT', 'V-1', 9, 4),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20501, 'SEAT', '1', 2, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20502, 'SEAT', '2', 4, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20503, 'SEAT', '3', 6, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20504, 'SEAT', '4', 11, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20505, 'SEAT', 'V-1', 12, 3),
    ('Пробный Клуб', 'Старая схема зала', 20601, 'SEAT', '1', 2, 2),
    ('Пробный Клуб', 'Старая схема зала', 20602, 'SEAT', '2', 4, 2),
    ('Пробный Клуб', 'Старая схема зала', 20603, 'SEAT', '3', 2, 4),
    ('Пробный Клуб', 'Старая схема зала', 20604, 'SEAT', '4', 7, 2),
    ('Пробный Клуб', 'Старая схема зала', 20605, 'SEAT', 'V-1', 8, 4),
    ('Северный Портал', 'Основная схема', 21001, 'SEAT', 'A1', 2, 2),
    ('Северный Портал', 'Основная схема', 21002, 'SEAT', 'A2', 4, 2),
    ('Северный Портал', 'Основная схема', 21003, 'SEAT', 'A3', 6, 2),
    ('Северный Портал', 'Основная схема', 21004, 'SEAT', 'A4', 8, 2),
    ('Северный Портал', 'Основная схема', 21005, 'SEAT', 'B1', 2, 4),
    ('Северный Портал', 'Основная схема', 21006, 'SEAT', 'B2', 4, 4),
    ('Северный Портал', 'Основная схема', 21007, 'SEAT', 'C1', 2, 6),
    ('Северный Портал', 'Основная схема', 21008, 'SEAT', 'C2', 4, 6),
    ('Северный Портал', 'Основная схема', 21009, 'SEAT', 'C3', 6, 6),
    ('Северный Портал', 'Основная схема', 21010, 'SEAT', 'C4', 8, 6),
    ('Северный Портал', 'Основная схема', 21011, 'SEAT', 'V1', 13, 2),
    ('Северный Портал', 'Основная схема', 21012, 'SEAT', 'V2', 15, 2),
    ('Северный Портал', 'Основная схема', 21013, 'SEAT', 'V3', 13, 6),
    ('Северный Портал', 'Основная схема', 21014, 'SEAT', 'V4', 15, 6),
    ('Северный Портал', 'Черновик турнира', 21101, 'SEAT', 'A1', 2, 3),
    ('Северный Портал', 'Черновик турнира', 21102, 'SEAT', 'A2', 4, 3),
    ('Северный Портал', 'Черновик турнира', 21103, 'SEAT', 'A3', 6, 3),
    ('Северный Портал', 'Черновик турнира', 21104, 'SEAT', 'A4', 8, 3),
    ('Северный Портал', 'Черновик турнира', 21105, 'SEAT', 'B1', 10, 4),
    ('Северный Портал', 'Черновик турнира', 21106, 'SEAT', 'B2', 10, 6),
    ('Северный Портал', 'Черновик турнира', 21107, 'SEAT', 'C1', 2, 7),
    ('Северный Портал', 'Черновик турнира', 21108, 'SEAT', 'C2', 4, 7),
    ('Северный Портал', 'Черновик турнира', 21109, 'SEAT', 'C3', 6, 7),
    ('Северный Портал', 'Черновик турнира', 21110, 'SEAT', 'C4', 8, 7),
    ('Северный Портал', 'Черновик турнира', 21111, 'SEAT', 'V1', 15, 3),
    ('Северный Портал', 'Черновик турнира', 21112, 'SEAT', 'V2', 16, 4),
    ('Северный Портал', 'Черновик турнира', 21113, 'SEAT', 'V3', 15, 6),
    ('Северный Портал', 'Черновик турнира', 21114, 'SEAT', 'V4', 16, 7),
    ('Северный Портал', 'Черновик стрим-зоны', 21201, 'SEAT', 'A1', 2, 2),
    ('Северный Портал', 'Черновик стрим-зоны', 21202, 'SEAT', 'A2', 4, 2),
    ('Северный Портал', 'Черновик стрим-зоны', 21203, 'SEAT', 'A3', 6, 3),
    ('Северный Портал', 'Черновик стрим-зоны', 21204, 'SEAT', 'A4', 8, 3),
    ('Северный Портал', 'Черновик стрим-зоны', 21205, 'SEAT', 'B1', 2, 6),
    ('Северный Портал', 'Черновик стрим-зоны', 21206, 'SEAT', 'B2', 4, 6),
    ('Северный Портал', 'Черновик стрим-зоны', 21207, 'SEAT', 'C1', 6, 7),
    ('Северный Портал', 'Черновик стрим-зоны', 21208, 'SEAT', 'C2', 8, 7),
    ('Северный Портал', 'Черновик стрим-зоны', 21209, 'SEAT', 'C3', 12, 2),
    ('Северный Портал', 'Черновик стрим-зоны', 21210, 'SEAT', 'C4', 14, 2),
    ('Северный Портал', 'Черновик стрим-зоны', 21211, 'SEAT', 'V1', 12, 5),
    ('Северный Портал', 'Черновик стрим-зоны', 21212, 'SEAT', 'V2', 14, 5),
    ('Северный Портал', 'Черновик стрим-зоны', 21213, 'SEAT', 'V3', 12, 7),
    ('Северный Портал', 'Черновик стрим-зоны', 21214, 'SEAT', 'V4', 14, 7),
    ('Северный Портал', 'Архив: старая расстановка', 21301, 'SEAT', 'A1', 2, 2),
    ('Северный Портал', 'Архив: старая расстановка', 21302, 'SEAT', 'A2', 4, 2),
    ('Северный Портал', 'Архив: старая расстановка', 21303, 'SEAT', 'A3', 6, 2),
    ('Северный Портал', 'Архив: старая расстановка', 21304, 'SEAT', 'A4', 2, 4),
    ('Северный Портал', 'Архив: старая расстановка', 21305, 'SEAT', 'B1', 4, 4),
    ('Северный Портал', 'Архив: старая расстановка', 21306, 'SEAT', 'B2', 6, 4),
    ('Северный Портал', 'Архив: старая расстановка', 21307, 'SEAT', 'C1', 2, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21308, 'SEAT', 'C2', 4, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21309, 'SEAT', 'C3', 6, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21310, 'SEAT', 'C4', 7, 5),
    ('Северный Портал', 'Архив: старая расстановка', 21311, 'SEAT', 'V1', 10, 2),
    ('Северный Портал', 'Архив: старая расстановка', 21312, 'SEAT', 'V2', 11, 3),
    ('Северный Портал', 'Архив: старая расстановка', 21313, 'SEAT', 'V3', 10, 5),
    ('Северный Портал', 'Архив: старая расстановка', 21314, 'SEAT', 'V4', 11, 5),
    ('Линия Пикселей', 'Основная схема', 22001, 'SEAT', 'A1', 2, 2),
    ('Линия Пикселей', 'Основная схема', 22002, 'SEAT', 'A2', 4, 2),
    ('Линия Пикселей', 'Основная схема', 22003, 'SEAT', 'A3', 6, 2),
    ('Линия Пикселей', 'Основная схема', 22004, 'SEAT', 'A4', 8, 2),
    ('Линия Пикселей', 'Основная схема', 22005, 'SEAT', 'B1', 2, 4),
    ('Линия Пикселей', 'Основная схема', 22006, 'SEAT', 'B2', 4, 4),
    ('Линия Пикселей', 'Основная схема', 22007, 'SEAT', 'C1', 2, 6),
    ('Линия Пикселей', 'Основная схема', 22008, 'SEAT', 'C2', 4, 6),
    ('Линия Пикселей', 'Основная схема', 22009, 'SEAT', 'C3', 6, 7),
    ('Линия Пикселей', 'Основная схема', 22010, 'SEAT', 'C4', 8, 7),
    ('Линия Пикселей', 'Основная схема', 22011, 'SEAT', 'V1', 14, 2),
    ('Линия Пикселей', 'Основная схема', 22012, 'SEAT', 'V2', 16, 2),
    ('Линия Пикселей', 'Основная схема', 22013, 'SEAT', 'V3', 14, 6),
    ('Линия Пикселей', 'Основная схема', 22014, 'SEAT', 'V4', 16, 6),
    ('Линия Пикселей', 'Черновик турнира', 22101, 'SEAT', 'A1', 2, 3),
    ('Линия Пикселей', 'Черновик турнира', 22102, 'SEAT', 'A2', 4, 3),
    ('Линия Пикселей', 'Черновик турнира', 22103, 'SEAT', 'A3', 6, 3),
    ('Линия Пикселей', 'Черновик турнира', 22104, 'SEAT', 'A4', 8, 3),
    ('Линия Пикселей', 'Черновик турнира', 22105, 'SEAT', 'B1', 10, 4),
    ('Линия Пикселей', 'Черновик турнира', 22106, 'SEAT', 'B2', 10, 6),
    ('Линия Пикселей', 'Черновик турнира', 22107, 'SEAT', 'C1', 2, 7),
    ('Линия Пикселей', 'Черновик турнира', 22108, 'SEAT', 'C2', 4, 7),
    ('Линия Пикселей', 'Черновик турнира', 22109, 'SEAT', 'C3', 6, 7),
    ('Линия Пикселей', 'Черновик турнира', 22110, 'SEAT', 'C4', 8, 7),
    ('Линия Пикселей', 'Черновик турнира', 22111, 'SEAT', 'V1', 15, 3),
    ('Линия Пикселей', 'Черновик турнира', 22112, 'SEAT', 'V2', 16, 4),
    ('Линия Пикселей', 'Черновик турнира', 22113, 'SEAT', 'V3', 15, 6),
    ('Линия Пикселей', 'Черновик турнира', 22114, 'SEAT', 'V4', 16, 7),
    ('Линия Пикселей', 'Черновик ночного формата', 22201, 'SEAT', 'A1', 2, 2),
    ('Линия Пикселей', 'Черновик ночного формата', 22202, 'SEAT', 'A2', 4, 2),
    ('Линия Пикселей', 'Черновик ночного формата', 22203, 'SEAT', 'A3', 6, 2),
    ('Линия Пикселей', 'Черновик ночного формата', 22204, 'SEAT', 'A4', 8, 3),
    ('Линия Пикселей', 'Черновик ночного формата', 22205, 'SEAT', 'B1', 2, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 22206, 'SEAT', 'B2', 4, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 22207, 'SEAT', 'C1', 6, 6),
    ('Линия Пикселей', 'Черновик ночного формата', 22208, 'SEAT', 'C2', 8, 6),
    ('Линия Пикселей', 'Черновик ночного формата', 22209, 'SEAT', 'C3', 12, 2),
    ('Линия Пикселей', 'Черновик ночного формата', 22210, 'SEAT', 'C4', 14, 2),
    ('Линия Пикселей', 'Черновик ночного формата', 22211, 'SEAT', 'V1', 12, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 22212, 'SEAT', 'V2', 14, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 22213, 'SEAT', 'V3', 16, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 22214, 'SEAT', 'V4', 16, 7),
    ('Линия Пикселей', 'Архив: старая расстановка', 22301, 'SEAT', 'A1', 2, 2),
    ('Линия Пикселей', 'Архив: старая расстановка', 22302, 'SEAT', 'A2', 4, 2),
    ('Линия Пикселей', 'Архив: старая расстановка', 22303, 'SEAT', 'A3', 6, 2),
    ('Линия Пикселей', 'Архив: старая расстановка', 22304, 'SEAT', 'A4', 2, 4),
    ('Линия Пикселей', 'Архив: старая расстановка', 22305, 'SEAT', 'B1', 4, 4),
    ('Линия Пикселей', 'Архив: старая расстановка', 22306, 'SEAT', 'B2', 6, 4),
    ('Линия Пикселей', 'Архив: старая расстановка', 22307, 'SEAT', 'C1', 2, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22308, 'SEAT', 'C2', 4, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22309, 'SEAT', 'C3', 6, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22310, 'SEAT', 'C4', 7, 5),
    ('Линия Пикселей', 'Архив: старая расстановка', 22311, 'SEAT', 'V1', 10, 2),
    ('Линия Пикселей', 'Архив: старая расстановка', 22312, 'SEAT', 'V2', 11, 3),
    ('Линия Пикселей', 'Архив: старая расстановка', 22313, 'SEAT', 'V3', 10, 5),
    ('Линия Пикселей', 'Архив: старая расстановка', 22314, 'SEAT', 'V4', 11, 5);

INSERT INTO demo_owner_floorplan_items (club_name, floorplan_name, item_order, item_type, col_no, row_no, orientation, auto_wall)
SELECT DISTINCT club_name, floorplan_name, 30000 + row_no * 200 + col_no, 'WALL', col_no, row_no, orientation, true
FROM (
    SELECT f.club_name, f.floorplan_name, f.col_no, f.row_no, 'H'::text AS orientation
    FROM demo_owner_floorplan_items f
    WHERE f.item_type = 'FLOOR'
      AND NOT EXISTS (
          SELECT 1 FROM demo_owner_floorplan_items n
          WHERE n.item_type = 'FLOOR'
            AND n.club_name = f.club_name
            AND n.floorplan_name = f.floorplan_name
            AND n.col_no = f.col_no
            AND n.row_no = f.row_no - 1
            AND n.room_type = f.room_type
      )
    UNION
    SELECT f.club_name, f.floorplan_name, f.col_no, f.row_no + 1, 'H'::text AS orientation
    FROM demo_owner_floorplan_items f
    WHERE f.item_type = 'FLOOR'
      AND NOT EXISTS (
          SELECT 1 FROM demo_owner_floorplan_items n
          WHERE n.item_type = 'FLOOR'
            AND n.club_name = f.club_name
            AND n.floorplan_name = f.floorplan_name
            AND n.col_no = f.col_no
            AND n.row_no = f.row_no + 1
            AND n.room_type = f.room_type
      )
    UNION
    SELECT f.club_name, f.floorplan_name, f.col_no, f.row_no, 'V'::text AS orientation
    FROM demo_owner_floorplan_items f
    WHERE f.item_type = 'FLOOR'
      AND NOT EXISTS (
          SELECT 1 FROM demo_owner_floorplan_items n
          WHERE n.item_type = 'FLOOR'
            AND n.club_name = f.club_name
            AND n.floorplan_name = f.floorplan_name
            AND n.col_no = f.col_no - 1
            AND n.row_no = f.row_no
            AND n.room_type = f.room_type
      )
    UNION
    SELECT f.club_name, f.floorplan_name, f.col_no + 1, f.row_no, 'V'::text AS orientation
    FROM demo_owner_floorplan_items f
    WHERE f.item_type = 'FLOOR'
      AND NOT EXISTS (
          SELECT 1 FROM demo_owner_floorplan_items n
          WHERE n.item_type = 'FLOOR'
            AND n.club_name = f.club_name
            AND n.floorplan_name = f.floorplan_name
            AND n.col_no = f.col_no + 1
            AND n.row_no = f.row_no
            AND n.room_type = f.room_type
      )
) walls;

WITH target_floorplans AS (
    SELECT fp.id, fp.name AS floorplan_name, c.name AS club_name
    FROM club_floorplans fp
    JOIN clubs c ON c.id = fp.club_id
    JOIN club_staff cs ON cs.club_id = c.id AND cs.role = 'OWNER'
    JOIN users u ON u.id = cs.user_id AND u.phone = '+79991234567'
    JOIN demo_owner_floorplan_defs d
        ON d.club_name = c.name
       AND d.floorplan_name = fp.name
), item_json AS (
    SELECT
        tf.id AS floorplan_id,
        jsonb_agg(
            CASE i.item_type
                WHEN 'FLOOR' THEN jsonb_build_object(
                    'type', 'FLOOR',
                    'col', i.col_no,
                    'row', i.row_no,
                    'roomType', i.room_type
                )
                WHEN 'WALL' THEN jsonb_build_object(
                    'type', 'WALL',
                    'orientation', i.orientation,
                    'col', i.col_no,
                    'row', i.row_no,
                    'auto', i.auto_wall
                )
                WHEN 'SEAT' THEN jsonb_build_object(
                    'type', 'SEAT',
                    'seatId', s.id,
                    'col', i.col_no,
                    'row', i.row_no
                )
            END
            ORDER BY i.item_order, i.item_type, i.col_no, i.row_no
        ) FILTER (
            WHERE i.item_type <> 'SEAT' OR s.id IS NOT NULL
        ) AS items,
        MAX(i.col_no) AS max_col,
        MAX(i.row_no) AS max_row
    FROM target_floorplans tf
    JOIN demo_owner_floorplan_items i
        ON i.club_name = tf.club_name
       AND i.floorplan_name = tf.floorplan_name
    LEFT JOIN clubs c ON c.name = i.club_name
    LEFT JOIN seats s
        ON s.club_id = c.id
       AND s.label = i.seat_label
    GROUP BY tf.id
)
UPDATE club_floorplans fp
SET width = (item_json.max_col + 2) * 32,
    height = (item_json.max_row + 2) * 32,
    grid_size = 32,
    version = fp.version + 1,
    data = jsonb_build_object('version', fp.version + 1, 'items', COALESCE(item_json.items, '[]'::jsonb)),
    updated_at = localtimestamp
FROM item_json
WHERE fp.id = item_json.floorplan_id;
