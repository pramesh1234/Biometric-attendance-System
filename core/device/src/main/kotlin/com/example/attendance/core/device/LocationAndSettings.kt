package com.example.attendance.core.device
import android.annotation.SuppressLint
import android.content.Context
import android.location.*
import android.os.*
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.time.*
import java.util.Locale
import kotlin.coroutines.resume

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
        val passwordVersion = Instant.parse(preferences.getString("password_version", null) ?: return null)
        Session(accountId, role, preferences.getString("employee_id", null), passwordVersion, preferences.getBoolean("must_change_password", false))
    }.getOrNull()
}

class LocalOfficeSettings(context: Context) : OfficeSettings {
    private val preferences = context.getSharedPreferences("office", Context.MODE_PRIVATE)
    private val value = MutableStateFlow(runCatching { ZoneId.of(preferences.getString("zone", ZoneId.systemDefault().id)) }.getOrDefault(ZoneId.systemDefault()))
    private val office = MutableStateFlow(readOfficeLocation())
    override val zone = value.asStateFlow()
    override val officeLocation = office.asStateFlow()
    override fun setZone(zone: ZoneId) { preferences.edit().putString("zone", zone.id).apply(); value.value = zone }
    override fun setOfficeLocation(location: OfficeLocation) { preferences.edit().putFloat("office_lat", location.latitude.toFloat()).putFloat("office_lon", location.longitude.toFloat()).putString("office_address", location.address).apply(); office.value = location }
    private fun readOfficeLocation(): OfficeLocation? {
        if (!preferences.contains("office_lat") || !preferences.contains("office_lon")) return null
        return OfficeLocation(preferences.getFloat("office_lat", 0f).toDouble(), preferences.getFloat("office_lon", 0f).toDouble(), preferences.getString("office_address", null))
    }
}
class AndroidLocationProvider(private val context: Context) : com.example.attendance.core.domain.LocationProvider {
    private val manager = context.getSystemService(LocationManager::class.java)
    @SuppressLint("MissingPermission")
    override suspend fun current(): GeoPoint = withContext(Dispatchers.Main) {
        val provider = when { context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER; manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER; else -> throw DomainException("Turn on device location and try again.") }
        val location = try {
            withTimeout(30_000) {
                suspendCancellableCoroutine<Location> { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) { manager.removeUpdates(this); if (continuation.isActive) continuation.resume(location) }
                        override fun onProviderDisabled(provider: String) {}
                        override fun onProviderEnabled(provider: String) {}
                        @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    }
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                }
            }
        } catch (e: TimeoutCancellationException) { throw DomainException("Location timed out. Move near a window and retry.") }
        GeoPoint(location.latitude, location.longitude, location.accuracy, addressFor(location), Instant.ofEpochMilli(location.time))
    }
    @SuppressLint("MissingPermission")
    override fun updates(): Flow<GeoPoint> = callbackFlow {
        val provider = when {
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED && manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            (context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED || context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> throw DomainException("Turn on device location and try again.")
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { trySend(GeoPoint(location.latitude, location.longitude, location.accuracy, "%.5f, %.5f".format(java.util.Locale.ROOT, location.latitude, location.longitude), Instant.ofEpochMilli(location.time))) }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            @Deprecated("Legacy Android callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        manager.requestLocationUpdates(provider, 1_000L, 1f, listener, Looper.getMainLooper())
        awaitClose { manager.removeUpdates(listener) }
    }
    @Suppress("DEPRECATION")
    private suspend fun addressFor(location: Location): String = withContext(Dispatchers.IO) {
        val fallback = "%.5f, %.5f".format(Locale.ROOT, location.latitude, location.longitude)
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching fallback
            Geocoder(context, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.getAddressLine(0)?.takeIf { it.isNotBlank() } ?: fallback
        }.getOrDefault(fallback)
    }
}
