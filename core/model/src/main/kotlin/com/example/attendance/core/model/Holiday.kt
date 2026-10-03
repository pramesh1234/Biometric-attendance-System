package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class Holiday(
    val id: String,
    val name: String,
    val date: LocalDate,
    val description: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
