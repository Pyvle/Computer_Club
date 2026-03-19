-- audit_log.club_id is nullable but lacks ON DELETE CASCADE — set null on club delete
alter table audit_log
    drop constraint audit_log_club_id_fkey,
    add constraint audit_log_club_id_fkey
        foreign key (club_id) references clubs(id) on delete set null;
