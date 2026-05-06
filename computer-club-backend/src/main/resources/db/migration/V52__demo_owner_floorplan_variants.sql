WITH renamed(club_name, old_name, new_name) AS (
    VALUES
        ('Пробный Клуб', 'test', 'Старая схема зала'),
        ('Пробный Клуб', 'test2', 'Архив: компактная расстановка'),
        ('Пробный Клуб', 'test3', 'Основная схема'),
        ('Пробный Клуб', 'text4', 'Черновик расширения')
)
UPDATE club_floorplans fp
SET name = r.new_name,
    updated_at = localtimestamp
FROM renamed r
JOIN clubs c ON c.name = r.club_name
WHERE fp.club_id = c.id
  AND fp.name = r.old_name;

WITH published_data(club_name, width, height, data) AS (
    VALUES
        ('Пробный Клуб', 720, 520, '{"version":3,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":720,"h":20},{"id":"wall-south","type":"wall","x":0,"y":500,"w":720,"h":20},{"id":"reception","type":"furniture","x":40,"y":390,"w":150,"h":70,"label":"Стойка"},{"id":"sofa","type":"furniture","x":500,"y":390,"w":150,"h":70,"label":"Ожидание"},{"id":"seat-1","type":"seat","seatLabel":"1","x":80,"y":80,"w":72,"h":72},{"id":"seat-2","type":"seat","seatLabel":"2","x":180,"y":80,"w":72,"h":72},{"id":"seat-3","type":"seat","seatLabel":"3","x":280,"y":80,"w":72,"h":72},{"id":"seat-4","type":"seat","seatLabel":"4","x":380,"y":80,"w":72,"h":72},{"id":"seat-v1","type":"seat","seatLabel":"V-1","x":520,"y":190,"w":96,"h":96}]}'::jsonb),
        ('Северный Портал', 1000, 640, '{"version":3,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":1000,"h":20},{"id":"wall-south","type":"wall","x":0,"y":620,"w":1000,"h":20},{"id":"reception","type":"furniture","x":40,"y":480,"w":170,"h":70,"label":"Стойка"},{"id":"lounge","type":"furniture","x":700,"y":480,"w":210,"h":70,"label":"Зона ожидания"},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":80,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":80,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":270,"y":80,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":360,"y":80,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":220,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":220,"w":70,"h":70},{"id":"seat-c1","type":"seat","seatLabel":"C1","x":90,"y":360,"w":70,"h":70},{"id":"seat-c2","type":"seat","seatLabel":"C2","x":180,"y":360,"w":70,"h":70},{"id":"seat-c3","type":"seat","seatLabel":"C3","x":270,"y":360,"w":70,"h":70},{"id":"seat-c4","type":"seat","seatLabel":"C4","x":360,"y":360,"w":70,"h":70},{"id":"vip-wall","type":"wall","x":630,"y":60,"w":20,"h":360},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":700,"y":90,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":820,"y":90,"w":90,"h":90},{"id":"seat-v3","type":"seat","seatLabel":"V3","x":700,"y":250,"w":90,"h":90},{"id":"seat-v4","type":"seat","seatLabel":"V4","x":820,"y":250,"w":90,"h":90}]}'::jsonb),
        ('Линия Пикселей', 980, 620, '{"version":3,"items":[{"id":"wall-left","type":"wall","x":0,"y":0,"w":20,"h":620},{"id":"wall-right","type":"wall","x":960,"y":0,"w":20,"h":620},{"id":"stage","type":"furniture","x":370,"y":470,"w":240,"h":70,"label":"Турнирная зона"},{"id":"desk","type":"furniture","x":735,"y":470,"w":175,"h":70,"label":"Ресепшн"},{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":90,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":90,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":270,"y":90,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":360,"y":90,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":230,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":230,"w":70,"h":70},{"id":"seat-c1","type":"seat","seatLabel":"C1","x":90,"y":370,"w":70,"h":70},{"id":"seat-c2","type":"seat","seatLabel":"C2","x":180,"y":370,"w":70,"h":70},{"id":"seat-c3","type":"seat","seatLabel":"C3","x":270,"y":370,"w":70,"h":70},{"id":"seat-c4","type":"seat","seatLabel":"C4","x":360,"y":370,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":650,"y":90,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":780,"y":90,"w":90,"h":90},{"id":"seat-v3","type":"seat","seatLabel":"V3","x":650,"y":250,"w":90,"h":90},{"id":"seat-v4","type":"seat","seatLabel":"V4","x":780,"y":250,"w":90,"h":90}]}'::jsonb)
)
UPDATE club_floorplans fp
SET width = p.width,
    height = p.height,
    grid_size = 10,
    version = GREATEST(fp.version, 3),
    data = p.data,
    updated_at = localtimestamp
