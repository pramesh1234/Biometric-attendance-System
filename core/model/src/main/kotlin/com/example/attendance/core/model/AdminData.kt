package com.example.attendance.core.model

data class AdminData(
    val staff: List<StaffProfile> = emptyList(),
    val reviews: List<ReviewDetail> = emptyList(),
    val checkedIn: Int = 0,
    val holidays: List<Holiday> = emptyList()
)
