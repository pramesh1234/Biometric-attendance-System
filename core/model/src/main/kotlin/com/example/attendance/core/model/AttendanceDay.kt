package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class AttendanceDay(
    val date: LocalDate,
    val type: DayType,
    val checkIn: Instant?,
    val checkOut: Instant?,
    val pending: Boolean,
    val timeZoneId: String = "UTC"
)
