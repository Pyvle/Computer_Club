package com.club.backend.api.dto

data class UserContextResponse(
    val userId: Long,
    val phone: String?,
    val email: String?,
    val globalRole: String,
    val clubs: List<ClubMembership>,
    val pendingApplications: List<PendingApplicationBrief>
)

data class ClubMembership(
    val clubId: Long,
    val clubName: String,
    val role: String
)

data class PendingApplicationBrief(
    val applicationId: Long,
    val clubName: String,
    val status: String
)
