package com.example.computerclub.data

import com.example.computerclub.model.*

object FakeData {
    val clubs = listOf(
        Club(
            id = "c1",
            name = "Cyber Arena",
            location = "Москва, м. Белорусская",
            address = "Ленинградский пр-т, 10",
            description = "Большой зал, VIP-зона, кафе, турниры по выходным."
        ),
        Club(
            id = "c2",
            name = "Night Owl Club",
            location = "Санкт-Петербург, Центр",
            address = "Невский пр-т, 55",
            description = "Ночные тарифы, комфортные кресла, много периферии."
        ),
        Club(
            id = "c3",
            name = "VIP Station",
            location = "Казань, Ново-Савиновский",
            address = "ул. Чистопольская, 20",
            description = "Топовое железо, 240Hz мониторы, отдельные кабинки."
        )
    )

    val seatMapByClub: Map<String, List<Seat>> = mapOf(
        "c1" to buildSeats("A"),
        "c2" to buildSeats("B"),
        "c3" to buildSeats("C"),
    )

    val categories = listOf(
        ProductCategory("food", "Еда"),
        ProductCategory("drink", "Напитки"),
        ProductCategory("service", "Услуги"),
    )

    /**
     * ✅ Разное меню для каждого клуба
     */
    val productsByClubId: Map<String, List<Product>> = mapOf(
        // Cyber Arena — самое большое меню
        "c1" to listOf(
            Product("p1", "food", "Бургер", 450, "Сочный бургер.", variants = listOf("Обычный", "Острый")),
            Product("p2", "food", "Картофель фри", 200, "Хрустящий.", variants = listOf("Маленький", "Большой")),
            Product("p3", "drink", "Кола", 150, "0.5L"),
            Product("p4", "drink", "Энергетик", 220, "0.5L"),
            Product("p5", "service", "VIP-наушники", 100, "Аренда на время сессии"),
            Product("p6", "service", "Доп. время (30 мин)", 120, "Добавится к сессии"),
        ),

        // Night Owl — упор на напитки + ночные штуки
        "c2" to listOf(
            Product("p2", "food", "Картофель фри", 210, "Хрустящий.", variants = listOf("Маленький", "Большой")),
            Product("p7", "food", "Хот-дог", 180, "Классический хот-дог."),
            Product("p3", "drink", "Кола", 150, "0.5L"),
            Product("p4", "drink", "Энергетик", 230, "0.5L"),
            Product("p8", "drink", "Кофе", 140, "0.3L"),
            Product("p9", "service", "Ночной буст (1 час)", 150, "Добавится к сессии"),
            Product("p6", "service", "Доп. время (30 мин)", 120, "Добавится к сессии"),
        ),

        // VIP Station — премиум меню
        "c3" to listOf(
            Product("p10", "food", "Сэндвич", 260, "С курицей и сыром."),
            Product("p2", "food", "Картофель фри", 220, "Хрустящий.", variants = listOf("Маленький", "Большой")),
            Product("p11", "food", "Салат", 240, "Лёгкий салат."),
            Product("p12", "drink", "Вода", 80, "0.5L"),
            Product("p4", "drink", "Энергетик", 240, "0.5L"),
            Product("p13", "drink", "Лимонад", 170, "0.5L"),
            Product("p5", "service", "VIP-наушники", 120, "Аренда на время сессии"),
            Product("p14", "service", "Премиум-место (30 мин)", 200, "Добавится к сессии"),
        ),
    )

    /** ✅ Товары для выбранного клуба */
    fun productsForClub(clubId: String): List<Product> =
        productsByClubId[clubId] ?: products

    /**
     * Общий список оставляем, чтобы не ломать другие экраны.
     * Это объединение всех клубных меню.
     */
    val products: List<Product> = productsByClubId.values.flatten().distinctBy { it.id }

    val notifications = listOf(
        AppNotification("a1", "Пополнение", "Баланс успешно пополнен."),
        AppNotification("a2", "Заказ", "Заказ принят и готовится."),
        AppNotification("a3", "Бронирование", "Бронирование подтверждено.")
    )

    /**
     * VIP только на местах 5/10/15/20, остальные — СТАНДАРТ.
     * Мест делаем 34, чтобы спокойно встречались номера типа #27, #34 и т.д.
     */
    private fun buildSeats(prefix: String): List<Seat> {
        val booked1 = listOf(TimeRange(12 * 60, 14 * 60))
        val booked2 = listOf(TimeRange(18 * 60, 20 * 60))
        val bookedExtendDemo = listOf(TimeRange(9 * 60, 10 * 60))

        val vipNumbers = setOf(5, 10, 15, 20)

        return (1..34).map { i ->
            val type = if (i in vipNumbers) SeatType.VIP else SeatType.REGULAR
            val booked = when {
                i % 8 == 0 -> bookedExtendDemo
                i % 7 == 0 -> booked1
                i % 9 == 0 -> booked2
                i % 11 == 0 -> booked1 + booked2
                else -> emptyList()
            }

            Seat(
                id = "${prefix}${i}",
                label = "$prefix-$i",
                type = type,
                hasPc = true,
                equipment = if (type == SeatType.VIP)
                    "VIP: RTX 4070 / 240Hz / Pro-микрофон"
                else
                    "Стандарт: RTX 3060 / 144Hz",
                booked = booked
            )
        }
    }
}
