package com.example.attendance.core.model

data class StaffProfile(
    val employee: Employee,
    val username: String,
    val status: AccountStatus,
    val photoKey: String?
)
