package com.club.backend.service

import com.club.backend.api.dto.AddressSearchItem
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Service
class GeoService(
    private val objectMapper: ObjectMapper
) {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    fun search(query: String): List<AddressSearchItem> {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val url = "https://nominatim.openstreetmap.org/search" +
            "?q=$encoded&format=json&addressdetails=1&limit=5&accept-language=ru"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            // Nominatim требует непустой User-Agent
            .header("User-Agent", "ComputerClubAdmin/1.0")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val root: JsonNode = objectMapper.readTree(response.body())

        return root.mapNotNull { node ->
            val lat = node.get("lat")?.asDouble() ?: return@mapNotNull null
            val lon = node.get("lon")?.asDouble() ?: return@mapNotNull null
            val address = node.get("address") ?: return@mapNotNull null
            AddressSearchItem(
                addressFull = formatFullAddress(address),
                addressShort = formatShortAddress(address),
                latitude = lat,
                longitude = lon
            )
        }
    }

    /**
     * Полный адрес от общего к частному для главы клуба:
     * индекс, страна, регион, муниципальный район, город, район города, улица, дом, название объекта
     * Пример: 121615, Россия, Москва, Москва, район Кунцево, Рублёвское шоссе, 28, Бизнес-центр «Профико»
     */
    private fun formatFullAddress(a: JsonNode): String {
        val parts = listOfNotNull(
            a.str("postcode"),
            a.str("country"),
            a.str("state"),
            a.str("county"),
            a.str("city") ?: a.str("town") ?: a.str("village"),
            a.str("city_district") ?: a.str("suburb"),
            a.str("road") ?: a.str("pedestrian") ?: a.str("footway"),
            a.str("house_number"),
            // название объекта: БЦ, ТЦ, здание — если поиск был по нему
            a.str("amenity") ?: a.str("tourism") ?: a.str("office")
        )
        return parts.joinToString(", ")
    }

    /**
     * Короткий адрес для пользователя: город, улица, дом — без страны, индекса, районов
     * Пример: Москва, Рублёвское шоссе, 28
     */
    private fun formatShortAddress(a: JsonNode): String {
        val parts = listOfNotNull(
            a.strClean("city") ?: a.strClean("town") ?: a.strClean("village"),
            a.strClean("road") ?: a.strClean("pedestrian") ?: a.strClean("footway"),
            a.str("house_number")
        )
        return parts.joinToString(", ")
    }

    // полное значение поля — для addressFull
    private fun JsonNode.str(field: String): String? =
        get(field)?.takeIf { !it.isNull }?.asText()?.takeIf { it.isNotBlank() }

    // значение без скобочных уточнений — для addressShort
    private fun JsonNode.strClean(field: String): String? =
        str(field)?.replace(Regex("\\s*\\([^)]*\\)"), "")?.trim()?.takeIf { it.isNotBlank() }
}
