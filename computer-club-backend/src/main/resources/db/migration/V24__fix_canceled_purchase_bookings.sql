-- Исправляет данные: ранее отмена покупки не отменяла связанные брони.
-- Приводим все UPCOMING/ACTIVE брони в соответствие с отменёнными покупками.
update bookings
set status = 'CANCELED'
where status in ('UPCOMING', 'ACTIVE')
  and purchase_id in (
      select id from purchases where payment_status = 'CANCELED'
  );
