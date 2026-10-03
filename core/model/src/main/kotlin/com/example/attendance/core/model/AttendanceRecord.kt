package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class AttendanceRecord(
    val id: String,
    val employeeId: String,
    val date: LocalDate,
    val timeZoneId: String,
    val checkInAttemptId: String,
    val checkOutAttemptId: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant
)
