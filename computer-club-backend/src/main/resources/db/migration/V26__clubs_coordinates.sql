alter table clubs
    add column if not exists latitude  double precision,
    add column if not exists longitude double precision;

-- проставляем координаты существующим клубам по их адресам
update clubs set latitude = 55.7623, longitude = 37.6079 where name = 'Cybersport Arena Central';
update clubs set latitude = 55.7897, longitude = 37.5590 where name = 'GG Hub North';
update clubs set latitude = 55.7422, longitude = 37.3886 where name = 'Pixel Zone West';
