create table if not exists product_categories (
    id bigserial primary key,
    title varchar(120) not null,
    sort_order int not null default 0,
    is_active boolean not null default true
);

create table if not exists products (
    id bigserial primary key,
    category_id bigint not null references product_categories(id),
    title varchar(160) not null,
    description text,
    is_active boolean not null default true
);

create table if not exists club_products (
    club_id bigint not null references clubs(id) on delete cascade,
    product_id bigint not null references products(id) on delete cascade,
    price_rub int not null check (price_rub >= 0),
    is_available boolean not null default true,
    primary key (club_id, product_id)
);

create index if not exists idx_product_categories_active_sort
    on product_categories (is_active, sort_order, id);

create index if not exists idx_products_category_active
    on products (category_id, is_active, id);

create index if not exists idx_club_products_club_available
    on club_products (club_id, is_available, product_id);

-- seed categories
insert into product_categories (title, sort_order, is_active) values
('Напитки', 1, true),
('Снеки', 2, true),
('Горячее', 3, true)
on conflict do nothing;

-- seed products
insert into products (category_id, title, description, is_active)
select c.id, x.title, x.description, true
from (
  values
    ('Напитки', 'Кола 0.5', 'Охлажденный напиток'),
    ('Напитки', 'Энергетик 0.45', 'Безалкогольный энергетический напиток'),
    ('Снеки', 'Чипсы 140г', 'Соленые картофельные чипсы'),
    ('Снеки', 'Арахис 90г', 'Жареный соленый арахис'),
    ('Горячее', 'Хот-дог', 'Сосиска в булочке с соусом'),
    ('Горячее', 'Наггетсы 8 шт', 'Куриные наггетсы')
) as x(category_title, title, description)
join product_categories c on c.title = x.category_title
where not exists (
    select 1 from products p where p.title = x.title
);

-- привяжем товары к клубам с ценами
insert into club_products (club_id, product_id, price_rub, is_available)
select cl.id, p.id,
       case
         when p.title = 'Кола 0.5' then 120
         when p.title = 'Энергетик 0.45' then 170
         when p.title = 'Чипсы 140г' then 140
         when p.title = 'Арахис 90г' then 110
         when p.title = 'Хот-дог' then 220
         when p.title = 'Наггетсы 8 шт' then 260
         else 150
       end as price_rub,
       true
from clubs cl
cross join products p
where not exists (
    select 1
    from club_products cp
    where cp.club_id = cl.id and cp.product_id = p.id
);