package com.club.backend.api.dto

data class UserContextResponse(
    val userId: Long,
    val phone: String?,
    val email: String?,
    val globalRole: String,
    val clubs: List<ClubMembership>,
    val hasPassword: Boolean,
    val activeApplication: ActiveApplicationBrief?
)

data class ClubMembership(
    val clubId: Long,
    val clubName: String,
    val role: String
)

/** Актуальная (последняя не-APPROVED) заявка пользователя на открытие клуба. */
data class ActiveApplicationBrief(
    val applicationId: Long,
    val clubName: String,
    val status: String,
    val decisionComment: String?
)
