package com.example.attendance.core.model

data class OfficeLocation(val latitude: Double, val longitude: Double, val address: String? = null)
enum class GeofenceStatus { NOT_CONFIGURED, WAITING, UNAVAILABLE, INVALID, STALE, INACCURATE, OUTSIDE, INSIDE }
data class GeofenceState(
    val status: GeofenceStatus,
    val distanceM: Double? = null,
    val fix: GeoPoint? = null,
    val office: OfficeLocation? = null
)
