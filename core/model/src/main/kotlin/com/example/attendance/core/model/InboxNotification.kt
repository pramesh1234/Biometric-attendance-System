package com.example.attendance.core.model

import java.time.Instant

data class InboxNotification(
    val id: String,
    val employeeId: String,
    val type: NotificationType,
    val noticeRecipientId: String? = null,
    val reviewRequestId: String? = null,
    val createdAt: Instant,
    val readAt: Instant? = null
)
