package com.example.attendance.core.domain

import com.example.attendance.core.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.time.Clock
import java.time.Duration
import kotlin.math.*

private const val EARTH_RADIUS_M = 6371008.8
const val ATTENDANCE_GEOFENCE_RADIUS_M = 100.0

data class GeofenceReading(val location: GeoPoint?, val state: GeofenceState)

fun isValidOfficeLocation(location: OfficeLocation) =
    location.latitude.isFinite() && location.longitude.isFinite() && location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0

fun distanceMeters(from: OfficeLocation, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val dLat = lat2 - lat1
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2.0) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2.0)
    return EARTH_RADIUS_M * 2 * asin(min(1.0, sqrt(a)))
}

fun evaluateGeofence(office: OfficeLocation?, location: GeoPoint?, now: java.time.Instant): GeofenceState {
    if (office == null || !isValidOfficeLocation(office)) return GeofenceState(GeofenceStatus.NOT_CONFIGURED, office = office)
    if (location == null) return GeofenceState(GeofenceStatus.WAITING, office = office)
    val validPoint = location.latitude.isFinite() && location.longitude.isFinite() && location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 && location.accuracyM.isFinite() && location.accuracyM >= 0f
    if (!validPoint) return GeofenceState(GeofenceStatus.INVALID, fix = location, office = office)
    if (Duration.between(location.measuredAt, now).abs() > Duration.ofSeconds(30)) return GeofenceState(GeofenceStatus.STALE, fix = location, office = office)
    if (location.accuracyM > 50f) return GeofenceState(GeofenceStatus.INACCURATE, fix = location, office = office)
    val distance = distanceMeters(office, location)
    return GeofenceState(if (distance <= ATTENDANCE_GEOFENCE_RADIUS_M + 0.000001) GeofenceStatus.INSIDE else GeofenceStatus.OUTSIDE, distance, location, office)
}

fun GeofenceState.message(): String = when (status) {
    GeofenceStatus.NOT_CONFIGURED -> "Office location is not configured. Ask your admin to set it before marking attendance."
    GeofenceStatus.WAITING -> "Getting your current location..."
    GeofenceStatus.UNAVAILABLE -> "Turn on device location and allow precise location permission."
    GeofenceStatus.INVALID -> "Location looks invalid. Please retry."
    GeofenceStatus.STALE -> "Waiting for a fresh location fix."
    GeofenceStatus.INACCURATE -> "Location accuracy is low. Move near a window or enable precise location."
    GeofenceStatus.OUTSIDE -> "You are outside the allowed 100-meter attendance area. Move closer to the office location and try again."
    GeofenceStatus.INSIDE -> "You are within the allowed attendance area."
}

class GeofenceException(val state: GeofenceState) : DomainException(state.message())

class SetOfficeLocationUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val settings: OfficeSettings) {
    suspend operator fun invoke(latitude: Double, longitude: Double, address: String? = null) {
        repo.read().authorize(sessions.session.value, Role.ADMIN)
        val location = OfficeLocation(latitude, longitude, address?.trim()?.takeIf { it.isNotBlank() })
        requireRule(isValidOfficeLocation(location), "Enter a valid latitude and longitude.")
        settings.setOfficeLocation(location)
    }
}

class ObserveGeofenceUseCase(private val locations: LocationProvider, private val settings: OfficeSettings, private val clock: Clock) {
    operator fun invoke(): Flow<GeofenceReading> {
        val ticks = flow {
            while (true) {
                emit(clock.instant())
                delay(1_000)
            }
        }
        val points = locations.updates().map<GeoPoint, GeoPoint?> { it }.onStart { emit(null) }.catch { emit(null) }
        return combine(settings.officeLocation, points, ticks) { office, point, now ->
            GeofenceReading(point, evaluateGeofence(office, point, now))
        }.distinctUntilChanged()
    }
}
