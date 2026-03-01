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
