insert into users (id, phone, username, password_hash, is_active)
values (1, '+79990000001', 'demo', null, true)
on conflict (id) do nothing;