package com.example.attendance.core.model

import java.time.Instant

data class InboxItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val createdAt: Instant,
    val read: Boolean
)
