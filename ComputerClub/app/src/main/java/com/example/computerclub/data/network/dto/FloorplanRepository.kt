package com.example.computerclub.data.repository

import com.example.computerclub.data.network.FloorplanApi

class FloorplanRepository(
    private val api: FloorplanApi
) {
    suspend fun published(clubId: Long) = api.getPublished(clubId)

    suspend fun publishedWithAvailability(clubId: Long, from: String, to: String) =
        api.getPublishedWithAvailability(clubId, from, to)
}
