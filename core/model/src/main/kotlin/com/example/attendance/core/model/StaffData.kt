package com.example.attendance.core.model

import java.time.Instant

data class StaffData(
    val profile: StaffProfile,
    val record: AttendanceRecord?,
    val checkIn: Instant?,
    val checkOut: Instant?,
    val pending: ReviewDetail?,
    val month: AttendanceSummary,
    val nextHoliday: Holiday?,
    val unreadCount: Int
)
