package com.example.computerclub.data.repository

import com.example.computerclub.data.network.FavoritesApi

class FavoritesRepository(private val api: FavoritesApi) {
    suspend fun getFavorites(): List<Long> = api.getFavorites()
    suspend fun addFavorite(clubId: Long) = api.addFavorite(clubId)
    suspend fun removeFavorite(clubId: Long) = api.removeFavorite(clubId)
}
