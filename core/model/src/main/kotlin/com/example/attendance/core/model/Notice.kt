package com.example.attendance.core.model

import java.time.Instant

data class Notice(
    val id: String,
    val senderId: String,
    val title: String,
    val body: String,
    val sentAt: Instant
)
