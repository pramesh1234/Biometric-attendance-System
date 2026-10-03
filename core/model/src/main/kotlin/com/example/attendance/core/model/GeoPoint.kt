package com.example.attendance.core.model

import java.time.Instant

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float,
    val label: String,
    val measuredAt: Instant
)
