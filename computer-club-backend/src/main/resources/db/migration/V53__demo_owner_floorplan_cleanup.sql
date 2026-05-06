WITH probny AS (
    SELECT id FROM clubs WHERE name = 'Пробный Клуб'
)
UPDATE club_floorplans fp
SET name = 'Архив: большая схема',
    updated_at = localtimestamp
FROM probny c
WHERE fp.club_id = c.id
  AND fp.name = 'Основная схема'
  AND fp.status = 'ARCHIVED';

WITH probny AS (
    SELECT id FROM clubs WHERE name = 'Пробный Клуб'
)
UPDATE club_floorplans fp
SET width = 900,
    height = 620,
    grid_size = 10,
    status = 'DRAFT',
    version = GREATEST(fp.version, 2),
    data = '{"version":2,"items":[{"id":"wall-north","type":"wall","x":0,"y":0,"w":900,"h":20},{"id":"wall-south","type":"wall","x":0,"y":600,"w":900,"h":20},{"id":"reception","type":"furniture","x":50,"y":460,"w":160,"h":70,"label":"Стойка"},{"id":"seat-1","type":"seat","seatLabel":"1","x":90,"y":90,"w":70,"h":70},{"id":"seat-2","type":"seat","seatLabel":"2","x":180,"y":90,"w":70,"h":70},{"id":"seat-3","type":"seat","seatLabel":"3","x":270,"y":90,"w":70,"h":70},{"id":"seat-4","type":"seat","seatLabel":"4","x":360,"y":90,"w":70,"h":70},{"id":"seat-v1","type":"seat","seatLabel":"V-1","x":620,"y":120,"w":100,"h":100},{"id":"planned-zone","type":"furniture","x":80,"y":330,"w":300,"h":90,"label":"Планируемая зона"}]}'::jsonb,
    updated_at = localtimestamp
FROM probny c
WHERE fp.club_id = c.id
  AND fp.name = 'Черновик расширения';
