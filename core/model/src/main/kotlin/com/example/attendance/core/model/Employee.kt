package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class Employee(
    val id: String,
    val code: String,
    val name: String,
    val email: String,
    val phone: String,
    val jobTitle: String,
    val gender: String,
    val joiningDate: LocalDate,
    val endDate: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant? = null
)
