CREATE TEMP TABLE demo_owner_full_floorplan_defs (
    club_name text NOT NULL,
    floorplan_name text NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_full_floorplan_defs (club_name, floorplan_name) VALUES
    ('Пробный Клуб', 'Основная схема'),
    ('Пробный Клуб', 'Черновик расширения'),
    ('Пробный Клуб', 'Черновик VIP-зоны'),
    ('Пробный Клуб', 'Архив: большая схема'),
    ('Пробный Клуб', 'Архив: компактная расстановка'),
    ('Пробный Клуб', 'Архив: линейная расстановка'),
    ('Пробный Клуб', 'Старая схема зала'),
    ('Северный Портал', 'Основная схема'),
    ('Северный Портал', 'Черновик турнира'),
    ('Северный Портал', 'Черновик стрим-зоны'),
    ('Северный Портал', 'Архив: старая расстановка'),
    ('Линия Пикселей', 'Основная схема'),
    ('Линия Пикселей', 'Черновик турнира'),
    ('Линия Пикселей', 'Черновик ночного формата'),
    ('Линия Пикселей', 'Архив: старая расстановка');

CREATE TEMP TABLE demo_owner_full_floorplan_rooms (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    room_order int NOT NULL,
    col_from int NOT NULL,
    col_to int NOT NULL,
    row_from int NOT NULL,
    row_to int NOT NULL,
    room_type text NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_full_floorplan_rooms (club_name, floorplan_name, room_order, col_from, col_to, row_from, row_to, room_type) VALUES
    ('Пробный Клуб', 'Основная схема', 10, 1, 5, 1, 3, 'REGULAR'),
    ('Пробный Клуб', 'Основная схема', 20, 6, 7, 1, 10, 'REGULAR'),
    ('Пробный Клуб', 'Основная схема', 30, 1, 7, 4, 10, 'REGULAR'),
    ('Пробный Клуб', 'Основная схема', 40, 9, 14, 1, 2, 'REGULAR'),
    ('Пробный Клуб', 'Основная схема', 50, 9, 14, 3, 10, 'VIP'),
    ('Пробный Клуб', 'Черновик расширения', 10, 1, 5, 1, 3, 'REGULAR'),
    ('Пробный Клуб', 'Черновик расширения', 20, 6, 8, 1, 11, 'REGULAR'),
    ('Пробный Клуб', 'Черновик расширения', 30, 1, 8, 4, 11, 'REGULAR'),
    ('Пробный Клуб', 'Черновик расширения', 40, 10, 15, 1, 5, 'VIP'),
    ('Пробный Клуб', 'Черновик расширения', 50, 10, 15, 7, 11, 'VIP'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20, 6, 7, 1, 9, 'REGULAR'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 30, 1, 7, 5, 9, 'REGULAR'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 40, 9, 14, 1, 4, 'VIP'),
    ('Пробный Клуб', 'Черновик VIP-зоны', 50, 9, 14, 6, 9, 'VIP'),
    ('Пробный Клуб', 'Архив: большая схема', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Пробный Клуб', 'Архив: большая схема', 20, 6, 8, 1, 12, 'REGULAR'),
    ('Пробный Клуб', 'Архив: большая схема', 30, 1, 8, 5, 12, 'REGULAR'),
    ('Пробный Клуб', 'Архив: большая схема', 40, 10, 17, 1, 6, 'VIP'),
    ('Пробный Клуб', 'Архив: большая схема', 50, 10, 17, 8, 12, 'REGULAR'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 10, 1, 4, 1, 3, 'REGULAR'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20, 5, 6, 1, 8, 'REGULAR'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 30, 1, 6, 4, 8, 'REGULAR'),
    ('Пробный Клуб', 'Архив: компактная расстановка', 40, 8, 11, 2, 8, 'VIP'),
    ('Пробный Клуб', 'Архив: линейная расстановка', 10, 1, 4, 1, 3, 'REGULAR'),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20, 5, 14, 2, 5, 'REGULAR'),
    ('Пробный Клуб', 'Архив: линейная расстановка', 30, 16, 19, 2, 5, 'VIP'),
    ('Пробный Клуб', 'Старая схема зала', 10, 1, 5, 1, 3, 'REGULAR'),
    ('Пробный Клуб', 'Старая схема зала', 20, 1, 7, 4, 7, 'REGULAR'),
    ('Пробный Клуб', 'Старая схема зала', 30, 9, 12, 3, 7, 'VIP'),

    ('Северный Портал', 'Основная схема', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Северный Портал', 'Основная схема', 20, 6, 8, 1, 15, 'REGULAR'),
    ('Северный Портал', 'Основная схема', 30, 1, 12, 5, 15, 'REGULAR'),
    ('Северный Портал', 'Основная схема', 40, 14, 19, 1, 6, 'VIP'),
    ('Северный Портал', 'Основная схема', 50, 14, 19, 8, 15, 'VIP'),
    ('Северный Портал', 'Основная схема', 60, 21, 24, 2, 6, 'REGULAR'),
    ('Северный Портал', 'Основная схема', 70, 21, 24, 9, 13, 'REGULAR'),
    ('Северный Портал', 'Черновик турнира', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Северный Портал', 'Черновик турнира', 20, 6, 8, 1, 16, 'REGULAR'),
    ('Северный Портал', 'Черновик турнира', 30, 1, 13, 5, 16, 'REGULAR'),
    ('Северный Портал', 'Черновик турнира', 40, 15, 20, 1, 5, 'VIP'),
    ('Северный Портал', 'Черновик турнира', 50, 15, 20, 7, 16, 'VIP'),
    ('Северный Портал', 'Черновик стрим-зоны', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Северный Портал', 'Черновик стрим-зоны', 20, 6, 8, 1, 15, 'REGULAR'),
    ('Северный Портал', 'Черновик стрим-зоны', 30, 1, 11, 5, 15, 'REGULAR'),
    ('Северный Портал', 'Черновик стрим-зоны', 40, 13, 19, 1, 7, 'VIP'),
    ('Северный Портал', 'Черновик стрим-зоны', 50, 13, 19, 9, 15, 'VIP'),
    ('Северный Портал', 'Черновик стрим-зоны', 60, 21, 24, 4, 12, 'REGULAR'),
    ('Северный Портал', 'Архив: старая расстановка', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Северный Портал', 'Архив: старая расстановка', 20, 6, 7, 1, 12, 'REGULAR'),
    ('Северный Портал', 'Архив: старая расстановка', 30, 1, 10, 5, 12, 'REGULAR'),
    ('Северный Портал', 'Архив: старая расстановка', 40, 12, 16, 2, 12, 'VIP'),

    ('Линия Пикселей', 'Основная схема', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Линия Пикселей', 'Основная схема', 20, 6, 8, 1, 15, 'REGULAR'),
    ('Линия Пикселей', 'Основная схема', 30, 1, 12, 5, 15, 'REGULAR'),
    ('Линия Пикселей', 'Основная схема', 40, 14, 19, 2, 7, 'VIP'),
    ('Линия Пикселей', 'Основная схема', 50, 14, 19, 9, 15, 'VIP'),
    ('Линия Пикселей', 'Основная схема', 60, 21, 24, 1, 4, 'REGULAR'),
    ('Линия Пикселей', 'Основная схема', 70, 21, 24, 10, 15, 'REGULAR'),
    ('Линия Пикселей', 'Черновик турнира', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Линия Пикселей', 'Черновик турнира', 20, 6, 8, 1, 16, 'REGULAR'),
    ('Линия Пикселей', 'Черновик турнира', 30, 1, 13, 5, 16, 'REGULAR'),
    ('Линия Пикселей', 'Черновик турнира', 40, 15, 20, 2, 6, 'VIP'),
    ('Линия Пикселей', 'Черновик турнира', 50, 15, 20, 8, 16, 'VIP'),
    ('Линия Пикселей', 'Черновик ночного формата', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Линия Пикселей', 'Черновик ночного формата', 20, 6, 8, 1, 15, 'REGULAR'),
    ('Линия Пикселей', 'Черновик ночного формата', 30, 1, 11, 5, 15, 'REGULAR'),
    ('Линия Пикселей', 'Черновик ночного формата', 40, 13, 19, 1, 7, 'VIP'),
    ('Линия Пикселей', 'Черновик ночного формата', 50, 13, 19, 9, 15, 'VIP'),
    ('Линия Пикселей', 'Черновик ночного формата', 60, 21, 24, 5, 12, 'REGULAR'),
    ('Линия Пикселей', 'Архив: старая расстановка', 10, 1, 5, 1, 4, 'REGULAR'),
    ('Линия Пикселей', 'Архив: старая расстановка', 20, 6, 7, 1, 12, 'REGULAR'),
    ('Линия Пикселей', 'Архив: старая расстановка', 30, 1, 10, 5, 12, 'REGULAR'),
    ('Линия Пикселей', 'Архив: старая расстановка', 40, 12, 16, 1, 11, 'VIP');

CREATE TEMP TABLE demo_owner_full_floorplan_seats (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    item_order int NOT NULL,
    seat_label text NOT NULL,
    col_no int NOT NULL,
    row_no int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_full_floorplan_seats (club_name, floorplan_name, item_order, seat_label, col_no, row_no) VALUES
    ('Пробный Клуб', 'Основная схема', 20001, '1', 2, 5),
    ('Пробный Клуб', 'Основная схема', 20002, '2', 4, 5),
    ('Пробный Клуб', 'Основная схема', 20003, '3', 2, 8),
    ('Пробный Клуб', 'Основная схема', 20004, '4', 11, 5),
    ('Пробный Клуб', 'Основная схема', 20005, 'V-1', 12, 8),
    ('Пробный Клуб', 'Черновик расширения', 20101, '1', 2, 5),
    ('Пробный Клуб', 'Черновик расширения', 20102, '2', 4, 5),
    ('Пробный Клуб', 'Черновик расширения', 20103, '3', 6, 8),
    ('Пробный Клуб', 'Черновик расширения', 20104, '4', 12, 3),
    ('Пробный Клуб', 'Черновик расширения', 20105, 'V-1', 14, 9),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20201, '1', 2, 6),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20202, '2', 4, 6),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20203, '3', 2, 8),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20204, '4', 11, 2),
    ('Пробный Клуб', 'Черновик VIP-зоны', 20205, 'V-1', 11, 7),
    ('Пробный Клуб', 'Архив: большая схема', 20301, '1', 2, 6),
    ('Пробный Клуб', 'Архив: большая схема', 20302, '2', 4, 6),
    ('Пробный Клуб', 'Архив: большая схема', 20303, '3', 6, 9),
    ('Пробный Клуб', 'Архив: большая схема', 20304, '4', 13, 3),
    ('Пробный Клуб', 'Архив: большая схема', 20305, 'V-1', 16, 5),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20401, '1', 2, 5),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20402, '2', 4, 5),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20403, '3', 2, 7),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20404, '4', 9, 3),
    ('Пробный Клуб', 'Архив: компактная расстановка', 20405, 'V-1', 9, 6),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20501, '1', 5, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20502, '2', 7, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20503, '3', 9, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20504, '4', 16, 3),
    ('Пробный Клуб', 'Архив: линейная расстановка', 20505, 'V-1', 18, 3),
    ('Пробный Клуб', 'Старая схема зала', 20601, '1', 2, 5),
    ('Пробный Клуб', 'Старая схема зала', 20602, '2', 4, 5),
    ('Пробный Клуб', 'Старая схема зала', 20603, '3', 6, 6),
    ('Пробный Клуб', 'Старая схема зала', 20604, '4', 10, 4),
    ('Пробный Клуб', 'Старая схема зала', 20605, 'V-1', 10, 6),

    ('Северный Портал', 'Основная схема', 21001, 'A1', 2, 6),
    ('Северный Портал', 'Основная схема', 21002, 'A2', 4, 6),
    ('Северный Портал', 'Основная схема', 21003, 'A3', 2, 9),
    ('Северный Портал', 'Основная схема', 21004, 'A4', 4, 9),
    ('Северный Портал', 'Основная схема', 21005, 'B1', 7, 6),
    ('Северный Портал', 'Основная схема', 21006, 'B2', 7, 9),
    ('Северный Портал', 'Основная схема', 21007, 'C1', 10, 6),
    ('Северный Портал', 'Основная схема', 21008, 'C2', 10, 9),
    ('Северный Портал', 'Основная схема', 21009, 'C3', 2, 13),
    ('Северный Портал', 'Основная схема', 21010, 'C4', 4, 13),
    ('Северный Портал', 'Основная схема', 21011, 'V1', 15, 3),
    ('Северный Портал', 'Основная схема', 21012, 'V2', 18, 4),
    ('Северный Портал', 'Основная схема', 21013, 'V3', 15, 10),
    ('Северный Портал', 'Основная схема', 21014, 'V4', 18, 13),
    ('Северный Портал', 'Черновик турнира', 21101, 'A1', 2, 6),
    ('Северный Портал', 'Черновик турнира', 21102, 'A2', 4, 6),
    ('Северный Портал', 'Черновик турнира', 21103, 'A3', 6, 6),
    ('Северный Портал', 'Черновик турнира', 21104, 'A4', 8, 6),
    ('Северный Портал', 'Черновик турнира', 21105, 'B1', 10, 8),
    ('Северный Портал', 'Черновик турнира', 21106, 'B2', 10, 12),
    ('Северный Портал', 'Черновик турнира', 21107, 'C1', 2, 14),
    ('Северный Портал', 'Черновик турнира', 21108, 'C2', 4, 14),
    ('Северный Портал', 'Черновик турнира', 21109, 'C3', 6, 14),
    ('Северный Портал', 'Черновик турнира', 21110, 'C4', 8, 14),
    ('Северный Портал', 'Черновик турнира', 21111, 'V1', 16, 2),
    ('Северный Портал', 'Черновик турнира', 21112, 'V2', 18, 4),
    ('Северный Портал', 'Черновик турнира', 21113, 'V3', 16, 10),
    ('Северный Портал', 'Черновик турнира', 21114, 'V4', 18, 14),
    ('Северный Портал', 'Черновик стрим-зоны', 21201, 'A1', 2, 6),
    ('Северный Портал', 'Черновик стрим-зоны', 21202, 'A2', 4, 6),
    ('Северный Портал', 'Черновик стрим-зоны', 21203, 'A3', 6, 8),
    ('Северный Портал', 'Черновик стрим-зоны', 21204, 'A4', 8, 8),
    ('Северный Портал', 'Черновик стрим-зоны', 21205, 'B1', 2, 12),
    ('Северный Портал', 'Черновик стрим-зоны', 21206, 'B2', 4, 12),
    ('Северный Портал', 'Черновик стрим-зоны', 21207, 'C1', 7, 13),
    ('Северный Портал', 'Черновик стрим-зоны', 21208, 'C2', 9, 13),
    ('Северный Портал', 'Черновик стрим-зоны', 21209, 'C3', 14, 3),
    ('Северный Портал', 'Черновик стрим-зоны', 21210, 'C4', 17, 3),
    ('Северный Портал', 'Черновик стрим-зоны', 21211, 'V1', 14, 11),
    ('Северный Портал', 'Черновик стрим-зоны', 21212, 'V2', 17, 11),
    ('Северный Портал', 'Черновик стрим-зоны', 21213, 'V3', 14, 14),
    ('Северный Портал', 'Черновик стрим-зоны', 21214, 'V4', 17, 14),
    ('Северный Портал', 'Архив: старая расстановка', 21301, 'A1', 2, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21302, 'A2', 4, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21303, 'A3', 6, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21304, 'A4', 2, 9),
    ('Северный Портал', 'Архив: старая расстановка', 21305, 'B1', 4, 9),
    ('Северный Портал', 'Архив: старая расстановка', 21306, 'B2', 6, 9),
    ('Северный Портал', 'Архив: старая расстановка', 21307, 'C1', 8, 6),
    ('Северный Портал', 'Архив: старая расстановка', 21308, 'C2', 8, 9),
    ('Северный Портал', 'Архив: старая расстановка', 21309, 'C3', 2, 11),
    ('Северный Портал', 'Архив: старая расстановка', 21310, 'C4', 4, 11),
    ('Северный Портал', 'Архив: старая расстановка', 21311, 'V1', 13, 4),
    ('Северный Портал', 'Архив: старая расстановка', 21312, 'V2', 15, 5),
    ('Северный Портал', 'Архив: старая расстановка', 21313, 'V3', 13, 9),
    ('Северный Портал', 'Архив: старая расстановка', 21314, 'V4', 15, 11),

    ('Линия Пикселей', 'Основная схема', 22001, 'A1', 2, 6),
    ('Линия Пикселей', 'Основная схема', 22002, 'A2', 4, 6),
    ('Линия Пикселей', 'Основная схема', 22003, 'A3', 7, 6),
    ('Линия Пикселей', 'Основная схема', 22004, 'A4', 10, 6),
    ('Линия Пикселей', 'Основная схема', 22005, 'B1', 2, 10),
    ('Линия Пикселей', 'Основная схема', 22006, 'B2', 4, 10),
    ('Линия Пикселей', 'Основная схема', 22007, 'C1', 7, 12),
    ('Линия Пикселей', 'Основная схема', 22008, 'C2', 10, 12),
    ('Линия Пикселей', 'Основная схема', 22009, 'C3', 2, 14),
    ('Линия Пикселей', 'Основная схема', 22010, 'C4', 4, 14),
    ('Линия Пикселей', 'Основная схема', 22011, 'V1', 15, 4),
    ('Линия Пикселей', 'Основная схема', 22012, 'V2', 18, 5),
    ('Линия Пикселей', 'Основная схема', 22013, 'V3', 15, 11),
    ('Линия Пикселей', 'Основная схема', 22014, 'V4', 18, 13),
    ('Линия Пикселей', 'Черновик турнира', 22101, 'A1', 2, 6),
    ('Линия Пикселей', 'Черновик турнира', 22102, 'A2', 4, 6),
    ('Линия Пикселей', 'Черновик турнира', 22103, 'A3', 6, 6),
    ('Линия Пикселей', 'Черновик турнира', 22104, 'A4', 8, 6),
    ('Линия Пикселей', 'Черновик турнира', 22105, 'B1', 10, 8),
    ('Линия Пикселей', 'Черновик турнира', 22106, 'B2', 10, 12),
    ('Линия Пикселей', 'Черновик турнира', 22107, 'C1', 2, 14),
    ('Линия Пикселей', 'Черновик турнира', 22108, 'C2', 4, 14),
    ('Линия Пикселей', 'Черновик турнира', 22109, 'C3', 6, 14),
    ('Линия Пикселей', 'Черновик турнира', 22110, 'C4', 8, 14),
    ('Линия Пикселей', 'Черновик турнира', 22111, 'V1', 16, 3),
    ('Линия Пикселей', 'Черновик турнира', 22112, 'V2', 18, 5),
    ('Линия Пикселей', 'Черновик турнира', 22113, 'V3', 16, 11),
    ('Линия Пикселей', 'Черновик турнира', 22114, 'V4', 18, 14),
    ('Линия Пикселей', 'Черновик ночного формата', 22201, 'A1', 2, 6),
    ('Линия Пикселей', 'Черновик ночного формата', 22202, 'A2', 4, 6),
    ('Линия Пикселей', 'Черновик ночного формата', 22203, 'A3', 6, 8),
    ('Линия Пикселей', 'Черновик ночного формата', 22204, 'A4', 8, 8),
    ('Линия Пикселей', 'Черновик ночного формата', 22205, 'B1', 2, 12),
    ('Линия Пикселей', 'Черновик ночного формата', 22206, 'B2', 4, 12),
    ('Линия Пикселей', 'Черновик ночного формата', 22207, 'C1', 7, 13),
    ('Линия Пикселей', 'Черновик ночного формата', 22208, 'C2', 9, 13),
    ('Линия Пикселей', 'Черновик ночного формата', 22209, 'C3', 14, 3),
    ('Линия Пикселей', 'Черновик ночного формата', 22210, 'C4', 17, 3),
    ('Линия Пикселей', 'Черновик ночного формата', 22211, 'V1', 14, 11),
    ('Линия Пикселей', 'Черновик ночного формата', 22212, 'V2', 17, 11),
    ('Линия Пикселей', 'Черновик ночного формата', 22213, 'V3', 14, 14),
    ('Линия Пикселей', 'Черновик ночного формата', 22214, 'V4', 17, 14),
    ('Линия Пикселей', 'Архив: старая расстановка', 22301, 'A1', 2, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22302, 'A2', 4, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22303, 'A3', 6, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22304, 'A4', 2, 9),
    ('Линия Пикселей', 'Архив: старая расстановка', 22305, 'B1', 4, 9),
    ('Линия Пикселей', 'Архив: старая расстановка', 22306, 'B2', 6, 9),
    ('Линия Пикселей', 'Архив: старая расстановка', 22307, 'C1', 8, 6),
    ('Линия Пикселей', 'Архив: старая расстановка', 22308, 'C2', 8, 9),
    ('Линия Пикселей', 'Архив: старая расстановка', 22309, 'C3', 2, 11),
    ('Линия Пикселей', 'Архив: старая расстановка', 22310, 'C4', 4, 11),
    ('Линия Пикселей', 'Архив: старая расстановка', 22311, 'V1', 13, 3),
    ('Линия Пикселей', 'Архив: старая расстановка', 22312, 'V2', 15, 4),
    ('Линия Пикселей', 'Архив: старая расстановка', 22313, 'V3', 13, 8),
    ('Линия Пикселей', 'Архив: старая расстановка', 22314, 'V4', 15, 10);

CREATE TEMP TABLE demo_owner_full_wall_segments (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    orientation text NOT NULL,
    fixed_no int NOT NULL,
    from_no int NOT NULL,
    to_no int NOT NULL,
    gap_from int,
    gap_to int
) ON COMMIT DROP;

INSERT INTO demo_owner_full_wall_segments (club_name, floorplan_name, orientation, fixed_no, from_no, to_no, gap_from, gap_to)
SELECT club_name, floorplan_name, 'H', 4, 1, 5, 3, 3
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб'
UNION ALL
SELECT club_name, floorplan_name, 'V', 8, 2, 10, 5, 6
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб'
UNION ALL
SELECT club_name, floorplan_name, 'V', 13, 3, 10, 5, 6
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб' AND floorplan_name IN ('Черновик расширения', 'Архив: большая схема')
UNION ALL
SELECT club_name, floorplan_name, 'H', 7, 10, 15, 13, 13
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб' AND floorplan_name = 'Черновик расширения'
UNION ALL
SELECT club_name, floorplan_name, 'H', 6, 9, 14, 11, 12
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб' AND floorplan_name = 'Черновик VIP-зоны'
UNION ALL
SELECT club_name, floorplan_name, 'V', 13, 1, 15, 4, 5
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей')
UNION ALL
SELECT club_name, floorplan_name, 'V', 20, 2, 15, 7, 8
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей')
UNION ALL
SELECT club_name, floorplan_name, 'H', 5, 1, 5, 3, 3
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей')
UNION ALL
SELECT club_name, floorplan_name, 'H', 8, 14, 19, 16, 17
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей') AND floorplan_name IN ('Основная схема', 'Черновик стрим-зоны', 'Черновик ночного формата')
UNION ALL
SELECT club_name, floorplan_name, 'H', 7, 15, 20, 17, 18
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей') AND floorplan_name = 'Черновик турнира'
UNION ALL
SELECT club_name, floorplan_name, 'V', 9, 5, 16, 8, 9
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей') AND floorplan_name IN ('Черновик турнира');

CREATE TEMP TABLE demo_owner_full_wall_gaps (
    club_name text NOT NULL,
    floorplan_name text NOT NULL,
    orientation text NOT NULL,
    col_no int NOT NULL,
    row_no int NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_owner_full_wall_gaps (club_name, floorplan_name, orientation, col_no, row_no)
SELECT club_name, floorplan_name, 'H', 2, 1
FROM demo_owner_full_floorplan_defs
UNION ALL
SELECT club_name, floorplan_name, 'H', 3, 1
FROM demo_owner_full_floorplan_defs
UNION ALL
SELECT club_name, floorplan_name, 'V', 6, 3
FROM demo_owner_full_floorplan_defs
UNION ALL
SELECT club_name, floorplan_name, 'V', 13, 5
FROM demo_owner_full_floorplan_defs
WHERE club_name IN ('Северный Портал', 'Линия Пикселей')
UNION ALL
SELECT club_name, floorplan_name, 'V', 14, 4
FROM demo_owner_full_floorplan_defs
WHERE club_name = 'Пробный Клуб';

WITH floor_cells AS (
    SELECT DISTINCT
        r.club_name,
        r.floorplan_name,
        cols.col_no,
        rows.row_no,
        r.room_type
    FROM demo_owner_full_floorplan_rooms r
    CROSS JOIN LATERAL generate_series(r.col_from, r.col_to) AS cols(col_no)
    CROSS JOIN LATERAL generate_series(r.row_from, r.row_to) AS rows(row_no)
), manual_walls AS (
    SELECT
        s.club_name,
        s.floorplan_name,
        s.orientation,
        CASE WHEN s.orientation = 'H' THEN pos.no ELSE s.fixed_no END AS col_no,
        CASE WHEN s.orientation = 'H' THEN s.fixed_no ELSE pos.no END AS row_no
    FROM demo_owner_full_wall_segments s
    CROSS JOIN LATERAL generate_series(s.from_no, s.to_no) AS pos(no)
    WHERE s.gap_from IS NULL
       OR pos.no < s.gap_from
       OR pos.no > s.gap_to
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
        FROM demo_owner_full_wall_gaps g
        WHERE g.club_name = w.club_name
          AND g.floorplan_name = w.floorplan_name
          AND g.orientation = w.orientation
          AND g.col_no = w.col_no
          AND g.row_no = w.row_no
    )
), target_floorplans AS (
    SELECT fp.id, fp.name AS floorplan_name, c.name AS club_name
    FROM club_floorplans fp
    JOIN clubs c ON c.id = fp.club_id
    JOIN club_staff cs ON cs.club_id = c.id AND cs.role = 'OWNER'
    JOIN users u ON u.id = cs.user_id AND u.phone = '+79991234567'
    JOIN demo_owner_full_floorplan_defs d
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
    UNION ALL
    SELECT
        tf.id AS floorplan_id,
        s.item_order,
        s.col_no,
        s.row_no,
        jsonb_build_object(
            'type', 'SEAT',
            'seatId', seat.id,
            'col', s.col_no,
            'row', s.row_no
        ) AS item
    FROM target_floorplans tf
    JOIN demo_owner_full_floorplan_seats s
        ON s.club_name = tf.club_name
       AND s.floorplan_name = tf.floorplan_name
    JOIN clubs c ON c.name = s.club_name
    JOIN seats seat
        ON seat.club_id = c.id
       AND seat.label = s.seat_label
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