FROM published_data p
JOIN clubs c ON c.name = p.club_name
WHERE fp.club_id = c.id
  AND fp.status = 'PUBLISHED';

CREATE TEMP TABLE demo_owner_draft_floorplans (
    club_name text,
    name text,
    width int,
    height int,
    data jsonb
) ON COMMIT DROP;

INSERT INTO demo_owner_draft_floorplans (club_name, name, width, height, data) VALUES
('Пробный Клуб', 'Черновик расширения', 900, 620, '{"version":1,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":900,"h":20},{"id":"seat-1","type":"seat","seatLabel":"1","x":90,"y":90,"w":70,"h":70},{"id":"seat-2","type":"seat","seatLabel":"2","x":180,"y":90,"w":70,"h":70},{"id":"seat-3","type":"seat","seatLabel":"3","x":270,"y":90,"w":70,"h":70},{"id":"seat-4","type":"seat","seatLabel":"4","x":360,"y":90,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V-1","x":620,"y":120,"w":100,"h":100},{"id":"planned-zone","type":"furniture","x":80,"y":330,"w":300,"h":90,"label":"Планируемая зона"}]}'::jsonb),
('Северный Портал', 'Черновик турнира', 1100, 700, '{"version":1,"items":[{"id":"stage","type":"furniture","x":420,"y":40,"w":260,"h":90,"label":"Сцена"},{"id":"team-a1","type":"seat","seatLabel":"A1","x":120,"y":180,"w":70,"h":70},{"id":"team-a2","type":"seat","seatLabel":"A2","x":220,"y":180,"w":70,"h":70},{"id":"team-a3","type":"seat","seatLabel":"A3","x":320,"y":180,"w":70,"h":70},{"id":"team-a4","type":"seat","seatLabel":"A4","x":420,"y":180,"w":70,"h":70},{"id":"team-b1","type":"seat","seatLabel":"B1","x":620,"y":180,"w":70,"h":70},{"id":"team-b2","type":"seat","seatLabel":"B2","x":720,"y":180,"w":70,"h":70},{"id":"team-c1","type":"seat","seatLabel":"C1","x":120,"y":340,"w":70,"h":70},{"id":"team-c2","type":"seat","seatLabel":"C2","x":220,"y":340,"w":70,"h":70},{"id":"stream","type":"seat","seatLabel":"V1","x":780,"y":360,"w":95,"h":95},{"id":"stream2","type":"seat","seatLabel":"V2","x":900,"y":360,"w":95,"h":95}]}'::jsonb),
('Линия Пикселей', 'Черновик турнира', 1100, 700, '{"version":1,"items":[{"id":"bracket","type":"furniture","x":430,"y":40,"w":240,"h":80,"label":"Турнирная сетка"},{"id":"row-a1","type":"seat","seatLabel":"A1","x":100,"y":170,"w":70,"h":70},{"id":"row-a2","type":"seat","seatLabel":"A2","x":190,"y":170,"w":70,"h":70},{"id":"row-a3","type":"seat","seatLabel":"A3","x":280,"y":170,"w":70,"h":70},{"id":"row-a4","type":"seat","seatLabel":"A4","x":370,"y":170,"w":70,"h":70},{"id":"row-b1","type":"seat","seatLabel":"B1","x":100,"y":300,"w":70,"h":70},{"id":"row-b2","type":"seat","seatLabel":"B2","x":190,"y":300,"w":70,"h":70},{"id":"row-c1","type":"seat","seatLabel":"C1","x":280,"y":300,"w":70,"h":70},{"id":"row-c2","type":"seat","seatLabel":"C2","x":370,"y":300,"w":70,"h":70},{"id":"vip-stream","type":"seat","seatLabel":"V3","x":760,"y":220,"w":95,"h":95},{"id":"vip-stream2","type":"seat","seatLabel":"V4","x":880,"y":220,"w":95,"h":95}]}'::jsonb);

INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT c.id, d.name, d.width, d.height, 10, 'DRAFT', 1, d.data, localtimestamp - interval '2 days', localtimestamp
FROM demo_owner_draft_floorplans d
JOIN clubs c ON c.name = d.club_name
WHERE NOT EXISTS (
    SELECT 1 FROM club_floorplans existing
    WHERE existing.club_id = c.id AND existing.name = d.name
);

UPDATE club_floorplans fp
SET width = d.width,
    height = d.height,
    data = d.data,
    updated_at = localtimestamp
FROM demo_owner_draft_floorplans d
JOIN clubs c ON c.name = d.club_name
WHERE fp.club_id = c.id
  AND fp.name = d.name
  AND fp.status = 'DRAFT';

WITH archived_data(club_name, name, width, height, data, version) AS (
    VALUES
        ('Пробный Клуб', 'Архив: линейная расстановка', 620, 420, '{"version":1,"items":[{"id":"seat-1","type":"seat","seatLabel":"1","x":80,"y":90,"w":70,"h":70},{"id":"seat-2","type":"seat","seatLabel":"2","x":170,"y":90,"w":70,"h":70},{"id":"seat-3","type":"seat","seatLabel":"3","x":260,"y":90,"w":70,"h":70},{"id":"seat-4","type":"seat","seatLabel":"4","x":350,"y":90,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V-1","x":450,"y":230,"w":90,"h":90}]}'::jsonb, 1),
        ('Северный Портал', 'Архив: старая расстановка', 900, 540, '{"version":1,"items":[{"id":"seat-a1","type":"seat","seatLabel":"A1","x":80,"y":80,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":170,"y":80,"w":70,"h":70},{"id":"seat-a3","type":"seat","seatLabel":"A3","x":260,"y":80,"w":70,"h":70},{"id":"seat-a4","type":"seat","seatLabel":"A4","x":350,"y":80,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":610,"y":290,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":720,"y":290,"w":90,"h":90}]}'::jsonb, 1),
        ('Линия Пикселей', 'Архив: старая расстановка', 900, 540, '{"version":1,"items":[{"id":"seat-a1","type":"seat","seatLabel":"A1","x":90,"y":90,"w":70,"h":70},{"id":"seat-a2","type":"seat","seatLabel":"A2","x":180,"y":90,"w":70,"h":70},{"id":"seat-b1","type":"seat","seatLabel":"B1","x":90,"y":220,"w":70,"h":70},{"id":"seat-b2","type":"seat","seatLabel":"B2","x":180,"y":220,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V1","x":650,"y":120,"w":90,"h":90},{"id":"seat-v2","type":"seat","seatLabel":"V2","x":650,"y":240,"w":90,"h":90}]}'::jsonb, 1)
)
INSERT INTO club_floorplans (club_id, name, width, height, grid_size, status, version, data, created_at, updated_at)
SELECT c.id, a.name, a.width, a.height, 10, 'ARCHIVED', a.version, a.data, localtimestamp - interval '30 days', localtimestamp - interval '20 days'
FROM archived_data a
JOIN clubs c ON c.name = a.club_name
WHERE NOT EXISTS (
    SELECT 1 FROM club_floorplans existing
    WHERE existing.club_id = c.id AND existing.name = a.name
);
