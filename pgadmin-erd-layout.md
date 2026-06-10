# Раскладка ERD в pgAdmin

Диаграмма для системы «Приложение управления компьютерным клубом».

## Общая идея

Размещай таблицы слева направо по пользовательскому сценарию:

1. пользователи и авторизация
2. ядро клуба
3. бронирование
4. магазин и оплаты
5. администрирование и модерация
6. технические таблицы

## Сетка

```text
┌────────────────────┬────────────────────┬────────────────────┬────────────────────┐
│ Пользователи       │ Ядро клуба          │ Бронирование       │ Магазин и оплаты   │
│                    │                    │                    │                    │
│ users              │ clubs              │ bookings           │ product_categories │
│ otp_challenges     │ seats              │ booking_seats      │ products           │
│ auth_sessions      │ club_floorplans    │                    │ club_products      │
│ user_favorite_clubs│ club_time_packages │                    │ carts              │
│                    │ club_seat_type_... │                    │ cart_items         │
│                    │                    │                    │ cart_item_seats    │
│                    │                    │                    │ purchases          │
│                    │                    │                    │ product_order_items│
└────────────────────┴────────────────────┴────────────────────┴────────────────────┘

┌─────────────────────────────────────────┬────────────────────┐
│ Администрирование и модерация           │ Технические        │
│                                         │                    │
│ club_staff                              │ idempotency_keys   │
│ club_permission_rules                   │ flyway_schema_...  │
│ club_applications                       │                    │
│ club_user_blocks                        │                    │
│ club_messages                           │                    │
│ audit_log                               │                    │
└─────────────────────────────────────────┴────────────────────┘
```

## Рекомендуемое расположение

### 1. Пользователи и авторизация

Цвет: светло-синий.

- `users` — главный узел блока, поставь ближе к центру слева
- `otp_challenges` — слева/сверху от `users`
- `auth_sessions` — слева/снизу от `users`
- `user_favorite_clubs` — между `users` и `clubs`, потому что связывает пользователя с клубом

### 2. Ядро клуба

Цвет: светло-зеленый.

- `clubs` — центральная таблица всей диаграммы
- `seats` — под `clubs`
- `club_floorplans` — справа от `clubs`
- `club_time_packages` — под `club_floorplans`
- `club_seat_type_settings` — под `seats`

### 3. Бронирование

Цвет: светло-оранжевый.

- `bookings` — справа от `clubs`, на одной горизонтали
- `booking_seats` — под `bookings`, между `bookings` и `seats`

### 4. Магазин и оплаты

Цвет: светло-фиолетовый.

- `product_categories` — верх блока
- `products` — под `product_categories`
- `club_products` — между `products` и `clubs`
- `carts` — справа от `users` и ниже `club_products`
- `cart_items` — справа от `carts`
- `cart_item_seats` — под `cart_items`, ближе к `seats`
- `purchases` — ниже `carts`
- `product_order_items` — справа от `purchases`, под `cart_items`

### 5. Администрирование и модерация

Цвет: светло-красный или светло-желтый.

- `club_staff` — между `users` и `clubs`, выше `user_favorite_clubs`
- `club_permission_rules` — рядом с `club_staff`
- `club_applications` — слева от `clubs`, ниже блока авторизации
- `club_user_blocks` — под `club_staff`
- `club_messages` — справа от `club_user_blocks`
- `audit_log` — внизу административного блока

### 6. Технические таблицы

Цвет: светло-серый.

- `idempotency_keys` — внизу, рядом с `purchases` или отдельно справа снизу
- `flyway_schema_history` — можно скрыть с ERD для ВКР; если оставлять, поставить в самый нижний правый угол

## Как сделать в pgAdmin

1. Открой ERD Tool для схемы `public`.
2. Включи отображение связей и Crow's Foot Notation.
3. Сначала поставь `clubs` в центр.
4. От `clubs` разложи соседние домены: слева `users`, справа `bookings`, дальше справа магазин.
5. Выделяй таблицы одного домена и задавай им одинаковый Fill Color.
6. Используй Auto align только в начале, затем вручную поправь группы.
7. После ручной раскладки сохрани диаграмму как `.pgerd`.

## Если нужно, чтобы я поправил файл

Сохрани диаграмму в pgAdmin через Save As как `.pgerd` и положи файл в `D:\diploma`.
После этого можно поправить координаты прямо в JSON-файле ERD и открыть его обратно в pgAdmin.
