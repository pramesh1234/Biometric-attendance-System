package com.example.attendance.core.model

data class ReviewDetail(
    val request: ReviewRequest,
    val attempt: VerificationAttempt,
    val employee: Employee,
    val referenceKey: String
)
