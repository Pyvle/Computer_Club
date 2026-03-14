package com.example.computerclub.model

/**
 * Позиция места на схеме зала в долях (0..1) от ширины/высоты схемы.
 * Используется для отрисовки зала по опубликованному floorplan.data.
 */
data class FloorplanSeatPos(
    val seatId: String,
    val x: Float, // 0..1
    val y: Float, // 0..1
    val w: Float, // 0..1
    val h: Float  // 0..1
)

enum class WallOrientation { H, V }

/**
 * Стена между ячейками сетки.
 * H — горизонтальная линия между строками (row-1) и row.
 * V — вертикальная линия между столбцами (col-1) и col.
 */
data class FloorplanWallPos(
    val orientation: WallOrientation,
    val col: Int,
    val row: Int
)

/**
 * Ячейка пола на схеме зала — часть комнаты определённого типа.
 * Используется для окраски фона в BookingSeatsScreen.
 */
data class FloorplanFloorPos(
    val col: Int,
    val row: Int,
    val roomType: String // "REGULAR" | "VIP"
)
