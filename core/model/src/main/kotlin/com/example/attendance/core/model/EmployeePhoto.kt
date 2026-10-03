package com.example.attendance.core.model

import java.time.Instant

data class EmployeePhoto(
    val id: String,
    val employeeId: String,
    val fileKey: String,
    val createdBy: String,
    val createdAt: Instant,
    val retiredAt: Instant? = null
)
