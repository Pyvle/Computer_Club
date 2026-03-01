insert into clubs (name, address, location_text, description, is_active)
values
('Cybersport Arena Central', 'Москва, ул. Тверская, 10', 'Центр', 'Большой зал, топовые ПК, VIP-зона', true),
('GG Hub North', 'Москва, Ленинградский проспект, 25', 'Север', 'Уютный клуб рядом с метро', true),
('Pixel Zone West', 'Москва, Кутузовский проспект, 48', 'Запад', 'Ночные турниры и выгодные пакеты', true)
on conflict do nothing;