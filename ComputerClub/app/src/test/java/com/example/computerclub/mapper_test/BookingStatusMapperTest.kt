package com.example.computerclub.mapper_test

import com.example.computerclub.model.BookingStatus
import com.example.computerclub.ui.components.ChipTone
import com.example.computerclub.ui.components.toChipTone
import com.example.computerclub.ui.components.toLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class BookingStatusMapperTest {

    @Test
    fun bookingStatusesHaveUserLabels() {
        assertEquals("Предстоит", BookingStatus.UPCOMING.toLabel())
        assertEquals("Активна", BookingStatus.ACTIVE.toLabel())
        assertEquals("Завершена", BookingStatus.DONE.toLabel())
        assertEquals("Отменена", BookingStatus.CANCELED.toLabel())
    }

    @Test
    fun bookingStatusesHaveExpectedChipTones() {
        assertEquals(ChipTone.INFO, BookingStatus.UPCOMING.toChipTone())
        assertEquals(ChipTone.SUCCESS, BookingStatus.ACTIVE.toChipTone())
        assertEquals(ChipTone.NEUTRAL, BookingStatus.DONE.toChipTone())
        assertEquals(ChipTone.ERROR, BookingStatus.CANCELED.toChipTone())
    }
}
