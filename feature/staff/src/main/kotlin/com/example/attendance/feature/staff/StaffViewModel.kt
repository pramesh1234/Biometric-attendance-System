package com.example.attendance.feature.staff

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.core.domain.CaptureAttendanceUseCase
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.domain.GeofenceException
import com.example.attendance.core.domain.GeofenceReading
import com.example.attendance.core.domain.LocationProvider
import com.example.attendance.core.domain.MarkNotificationReadUseCase
import com.example.attendance.core.domain.ObserveAttendanceUseCase
import com.example.attendance.core.domain.ObserveGeofenceUseCase
import com.example.attendance.core.domain.ObserveHolidaysUseCase
import com.example.attendance.core.domain.ObserveInboxUseCase
import com.example.attendance.core.domain.ObserveStaffUseCase
import com.example.attendance.core.domain.OfficeSettings
import com.example.attendance.core.domain.PhotoStore
import com.example.attendance.core.domain.SubmitReviewUseCase
import com.example.attendance.core.domain.message
import com.example.attendance.core.model.AttendanceSummary
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.GeofenceState
import com.example.attendance.core.model.GeofenceStatus
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.VerificationAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class StaffViewModel @Inject constructor(
    observe: ObserveStaffUseCase,
    inbox: ObserveInboxUseCase,
    holidays: ObserveHolidaysUseCase,
    private val history: ObserveAttendanceUseCase,
    private val capture: CaptureAttendanceUseCase,
    private val submit: SubmitReviewUseCase,
    private val markRead: MarkNotificationReadUseCase,
    private val locationProvider: LocationProvider,
    private val photos: PhotoStore,
    settings: OfficeSettings,
    private val clock: Clock,
    observeGeofence: ObserveGeofenceUseCase
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null);
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false);
    val busy = _busy.asStateFlow()
    private val _location = MutableStateFlow<GeoPoint?>(null);
    val location = _location.asStateFlow()
    private val _attempt = MutableStateFlow<VerificationAttempt?>(null);
    val attempt = _attempt.asStateFlow()
    private val _submitted = MutableStateFlow(false);
    val submitted = _submitted.asStateFlow()
    private val _geofenceWarning = MutableStateFlow<String?>(null);
    val geofenceWarning = _geofenceWarning.asStateFlow()
    val zone = settings.zone
    val geofence = observeGeofence().onEach { reading ->
        val state = reading.state

        Log.d(
            "GEOFENCE",
            """
            status=${state.status}
            distance=${state.distanceM}
            accuracy=${reading.location?.accuracyM}
            measuredAt=${reading.location?.measuredAt}
            officeLat=${state.office?.latitude}
            officeLng=${state.office?.longitude}
            currentLat=${reading.location?.latitude}
            currentLng=${reading.location?.longitude}
            """.trimIndent()
        )

        _location.value =
            reading.location; if (reading.state.status == GeofenceStatus.INSIDE) _geofenceWarning.value =
        null
    }.catch { emit(GeofenceReading(null, GeofenceState(GeofenceStatus.UNAVAILABLE))) }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(0),
        GeofenceReading(null, GeofenceState(GeofenceStatus.WAITING))
    )
    val data = observe().catch { _error.value = it.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val notifications = inbox().catch { _error.value = it.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holidays = holidays().catch { _error.value = it.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun history(id: String, month: YearMonth) = history.invoke(id, month)
        .catch { _error.value = it.message; emit(AttendanceSummary(null, 0, 0, emptyList())) }

    fun path(key: String) = photos.path(key)
    fun refreshLocation() = run { _location.value = locationProvider.current() }
    fun capture(uri: String, checkOut: Boolean) = run {
        val capturedAt = clock.instant();
        val location = locationProvider.current(); _location.value = location; _attempt.value =
        capture.invoke(
            if (checkOut) PunchAction.CHECK_OUT else PunchAction.CHECK_IN,
            uri,
            location,
            capturedAt
        )
    }

    fun retry() {
        _attempt.value = null; _submitted.value = false; _error.value = null
    }

    fun showGeofenceWarning(message: String) {
        _geofenceWarning.value = message
    }

    fun dismissGeofenceWarning() {
        _geofenceWarning.value = null
    }

    fun submit() = run {
        submit.invoke(
            _attempt.value?.id ?: throw DomainException("Capture a photo first.")
        ); _submitted.value = true
    }

    fun markRead(id: String) = run { markRead.invoke(id) }
    private fun run(block: suspend () -> Unit) {
        if (!_busy.compareAndSet(false, true)) return; viewModelScope.launch {
            _error.value = null; try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeofenceException) {
            _geofenceWarning.value = e.message ?: e.state.message()
        } catch (e: Exception) {
            _error.value = e.message ?: "Unable to complete this action."
        } finally {
            _busy.value = false
        }
        }
    }
}
