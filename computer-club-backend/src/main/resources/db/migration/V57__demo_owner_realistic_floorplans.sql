CREATE TEMP TABLE demo_real_floorplan_defs (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    layout_key text NOT NULL,
    max_col int NOT NULL,
    max_row int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_real_floorplan_defs (club_name, floorplan_name, layout_key, max_col, max_row) VALUES
    ('Пробный Клуб', 'Основная схема', 'small_main', 16, 12),
    ('Пробный Клуб', 'Черновик расширения', 'small_expanded', 18, 13),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'small_vip', 16, 11),
    ('Пробный Клуб', 'Архив: большая схема', 'small_big', 18, 13),
    ('Пробный Клуб', 'Архив: компактная расстановка', 'small_compact', 12, 10),
    ('Пробный Клуб', 'Архив: линейная расстановка', 'small_linear', 20, 7),
    ('Пробный Клуб', 'Старая схема зала', 'small_old', 13, 9),
    ('Северный Портал', 'Основная схема', 'big_main', 25, 17),
    ('Северный Портал', 'Черновик турнира', 'big_tournament', 25, 18),
    ('Северный Портал', 'Черновик стрим-зоны', 'big_stream', 25, 17),
    ('Северный Портал', 'Архив: старая расстановка', 'big_archive', 18, 13),
    ('Линия Пикселей', 'Основная схема', 'pixel_main', 25, 17),
    ('Линия Пикселей', 'Черновик турнира', 'pixel_tournament', 25, 18),
    ('Линия Пикселей', 'Черновик ночного формата', 'big_stream', 25, 17),
    ('Линия Пикселей', 'Архив: старая расстановка', 'pixel_archive', 18, 13);

