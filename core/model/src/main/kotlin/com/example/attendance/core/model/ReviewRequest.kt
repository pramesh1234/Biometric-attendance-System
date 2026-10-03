package com.example.attendance.core.model

import java.time.Instant

data class ReviewRequest(
    val id: String,
    val attemptId: String,
    val status: ReviewStatus,
    val submittedAt: Instant,
    val reviewedBy: String? = null,
    val reviewedAt: Instant? = null,
    val note: String = ""
)
