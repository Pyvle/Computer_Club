WITH drink_category AS (
    SELECT id
    FROM product_categories
    WHERE title = 'Напитки'
    ORDER BY id
    LIMIT 1
),
catalog_updates(old_title, new_title, new_description, new_image_url) AS (
    VALUES
        (
            'Кола 0.5 л',
            'Добрый Кола 0.5 л',
            'Газированный напиток Добрый Кола 0.5 л в пластиковой бутылке.',
            'https://catalog-cdn.detmir.st/media/sNNhYqKsc2crFnZPzAEEGxgr88T_tgn78FaPznVdX84%3D.webp?preset=site_product_gallery_r1500'
        ),
        (
            'Кола Zero 0.5 л',
            'Добрый Кола без сахара 0.5 л',
            'Газированный напиток Добрый Кола без сахара 0.5 л.',
            'https://catalog-cdn.detmir.st/media/sNNhYqKsc2crFnZPzAEEGxgr88T_tgn78FaPznVdX84%3D.webp?preset=site_product_gallery_r1500'
        ),
        (
            'Энергетик Drive 0.45 л',
            'Сок яблочный 0.5 л',
            'Осветленный яблочный сок в индивидуальной бутылке, подходит для подростков.',
            'https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=600&q=80'
        ),
        (
            'Энергетик Adrenaline 0.449 л',
            'Сок апельсиновый 0.5 л',
            'Апельсиновый сок в индивидуальной бутылке, без энергетических добавок.',
            'https://images.unsplash.com/photo-1613478223719-2ab802602423?auto=format&fit=crop&w=600&q=80'
        ),
        (
            'Энергетик 0.45',
            'Морс ягодный 0.5 л',
            'Ягодный морс в бутылке, без кофеина и энергетических добавок.',
            'https://images.unsplash.com/photo-1623065422902-30a2d299bbe4?auto=format&fit=crop&w=600&q=80'
        )
)
UPDATE products p
SET category_id = dc.id,
    title = u.new_title,
    description = u.new_description,
    image_url = u.new_image_url,
    is_active = true
FROM catalog_updates u
CROSS JOIN drink_category dc
WHERE p.title = u.old_title;

UPDATE club_products cp
SET price_rub = CASE p.title
        WHEN 'Добрый Кола 0.5 л' THEN LEAST(cp.price_rub, 130)
        WHEN 'Добрый Кола без сахара 0.5 л' THEN LEAST(cp.price_rub, 130)
        WHEN 'Сок яблочный 0.5 л' THEN 120
        WHEN 'Сок апельсиновый 0.5 л' THEN 120
        WHEN 'Морс ягодный 0.5 л' THEN 110
        ELSE cp.price_rub
    END,
    is_available = true
FROM products p
WHERE cp.product_id = p.id
  AND p.title IN (
      'Добрый Кола 0.5 л',
      'Добрый Кола без сахара 0.5 л',
      'Сок яблочный 0.5 л',
      'Сок апельсиновый 0.5 л',
      'Морс ягодный 0.5 л'
  );
