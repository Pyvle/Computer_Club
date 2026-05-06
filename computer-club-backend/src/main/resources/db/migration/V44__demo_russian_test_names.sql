CREATE TEMP TABLE demo_test_name_map (
    old_name text,
    new_name text
) ON COMMIT DROP;

INSERT INTO demo_test_name_map (old_name, new_name) VALUES
    ('test', 'Пробный Клуб'),
    ('test1', 'Пробная Заявка Один'),
    ('test2', 'Пробная Заявка Два'),
    ('testtest', 'Пробная Заявка Дополнительная');

UPDATE clubs c
SET name = m.new_name,
    updated_at = localtimestamp
FROM demo_test_name_map m
WHERE c.name = m.old_name
  AND NOT EXISTS (
      SELECT 1
      FROM clubs existing
      WHERE existing.name = m.new_name
        AND existing.id <> c.id
  );

UPDATE club_applications a
SET club_name = m.new_name,
    updated_at = localtimestamp
FROM demo_test_name_map m
WHERE a.club_name = m.old_name;