CREATE TEMP TABLE demo_real_vip_zones (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    col_from int NOT NULL,
    col_to int NOT NULL,
    row_from int NOT NULL,
    row_to int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_real_vip_zones (club_name, floorplan_name, col_from, col_to, row_from, row_to) VALUES
    ('Пробный Клуб', 'Основная схема', 12, 16, 4, 12),
    ('Пробный Клуб', 'Черновик расширения', 13, 18, 2, 6),
    ('Пробный Клуб', 'Черновик расширения', 13, 18, 8, 13),
    ('Пробный Клуб', 'Черновик VIP-зоны', 9, 16, 2, 5),
    ('Пробный Клуб', 'Черновик VIP-зоны', 9, 16, 7, 11),
    ('Пробный Клуб', 'Архив: большая схема', 13, 18, 2, 6),
    ('Пробный Клуб', 'Архив: большая схема', 13, 18, 8, 13),
    ('Пробный Клуб', 'Архив: компактная расстановка', 8, 12, 3, 10),
    ('Пробный Клуб', 'Архив: линейная расстановка', 16, 20, 2, 7),
    ('Пробный Клуб', 'Старая схема зала', 9, 13, 3, 9),
    ('Северный Портал', 'Основная схема', 16, 23, 2, 7),
    ('Северный Портал', 'Основная схема', 16, 23, 10, 16),
    ('Северный Портал', 'Черновик турнира', 16, 23, 2, 6),
    ('Северный Портал', 'Черновик турнира', 16, 23, 10, 17),
    ('Северный Портал', 'Черновик стрим-зоны', 13, 19, 2, 7),
    ('Северный Портал', 'Черновик стрим-зоны', 13, 19, 10, 16),
    ('Северный Портал', 'Архив: старая расстановка', 12, 17, 3, 12),
    ('Линия Пикселей', 'Основная схема', 15, 20, 3, 8),
    ('Линия Пикселей', 'Основная схема', 15, 20, 10, 16),
    ('Линия Пикселей', 'Черновик турнира', 15, 20, 2, 6),
    ('Линия Пикселей', 'Черновик турнира', 15, 20, 10, 17),
    ('Линия Пикселей', 'Черновик ночного формата', 13, 19, 2, 7),
    ('Линия Пикселей', 'Черновик ночного формата', 13, 19, 10, 16),
    ('Линия Пикселей', 'Архив: старая расстановка', 12, 17, 2, 11);

CREATE TEMP TABLE demo_real_seat_templates (
    layout_key text NOT NULL,
    item_order int NOT NULL,
    seat_label text NOT NULL,
    col_no int NOT NULL,
    row_no int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_real_seat_templates (layout_key, item_order, seat_label, col_no, row_no) VALUES
    ('small_main', 20001, '1', 2, 6),
    ('small_main', 20002, '2', 4, 6),
    ('small_main', 20003, '3', 7, 9),
    ('small_main', 20004, '4', 13, 6),
    ('small_main', 20005, 'V-1', 14, 10),
    ('small_expanded', 20101, '1', 2, 6),
    ('small_expanded', 20102, '2', 4, 6),
    ('small_expanded', 20103, '3', 7, 10),
    ('small_expanded', 20104, '4', 15, 4),
    ('small_expanded', 20105, 'V-1', 15, 11),
    ('small_vip', 20201, '1', 2, 6),
    ('small_vip', 20202, '2', 4, 6),
    ('small_vip', 20203, '3', 6, 9),
    ('small_vip', 20204, '4', 12, 3),
    ('small_vip', 20205, 'V-1', 12, 9),
    ('small_big', 20301, '1', 2, 7),
    ('small_big', 20302, '2', 4, 7),
    ('small_big', 20303, '3', 7, 10),
    ('small_big', 20304, '4', 15, 4),
    ('small_big', 20305, 'V-1', 15, 11),
    ('small_compact', 20401, '1', 2, 5),
    ('small_compact', 20402, '2', 4, 5),
    ('small_compact', 20403, '3', 6, 8),
    ('small_compact', 20404, '4', 10, 4),
    ('small_compact', 20405, 'V-1', 10, 8),
    ('small_linear', 20501, '1', 5, 3),
    ('small_linear', 20502, '2', 7, 3),
    ('small_linear', 20503, '3', 10, 5),
    ('small_linear', 20504, '4', 17, 3),
    ('small_linear', 20505, 'V-1', 18, 5),
    ('small_old', 20601, '1', 2, 5),
    ('small_old', 20602, '2', 4, 5),
    ('small_old', 20603, '3', 6, 7),
    ('small_old', 20604, '4', 10, 4),
    ('small_old', 20605, 'V-1', 10, 7),
    ('big_main', 21001, 'A1', 2, 7),
    ('big_main', 21002, 'A2', 4, 7),
    ('big_main', 21003, 'A3', 2, 10),
    ('big_main', 21004, 'A4', 4, 10),
    ('big_main', 21005, 'B1', 7, 7),
    ('big_main', 21006, 'B2', 7, 10),
    ('big_main', 21007, 'C1', 10, 7),
    ('big_main', 21008, 'C2', 10, 10),
    ('big_main', 21009, 'C3', 2, 14),
    ('big_main', 21010, 'C4', 4, 14),
    ('big_main', 21011, 'V1', 17, 3),
    ('big_main', 21012, 'V2', 20, 5),
    ('big_main', 21013, 'V3', 17, 12),
    ('big_main', 21014, 'V4', 20, 14),
    ('pixel_main', 21001, 'A1', 2, 7),
    ('pixel_main', 21002, 'A2', 4, 7),
    ('pixel_main', 21003, 'A3', 7, 7),
    ('pixel_main', 21004, 'A4', 10, 7),
    ('pixel_main', 21005, 'B1', 2, 11),
    ('pixel_main', 21006, 'B2', 4, 11),
    ('pixel_main', 21007, 'C1', 7, 13),
    ('pixel_main', 21008, 'C2', 10, 13),
    ('pixel_main', 21009, 'C3', 2, 15),
    ('pixel_main', 21010, 'C4', 4, 15),
    ('pixel_main', 21011, 'V1', 16, 4),
    ('pixel_main', 21012, 'V2', 19, 6),
    ('pixel_main', 21013, 'V3', 16, 12),
    ('pixel_main', 21014, 'V4', 19, 14),
    ('big_tournament', 21101, 'A1', 2, 6),
    ('big_tournament', 21102, 'A2', 4, 6),
    ('big_tournament', 21103, 'A3', 6, 6),
    ('big_tournament', 21104, 'A4', 8, 6),
    ('big_tournament', 21105, 'B1', 10, 9),
    ('big_tournament', 21106, 'B2', 10, 13),
    ('big_tournament', 21107, 'C1', 2, 15),
    ('big_tournament', 21108, 'C2', 4, 15),
    ('big_tournament', 21109, 'C3', 6, 15),
    ('big_tournament', 21110, 'C4', 8, 15),
    ('big_tournament', 21111, 'V1', 17, 3),
    ('big_tournament', 21112, 'V2', 20, 5),
    ('big_tournament', 21113, 'V3', 17, 12),
    ('big_tournament', 21114, 'V4', 20, 15),
    ('pixel_tournament', 21101, 'A1', 2, 6),
    ('pixel_tournament', 21102, 'A2', 4, 6),
    ('pixel_tournament', 21103, 'A3', 6, 6),
    ('pixel_tournament', 21104, 'A4', 8, 6),
    ('pixel_tournament', 21105, 'B1', 10, 9),
    ('pixel_tournament', 21106, 'B2', 10, 13),
    ('pixel_tournament', 21107, 'C1', 2, 15),
    ('pixel_tournament', 21108, 'C2', 4, 15),
    ('pixel_tournament', 21109, 'C3', 6, 15),
    ('pixel_tournament', 21110, 'C4', 8, 15),
    ('pixel_tournament', 21111, 'V1', 16, 3),
    ('pixel_tournament', 21112, 'V2', 19, 5),
    ('pixel_tournament', 21113, 'V3', 16, 12),
    ('pixel_tournament', 21114, 'V4', 19, 15),
    ('big_stream', 21201, 'A1', 2, 6),
    ('big_stream', 21202, 'A2', 4, 6),
    ('big_stream', 21203, 'A3', 7, 8),
    ('big_stream', 21204, 'A4', 10, 8),
    ('big_stream', 21205, 'B1', 2, 12),
    ('big_stream', 21206, 'B2', 4, 12),
    ('big_stream', 21207, 'C1', 7, 14),
    ('big_stream', 21208, 'C2', 10, 14),
    ('big_stream', 21209, 'C3', 21, 5),
    ('big_stream', 21210, 'C4', 23, 10),
    ('big_stream', 21211, 'V1', 14, 4),
    ('big_stream', 21212, 'V2', 17, 6),
    ('big_stream', 21213, 'V3', 14, 12),
    ('big_stream', 21214, 'V4', 17, 15),
    ('big_archive', 21301, 'A1', 2, 6),
    ('big_archive', 21302, 'A2', 4, 6),
    ('big_archive', 21303, 'A3', 6, 6),
    ('big_archive', 21304, 'A4', 2, 9),
    ('big_archive', 21305, 'B1', 4, 9),
    ('big_archive', 21306, 'B2', 6, 9),
    ('big_archive', 21307, 'C1', 8, 6),
    ('big_archive', 21308, 'C2', 8, 9),
    ('big_archive', 21309, 'C3', 2, 12),
    ('big_archive', 21310, 'C4', 4, 12),
    ('big_archive', 21311, 'V1', 13, 4),
    ('big_archive', 21312, 'V2', 15, 5),
    ('big_archive', 21313, 'V3', 13, 9),
    ('big_archive', 21314, 'V4', 15, 11),
    ('pixel_archive', 21301, 'A1', 2, 6),
    ('pixel_archive', 21302, 'A2', 4, 6),
    ('pixel_archive', 21303, 'A3', 6, 6),
    ('pixel_archive', 21304, 'A4', 2, 9),
    ('pixel_archive', 21305, 'B1', 4, 9),
    ('pixel_archive', 21306, 'B2', 6, 9),
    ('pixel_archive', 21307, 'C1', 8, 6),
    ('pixel_archive', 21308, 'C2', 8, 9),
    ('pixel_archive', 21309, 'C3', 2, 12),
    ('pixel_archive', 21310, 'C4', 4, 12),
    ('pixel_archive', 21311, 'V1', 13, 3),
    ('pixel_archive', 21312, 'V2', 15, 5),
    ('pixel_archive', 21313, 'V3', 13, 8),
    ('pixel_archive', 21314, 'V4', 15, 10);

CREATE TEMP TABLE demo_real_wall_segments (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    orientation text NOT NULL,
    fixed_no int NOT NULL,
    from_no int NOT NULL,
    to_no int NOT NULL,
    gap_from int,
    gap_to int
) ON COMMIT DROP;

INSERT INTO demo_real_wall_segments (club_name, floorplan_name, orientation, fixed_no, from_no, to_no, gap_from, gap_to) VALUES
    ('Пробный Клуб', 'Основная схема', 'H', 4, 1, 8, 3, 4),
    ('Пробный Клуб', 'Основная схема', 'V', 6, 1, 4, 2, 2),
    ('Пробный Клуб', 'Основная схема', 'V', 12, 1, 12, 7, 8),
    ('Пробный Клуб', 'Основная схема', 'H', 9, 1, 11, 6, 7),
    ('Пробный Клуб', 'Черновик расширения', 'H', 4, 1, 9, 4, 5),
    ('Пробный Клуб', 'Черновик расширения', 'V', 9, 1, 13, 6, 7),
    ('Пробный Клуб', 'Черновик расширения', 'V', 13, 2, 6, 4, 4),
    ('Пробный Клуб', 'Черновик расширения', 'V', 13, 8, 13, 10, 10),
    ('Пробный Клуб', 'Черновик расширения', 'H', 7, 13, 18, 15, 16),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'H', 5, 1, 8, 3, 4),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'V', 8, 1, 11, 5, 6),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'V', 9, 2, 5, 4, 4),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'V', 9, 7, 11, 9, 9),
    ('Пробный Клуб', 'Черновик VIP-зоны', 'H', 6, 9, 16, 12, 13),
    ('Пробный Клуб', 'Архив: большая схема', 'H', 5, 1, 9, 4, 5),
    ('Пробный Клуб', 'Архив: большая схема', 'V', 9, 1, 13, 6, 7),
    ('Пробный Клуб', 'Архив: большая схема', 'V', 13, 2, 6, 4, 4),
    ('Пробный Клуб', 'Архив: большая схема', 'V', 13, 8, 13, 10, 10),
    ('Пробный Клуб', 'Архив: компактная расстановка', 'H', 4, 1, 7, 3, 3),
    ('Пробный Клуб', 'Архив: компактная расстановка', 'V', 7, 1, 10, 5, 6),
    ('Пробный Клуб', 'Архив: компактная расстановка', 'V', 8, 3, 10, 6, 6),
    ('Пробный Клуб', 'Архив: линейная расстановка', 'V', 5, 1, 7, 3, 4),
    ('Пробный Клуб', 'Архив: линейная расстановка', 'V', 16, 2, 7, 4, 4),
    ('Пробный Клуб', 'Старая схема зала', 'H', 4, 1, 7, 3, 3),
    ('Пробный Клуб', 'Старая схема зала', 'V', 8, 3, 9, 6, 6),
    ('Пробный Клуб', 'Старая схема зала', 'V', 9, 3, 9, 6, 6),
    ('Северный Портал', 'Основная схема', 'H', 5, 1, 8, 4, 5),
    ('Северный Портал', 'Основная схема', 'V', 9, 1, 17, 8, 10),
    ('Северный Портал', 'Основная схема', 'V', 16, 2, 7, 4, 5),
    ('Северный Портал', 'Основная схема', 'V', 16, 10, 16, 12, 13),
    ('Северный Портал', 'Основная схема', 'H', 8, 16, 23, 18, 19),
    ('Северный Портал', 'Основная схема', 'H', 10, 16, 23, 18, 19),
    ('Северный Портал', 'Основная схема', 'V', 21, 1, 7, 3, 4),
    ('Северный Портал', 'Черновик турнира', 'H', 5, 1, 8, 4, 5),
    ('Северный Портал', 'Черновик турнира', 'V', 9, 1, 18, 8, 10),
    ('Северный Портал', 'Черновик турнира', 'V', 16, 2, 6, 4, 4),
    ('Северный Портал', 'Черновик турнира', 'V', 16, 10, 17, 13, 14),
    ('Северный Портал', 'Черновик турнира', 'H', 9, 1, 13, 6, 8),
    ('Северный Портал', 'Черновик стрим-зоны', 'H', 5, 1, 8, 4, 5),
    ('Северный Портал', 'Черновик стрим-зоны', 'V', 9, 1, 17, 8, 10),
    ('Северный Портал', 'Черновик стрим-зоны', 'V', 13, 2, 7, 4, 5),
    ('Северный Портал', 'Черновик стрим-зоны', 'V', 13, 10, 16, 12, 13),
    ('Северный Портал', 'Черновик стрим-зоны', 'V', 20, 1, 17, 8, 9),
    ('Северный Портал', 'Архив: старая расстановка', 'H', 5, 1, 7, 3, 4),
    ('Северный Портал', 'Архив: старая расстановка', 'V', 8, 1, 13, 7, 8),
    ('Северный Портал', 'Архив: старая расстановка', 'V', 12, 3, 12, 7, 8),
    ('Линия Пикселей', 'Основная схема', 'H', 5, 1, 8, 4, 5),
    ('Линия Пикселей', 'Основная схема', 'V', 9, 1, 17, 8, 10),
    ('Линия Пикселей', 'Основная схема', 'V', 15, 3, 8, 5, 6),
    ('Линия Пикселей', 'Основная схема', 'V', 15, 10, 16, 12, 13),
    ('Линия Пикселей', 'Основная схема', 'V', 21, 1, 17, 8, 9),
    ('Линия Пикселей', 'Черновик турнира', 'H', 5, 1, 8, 4, 5),
    ('Линия Пикселей', 'Черновик турнира', 'V', 9, 1, 18, 8, 10),
    ('Линия Пикселей', 'Черновик турнира', 'V', 15, 2, 6, 4, 4),
    ('Линия Пикселей', 'Черновик турнира', 'V', 15, 10, 17, 13, 14),
    ('Линия Пикселей', 'Черновик турнира', 'H', 9, 1, 13, 6, 8),
    ('Линия Пикселей', 'Черновик ночного формата', 'H', 5, 1, 8, 4, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 'V', 9, 1, 17, 8, 10),
    ('Линия Пикселей', 'Черновик ночного формата', 'V', 13, 2, 7, 4, 5),
    ('Линия Пикселей', 'Черновик ночного формата', 'V', 13, 10, 16, 12, 13),
    ('Линия Пикселей', 'Черновик ночного формата', 'V', 20, 1, 17, 8, 9),
    ('Линия Пикселей', 'Архив: старая расстановка', 'H', 5, 1, 7, 3, 4),
    ('Линия Пикселей', 'Архив: старая расстановка', 'V', 8, 1, 13, 7, 8),
    ('Линия Пикселей', 'Архив: старая расстановка', 'V', 12, 2, 11, 6, 7);

