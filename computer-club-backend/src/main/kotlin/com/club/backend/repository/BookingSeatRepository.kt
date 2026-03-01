package com.club.backend.repository

import com.club.backend.domain.entity.BookingSeatEntity
import com.club.backend.domain.entity.BookingSeatId
import org.springframework.data.jpa.repository.JpaRepository

interface BookingSeatRepository : JpaRepository<BookingSeatEntity, BookingSeatId>