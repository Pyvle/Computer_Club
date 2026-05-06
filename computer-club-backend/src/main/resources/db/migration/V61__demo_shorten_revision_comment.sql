UPDATE club_applications
SET decision_comment = 'Название выглядит слишком общим и больше похоже на черновик. Пожалуйста, укажите нормальное название клуба.'
WHERE id = 6
  AND applicant_user_id = 17
  AND status = 'REVISION_REQUESTED';
