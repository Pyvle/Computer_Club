package com.club.backend.repository

import com.club.backend.domain.entity.ClubRole
import com.club.backend.domain.entity.ClubStaffEntity
import com.club.backend.domain.entity.ClubStaffId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface ClubStaffRepository : JpaRepository<ClubStaffEntity, ClubStaffId> {

    fun findByIdClubIdAndIdUserId(clubId: Long, userId: Long): Optional<ClubStaffEntity>

    @Query(
        """
        select count(cs) > 0 from ClubStaffEntity cs
        where cs.club.id = :clubId and cs.user.id = :userId and cs.role in :roles
        """
    )
    fun existsWithAnyRole(
        @Param("clubId") clubId: Long,
        @Param("userId") userId: Long,
        @Param("roles") roles: Collection<ClubRole>
    ): Boolean

    @Query("select cs from ClubStaffEntity cs join fetch cs.user left join fetch cs.addedByUser where cs.id.clubId = :clubId")
    fun findAllByIdClubId(@Param("clubId") clubId: Long): List<ClubStaffEntity>

    @Query("select count(cs) > 0 from ClubStaffEntity cs where cs.user.id = :userId")
    fun existsByUserId(@Param("userId") userId: Long): Boolean

    @Query("select cs from ClubStaffEntity cs join fetch cs.club where cs.id.userId = :userId")
    fun findAllByIdUserId(@Param("userId") userId: Long): List<ClubStaffEntity>
}
