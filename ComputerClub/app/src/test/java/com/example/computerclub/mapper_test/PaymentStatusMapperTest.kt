package com.example.computerclub.mapper_test

import com.example.computerclub.ui.components.ChipTone
import com.example.computerclub.ui.components.toPaymentChipTone
import com.example.computerclub.ui.components.toPaymentLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentStatusMapperTest {

    @Test
    fun paymentStatusesMatchBackendValues() {
        assertEquals("Оплачено", "paid".toPaymentLabel())
        assertEquals("Ожидает оплаты", "CREATED".toPaymentLabel())
        assertEquals("Ошибка оплаты", "FAILED".toPaymentLabel())
        assertEquals("Отменено", "CANCELED".toPaymentLabel())
        assertEquals("Возврат", "REFUND".toPaymentLabel())
    }

    @Test
    fun paymentStatusesHaveExpectedChipTones() {
        assertEquals(ChipTone.SUCCESS, "paid".toPaymentChipTone())
        assertEquals(ChipTone.WARNING, "CREATED".toPaymentChipTone())
        assertEquals(ChipTone.ERROR, "FAILED".toPaymentChipTone())
        assertEquals(ChipTone.NEUTRAL, "CANCELED".toPaymentChipTone())
        assertEquals(ChipTone.WARNING, "REFUND".toPaymentChipTone())
    }

    @Test
    fun unknownPaymentStatusKeepsBackendValueAndNeutralTone() {
        assertEquals("UNKNOWN", "UNKNOWN".toPaymentLabel())
        assertEquals(ChipTone.NEUTRAL, "UNKNOWN".toPaymentChipTone())
    }
}
