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

    // seatMapByClub оставь как у тебя уже было (оно нужно)
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

    val products = listOf(
        Product("p1", "food", "Бургер", 450, "Сочный бургер.", variants = listOf("Обычный", "Острый")),
        Product("p2", "food", "Картофель фри", 200, "Хрустящий.", variants = listOf("Маленький", "Большой")),
        Product("p3", "drink", "Кола", 150, "0.5L"),
        Product("p4", "drink", "Энергетик", 220, "0.5L"),
        Product("p5", "service", "VIP-наушники", 100, "Аренда на время сессии"),
        Product("p6", "service", "Доп. время (30 мин)", 120, "Добавится к сессии"),
    )

    val notifications = listOf(
        AppNotification("a1", "Пополнение", "Баланс успешно пополнен."),
        AppNotification("a2", "Заказ", "Заказ принят и готовится."),
        AppNotification("a3", "Бронирование", "Бронирование подтверждено.")
    )

    private fun buildSeats(prefix: String): List<Seat> {
        val booked1 = listOf(TimeRange(12*60, 14*60))
        val booked2 = listOf(TimeRange(18*60, 20*60))

        return (1..20).map { i ->
            val type = if (i % 5 == 0) SeatType.VIP else SeatType.REGULAR
            val booked = when {
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
                equipment = if (type == SeatType.VIP) "RTX 4070 / 240Hz / Pro-микрофон" else "RTX 3060 / 144Hz / стандарт",
                booked = booked
            )
        }
    }
}
