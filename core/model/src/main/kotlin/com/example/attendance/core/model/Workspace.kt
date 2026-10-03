package com.example.attendance.core.model

data class Workspace(
    val employees: List<Employee> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val photos: List<EmployeePhoto> = emptyList(),
    val attempts: List<VerificationAttempt> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val reviews: List<ReviewRequest> = emptyList(),
    val notices: List<Notice> = emptyList(),
    val recipients: List<NoticeRecipient> = emptyList(),
    val notifications: List<InboxNotification> = emptyList(),
    val holidays: List<Holiday> = emptyList(),
    val calendar: List<CalendarDay> = emptyList()
)
