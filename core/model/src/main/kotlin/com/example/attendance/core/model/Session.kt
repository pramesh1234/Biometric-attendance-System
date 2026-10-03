package com.example.attendance.core.model

import java.time.Instant

data class Session(
    val accountId: String,
    val role: Role,
    val employeeId: String?,
    val passwordVersion: Instant,
    val mustChangePassword: Boolean
)
