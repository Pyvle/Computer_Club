package com.club.backend.service

import com.club.backend.domain.entity.UserFavoriteClubEntity
import com.club.backend.domain.entity.UserFavoriteClubId
import com.club.backend.repository.UserFavoriteClubRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteClubServiceTest {

    private val favorites = mutableListOf<UserFavoriteClubEntity>()
    private val repo: UserFavoriteClubRepository = proxyRepo { method, args ->
        when (method) {
            "findAllByIdUserId" -> favorites.filter { it.id.userId == args[0] as Long }
            "existsById" -> favorites.any { it.id == args[0] as UserFavoriteClubId }
            "save" -> (args[0] as UserFavoriteClubEntity).also { favorites.add(it) }
            "deleteById" -> favorites.removeIf { it.id == args[0] as UserFavoriteClubId }
            else -> unsupported(method)
        }
    }
    private val service = FavoriteClubService(repo)

    @Test
    fun `getFavoriteClubIds returns only club ids for user`() {
        favorites += listOf(
            UserFavoriteClubEntity(UserFavoriteClubId(userId = 5L, clubId = 2L)),
            UserFavoriteClubEntity(UserFavoriteClubId(userId = 5L, clubId = 7L))
        )

        val result = service.getFavoriteClubIds(5L)

        assertEquals(listOf(2L, 7L), result)
    }

    @Test
    fun `addFavorite saves new favorite when it does not exist`() {
        service.addFavorite(9L, 4L)

        assertTrue(favorites.any { it.id == UserFavoriteClubId(userId = 9L, clubId = 4L) })
    }

    @Test
    fun `addFavorite skips save when favorite already exists`() {
        favorites += UserFavoriteClubEntity(UserFavoriteClubId(userId = 9L, clubId = 4L))

        service.addFavorite(9L, 4L)

        assertEquals(1, favorites.count { it.id == UserFavoriteClubId(userId = 9L, clubId = 4L) })
    }

    @Test
    fun `removeFavorite deletes favorite by composite id`() {
        favorites += UserFavoriteClubEntity(UserFavoriteClubId(userId = 3L, clubId = 8L))

        service.removeFavorite(3L, 8L)

        assertTrue(favorites.none { it.id == UserFavoriteClubId(userId = 3L, clubId = 8L) })
    }
}
