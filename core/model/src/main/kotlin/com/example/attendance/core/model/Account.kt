package com.example.attendance.core.model

import java.time.Instant

data class Account(
    val id: String,
    val employeeId: String?,
    val username: String,
    val passwordHash: String,
    val role: Role,
    val status: AccountStatus,
    val mustChangePassword: Boolean,
    val passwordChangedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant
)
