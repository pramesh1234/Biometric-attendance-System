package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class CalendarDay(
    val id: String,
    val date: LocalDate,
    val type: DayType,
    val holidayId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
