package com.example.attendance.core.model

import java.time.Instant
import java.time.LocalDate

data class VerificationAttempt(
    val id: String,
    val employeeId: String,
    val referencePhotoId: String,
    val action: PunchAction,
    val capturedAt: Instant,
    val attendanceDate: LocalDate,
    val timeZoneId: String,
    val imageKey: String,
    val location: GeoPoint,
    val confidenceBps: Int,
    val outcome: Outcome,
    val verifierVersion: String,
    val policyVersion: String,
    val createdAt: Instant
)
