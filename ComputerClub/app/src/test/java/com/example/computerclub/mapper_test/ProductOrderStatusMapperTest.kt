package com.example.computerclub.mapper_test

import com.example.computerclub.model.ProductOrderStatus
import com.example.computerclub.ui.components.ChipTone
import com.example.computerclub.ui.components.toChipTone
import com.example.computerclub.ui.components.toLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductOrderStatusMapperTest {

    @Test
    fun productOrderStatusesHaveUserLabels() {
        assertEquals("Готовится", ProductOrderStatus.NOT_READY.toLabel())
        assertEquals("Готов", ProductOrderStatus.READY.toLabel())
        assertEquals("Отменён", ProductOrderStatus.CANCELED.toLabel())
    }

    @Test
    fun productOrderStatusesHaveExpectedChipTones() {
        assertEquals(ChipTone.WARNING, ProductOrderStatus.NOT_READY.toChipTone())
        assertEquals(ChipTone.SUCCESS, ProductOrderStatus.READY.toChipTone())
        assertEquals(ChipTone.ERROR, ProductOrderStatus.CANCELED.toChipTone())
    }
}