CREATE TEMP TABLE demo_real_extra_wall_gaps (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    orientation text NOT NULL,
    col_no int NOT NULL,
    row_no int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_real_extra_wall_gaps (club_name, floorplan_name, orientation, col_no, row_no)
SELECT club_name, floorplan_name, 'H', 2, 1 FROM demo_real_floorplan_defs
UNION ALL
SELECT club_name, floorplan_name, 'H', 3, 1 FROM demo_real_floorplan_defs;

WITH floor_cells AS (
    SELECT
        d.club_name,
        d.floorplan_name,
        cols.col_no,
        rows.row_no,
        CASE
            WHEN EXISTS (
                SELECT 1
                FROM demo_real_vip_zones z
                WHERE z.club_name = d.club_name
                  AND z.floorplan_name = d.floorplan_name
                  AND cols.col_no BETWEEN z.col_from AND z.col_to
                  AND rows.row_no BETWEEN z.row_from AND z.row_to
            ) THEN 'VIP'
            ELSE 'REGULAR'
        END AS room_type
    FROM demo_real_floorplan_defs d
    CROSS JOIN LATERAL generate_series(1, d.max_col) AS cols(col_no)
    CROSS JOIN LATERAL generate_series(1, d.max_row) AS rows(row_no)
), manual_walls AS (
    SELECT
        s.club_name,
        s.floorplan_name,
        s.orientation,
        CASE WHEN s.orientation = 'H' THEN pos.no ELSE s.fixed_no END AS col_no,
        CASE WHEN s.orientation = 'H' THEN s.fixed_no ELSE pos.no END AS row_no
    FROM demo_real_wall_segments s
    CROSS JOIN LATERAL generate_series(s.from_no, s.to_no) AS pos(no)
), segment_gaps AS (
    SELECT
        s.club_name,
        s.floorplan_name,
        s.orientation,
        CASE WHEN s.orientation = 'H' THEN pos.no ELSE s.fixed_no END AS col_no,
        CASE WHEN s.orientation = 'H' THEN s.fixed_no ELSE pos.no END AS row_no
    FROM demo_real_wall_segments s
    CROSS JOIN LATERAL generate_series(s.gap_from, s.gap_to) AS pos(no)
    WHERE s.gap_from IS NOT NULL
      AND s.gap_to IS NOT NULL
), all_gaps AS (
    SELECT * FROM segment_gaps
    UNION
    SELECT * FROM demo_real_extra_wall_gaps
), auto_walls AS (
    SELECT f.club_name, f.floorplan_name, 'H'::text AS orientation, f.col_no, f.row_no
    FROM floor_cells f
    LEFT JOIN floor_cells n
        ON n.club_name = f.club_name
       AND n.floorplan_name = f.floorplan_name
       AND n.col_no = f.col_no
       AND n.row_no = f.row_no - 1
       AND n.room_type = f.room_type
    WHERE n.club_name IS NULL
    UNION
    SELECT f.club_name, f.floorplan_name, 'H'::text AS orientation, f.col_no, f.row_no + 1
    FROM floor_cells f
    LEFT JOIN floor_cells n
        ON n.club_name = f.club_name
       AND n.floorplan_name = f.floorplan_name
       AND n.col_no = f.col_no
       AND n.row_no = f.row_no + 1
       AND n.room_type = f.room_type
    WHERE n.club_name IS NULL
    UNION
    SELECT f.club_name, f.floorplan_name, 'V'::text AS orientation, f.col_no, f.row_no
    FROM floor_cells f
    LEFT JOIN floor_cells n
        ON n.club_name = f.club_name
       AND n.floorplan_name = f.floorplan_name
       AND n.col_no = f.col_no - 1
       AND n.row_no = f.row_no
       AND n.room_type = f.room_type
    WHERE n.club_name IS NULL
    UNION
    SELECT f.club_name, f.floorplan_name, 'V'::text AS orientation, f.col_no + 1, f.row_no
    FROM floor_cells f
    LEFT JOIN floor_cells n
        ON n.club_name = f.club_name
       AND n.floorplan_name = f.floorplan_name
       AND n.col_no = f.col_no + 1
       AND n.row_no = f.row_no
       AND n.room_type = f.room_type
    WHERE n.club_name IS NULL
), all_walls AS (
    SELECT DISTINCT w.club_name, w.floorplan_name, w.orientation, w.col_no, w.row_no
    FROM (
        SELECT * FROM auto_walls
        UNION ALL
        SELECT * FROM manual_walls
    ) w
    WHERE NOT EXISTS (
        SELECT 1
        FROM all_gaps g
        WHERE g.club_name = w.club_name
          AND g.floorplan_name = w.floorplan_name
          AND g.orientation = w.orientation
          AND g.col_no = w.col_no
          AND g.row_no = w.row_no
    )
), target_floorplans AS (
    SELECT
        fp.id,
        fp.name AS floorplan_name,
        c.name AS club_name,
        d.layout_key
    FROM club_floorplans fp
    JOIN clubs c ON c.id = fp.club_id
    JOIN club_staff cs ON cs.club_id = c.id AND cs.role = 'OWNER'
    JOIN users u ON u.id = cs.user_id AND u.phone = '+79991234567'
    JOIN demo_real_floorplan_defs d
        ON d.club_name = c.name
       AND d.floorplan_name = fp.name
), all_items AS (
    SELECT
        tf.id AS floorplan_id,
        10000 + f.row_no * 100 + f.col_no AS item_order,
        f.col_no,
        f.row_no,
        jsonb_build_object(
            'type', 'FLOOR',
            'col', f.col_no,
            'row', f.row_no,
            'roomType', f.room_type
        ) AS item
    FROM target_floorplans tf
    JOIN floor_cells f
        ON f.club_name = tf.club_name
       AND f.floorplan_name = tf.floorplan_name
    UNION ALL
    SELECT
        tf.id AS floorplan_id,
        20000 + s.item_order AS item_order,
        s.col_no,
        s.row_no,
        jsonb_build_object(
            'type', 'SEAT',
            'seatId', seat.id,
            'col', s.col_no,
            'row', s.row_no
        ) AS item
    FROM target_floorplans tf
    JOIN demo_real_seat_templates s ON s.layout_key = tf.layout_key
    JOIN clubs c ON c.name = tf.club_name
    JOIN seats seat
        ON seat.club_id = c.id
       AND seat.label = s.seat_label
    UNION ALL
    SELECT
        tf.id AS floorplan_id,
        30000 + w.row_no * 100 + w.col_no AS item_order,
        w.col_no,
        w.row_no,
        jsonb_build_object(
            'type', 'WALL',
            'orientation', w.orientation,
            'col', w.col_no,
            'row', w.row_no,
            'auto', true
        ) AS item
    FROM target_floorplans tf
    JOIN all_walls w
        ON w.club_name = tf.club_name
       AND w.floorplan_name = tf.floorplan_name
), item_json AS (
    SELECT
        floorplan_id,
        jsonb_agg(item ORDER BY item_order, col_no, row_no) AS items,
        MAX(col_no) AS max_col,
        MAX(row_no) AS max_row
    FROM all_items
    GROUP BY floorplan_id
)
UPDATE club_floorplans fp
SET width = (item_json.max_col + 2) * 32,
    height = (item_json.max_row + 2) * 32,
    grid_size = 32,
    version = fp.version + 1,
    data = jsonb_build_object('version', fp.version + 1, 'items', item_json.items),
    updated_at = localtimestamp
FROM item_json
WHERE fp.id = item_json.floorplan_id;
