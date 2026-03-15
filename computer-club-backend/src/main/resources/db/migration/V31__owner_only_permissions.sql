-- CLUB_AUDIT_VIEW — право просмотра аудита, только для OWNER.
-- ADMIN в базе не получает это право (нет insert для ADMIN).

insert into club_role_permissions(role, permission) values
    ('OWNER', 'CLUB_AUDIT_VIEW')
on conflict do nothing;
