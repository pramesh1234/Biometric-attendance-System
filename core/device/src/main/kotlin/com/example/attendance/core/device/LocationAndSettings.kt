package com.example.attendance.core.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.domain.LocationProvider
import com.example.attendance.core.domain.OfficeSettings
import com.example.attendance.core.domain.SessionStore
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.OfficeLocation
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.Session
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class PersistentSessionStore(context: Context) : SessionStore {
    private val preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val value = MutableStateFlow(readSession())
    override val session: StateFlow<Session?> = value.asStateFlow()
    override fun set(session: Session?) {
        if (session == null) {
            preferences.edit().clear().apply()
        } else {
            preferences.edit()
                .putString("account_id", session.accountId)
                .putString("role", session.role.name)
                .putString("employee_id", session.employeeId)
                .putString("password_version", session.passwordVersion.toString())
                .putBoolean("must_change_password", session.mustChangePassword)
                .apply()
        }
        value.value = session
    }

    private fun readSession(): Session? = runCatching {
        val accountId = preferences.getString("account_id", null) ?: return null
        val role = Role.valueOf(preferences.getString("role", null) ?: return null)
        val passwordVersion =
            Instant.parse(preferences.getString("password_version", null) ?: return null)
        Session(
            accountId,
            role,
            preferences.getString("employee_id", null),
            passwordVersion,
            preferences.getBoolean("must_change_password", false)
        )
    }.getOrNull()
}

class LocalOfficeSettings(context: Context) : OfficeSettings {
    private val preferences = context.getSharedPreferences("office", Context.MODE_PRIVATE)
    private val value = MutableStateFlow(runCatching {
        ZoneId.of(
            preferences.getString(
                "zone",
                ZoneId.systemDefault().id
            )
        )
    }.getOrDefault(ZoneId.systemDefault()))
    private val office = MutableStateFlow(readOfficeLocation())
    override val zone = value.asStateFlow()
    override val officeLocation = office.asStateFlow()
    override fun setZone(zone: ZoneId) {
        preferences.edit().putString("zone", zone.id).apply(); value.value = zone
    }

    override fun setOfficeLocation(location: OfficeLocation) {
        preferences.edit().putFloat("office_lat", location.latitude.toFloat())
            .putFloat("office_lon", location.longitude.toFloat())
            .putString("office_address", location.address).apply(); office.value = location
    }

    private fun readOfficeLocation(): OfficeLocation? {
        if (!preferences.contains("office_lat") || !preferences.contains("office_lon")) return null
        return OfficeLocation(
            preferences.getFloat("office_lat", 0f).toDouble(),
            preferences.getFloat("office_lon", 0f).toDouble(),
            preferences.getString("office_address", null)
        )
    }
}

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val client =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun current(): GeoPoint {

        val fineGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            throw DomainException(
                "Location permission is required."
            )
        }

        Log.d("LOCATION", "Requesting fused location")

        /*
         * First try cached location.
         */
        val lastLocation = try {
            client.lastLocation.await()
        } catch (e: Exception) {
            null
        }

        if (lastLocation != null) {

            val age =
                System.currentTimeMillis() - lastLocation.time

            Log.d(
                "LOCATION",
                """
                Last location
                lat=${lastLocation.latitude}
                lng=${lastLocation.longitude}
                accuracy=${lastLocation.accuracy}
                age=$age ms
                """.trimIndent()
            )

            // Accept a reasonably recent cached location
            if (age <= 60_000) {
                return lastLocation.toGeoPoint()
            }
        }

        /*
         * Otherwise request a fresh location.
         */
        val cancellationTokenSource =
            CancellationTokenSource()

        val location = try {

            withTimeout(30_000) {

                client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
            }

        } catch (e: TimeoutCancellationException) {

            cancellationTokenSource.cancel()

            Log.e(
                "LOCATION",
                "Fused location timed out"
            )

            throw DomainException(
                "Unable to get your location. Please check location settings and try again."
            )
        }

        if (location == null) {
            throw DomainException(
                "Unable to determine your current location."
            )
        }

        Log.d(
            "LOCATION",
            """
            Current location received
            lat=${location.latitude}
            lng=${location.longitude}
            accuracy=${location.accuracy}
            """.trimIndent()
        )

        return location.toGeoPoint()
    }

    @SuppressLint("MissingPermission")
    override fun updates(): Flow<GeoPoint> =
        callbackFlow {

            val request =
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5_000L
                )
                    .setMinUpdateIntervalMillis(2_000L)
                    .setWaitForAccurateLocation(false)
                    .build()

            val callback =
                object : LocationCallback() {

                    override fun onLocationResult(
                        result: LocationResult
                    ) {

                        val location =
                            result.lastLocation ?: return

                        Log.d(
                            "LOCATION",
                            """
                        Location update
                        lat=${location.latitude}
                        lng=${location.longitude}
                        accuracy=${location.accuracy}
                        """.trimIndent()
                        )

                        trySend(
                            location.toGeoPoint()
                        )
                    }
                }

            client.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(callback)
            }
        }

    private fun Location.toGeoPoint(): GeoPoint {
        return GeoPoint(
            latitude = latitude,
            longitude = longitude,
            accuracyM = accuracy,
            label = addressFor(this).orEmpty(),
            measuredAt = Instant.ofEpochMilli(time)
        )
    }

    private fun addressFor(location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )

            addresses
                ?.firstOrNull()
                ?.getAddressLine(0)

        } catch (e: Exception) {
            null
        }
    }
}