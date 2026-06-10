package com.example.computerclub.ui.components

import com.example.computerclub.model.BookingStatus
import com.example.computerclub.model.ProductOrderStatus

enum class ChipTone { SUCCESS, WARNING, ERROR, INFO, NEUTRAL }

fun BookingStatus.toChipTone(): ChipTone = when (this) {
    BookingStatus.UPCOMING -> ChipTone.INFO
    BookingStatus.ACTIVE -> ChipTone.SUCCESS
    BookingStatus.DONE -> ChipTone.NEUTRAL
    BookingStatus.CANCELED -> ChipTone.ERROR
}

fun BookingStatus.toLabel(): String = when (this) {
    BookingStatus.UPCOMING -> "Предстоит"
    BookingStatus.ACTIVE -> "Активна"
    BookingStatus.DONE -> "Завершена"
    BookingStatus.CANCELED -> "Отменена"
}

fun ProductOrderStatus.toChipTone(): ChipTone = when (this) {
    ProductOrderStatus.READY -> ChipTone.SUCCESS
    ProductOrderStatus.NOT_READY -> ChipTone.WARNING
    ProductOrderStatus.CANCELED -> ChipTone.ERROR
}

fun ProductOrderStatus.toLabel(): String = when (this) {
    ProductOrderStatus.READY -> "Готов"
    ProductOrderStatus.NOT_READY -> "Готовится"
    ProductOrderStatus.CANCELED -> "Отменён"
}

// маппинг строкового paymentStatus из Purchase
fun String.toPaymentChipTone(): ChipTone = when (this.uppercase()) {
    "PAID" -> ChipTone.SUCCESS
    "CREATED" -> ChipTone.WARNING
    "REFUND" -> ChipTone.WARNING
    "FAILED" -> ChipTone.ERROR
    "CANCELED" -> ChipTone.NEUTRAL
    else -> ChipTone.NEUTRAL
}

fun String.toPaymentLabel(): String = when (this.uppercase()) {
    "PAID" -> "Оплачено"
    "CREATED" -> "Ожидает оплаты"
    "FAILED" -> "Ошибка оплаты"
    "REFUND" -> "Возврат"
    "CANCELED" -> "Отменено"
    else -> this
}
