package com.example.attendance.core.model

data class AttendanceSummary(
    val percentage: Double?,
    val attended: Int,
    val eligible: Int,
    val days: List<AttendanceDay>
)
