package com.club.backend.domain.enum

/**
 * Пермишены уровня клуба.
 * Начальный набор соответствует ролям OWNER/ADMIN; расширяется по мере роста продукта.
 */
enum class ClubPermission {
    CLUB_ADMINS_MANAGE,
    CLUB_CATALOG_MANAGE,
    CLUB_SEATS_MANAGE,
    CLUB_USER_BLOCKS_MANAGE,
    CLUB_FLOORPLANS_MANAGE,
    CLUB_REPORTS_VIEW
}
