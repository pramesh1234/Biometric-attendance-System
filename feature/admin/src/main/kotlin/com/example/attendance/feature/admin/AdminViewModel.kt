package com.example.attendance.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.core.domain.DecideReviewUseCase
import com.example.attendance.core.domain.EnsureCalendarUseCase
import com.example.attendance.core.domain.ImportReferencePhotoUseCase
import com.example.attendance.core.domain.LocationProvider
import com.example.attendance.core.domain.ObserveAdminUseCase
import com.example.attendance.core.domain.ObserveAttendanceUseCase
import com.example.attendance.core.domain.OfficeSettings
import com.example.attendance.core.domain.PhotoStore
import com.example.attendance.core.domain.ResetStaffPasswordUseCase
import com.example.attendance.core.domain.SaveCalendarDayUseCase
import com.example.attendance.core.domain.SaveStaffUseCase
import com.example.attendance.core.domain.SendNoticeUseCase
import com.example.attendance.core.domain.SetOfficeLocationUseCase
import com.example.attendance.core.domain.SetOfficeZoneUseCase
import com.example.attendance.core.domain.SetStaffAccessUseCase
import com.example.attendance.core.model.AttendanceSummary
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.StaffDraft
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class AdminViewModel @Inject constructor(
    observe: ObserveAdminUseCase,
    private val saveStaff: SaveStaffUseCase,
    private val setAccess: SetStaffAccessUseCase,
    private val reset: ResetStaffPasswordUseCase,
    private val send: SendNoticeUseCase,
    private val decide: DecideReviewUseCase,
    private val importPhoto: ImportReferencePhotoUseCase,
    private val photos: PhotoStore,
    private val history: ObserveAttendanceUseCase,
    private val saveCalendar: SaveCalendarDayUseCase,
    private val ensureCalendar: EnsureCalendarUseCase,
    private val setZone: SetOfficeZoneUseCase,
    private val setOffice: SetOfficeLocationUseCase,
    private val locations: LocationProvider,
    settings: OfficeSettings
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null);
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false);
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null);
    val message = _message.asStateFlow()
    val zone = settings.zone
    val officeLocation = settings.officeLocation
    val data = observe().catch { _error.value = it.message }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun history(id: String, month: YearMonth?) = history.invoke(id, month)
        .catch { _error.value = it.message; emit(AttendanceSummary(null, 0, 0, emptyList())) }

    fun path(key: String) = photos.path(key)
    fun clearMessage() {
        _message.value = null; _error.value = null
    }

    fun importPhoto(uri: String, onImported: (String) -> Unit) =
        run { onImported(importPhoto.invoke(uri)) }

    fun save(draft: StaffDraft, done: () -> Unit) = run { saveStaff(draft); done() }
    fun access(id: String, block: Boolean, delete: Boolean = false, done: () -> Unit = {}) = run {
        setAccess(id, block, delete); _message.value =
        if (delete) "Employee removed. History is preserved." else if (block) "Employee blocked." else "Employee unblocked."; done()
    }

    fun resetPassword(id: String, password: String, done: () -> Unit) =
        run { reset(id, password); _message.value = "Temporary password updated."; done() }

    fun send(ids: Set<String>, title: String, body: String, done: () -> Unit) =
        run { send.invoke(ids, title, body); done() }

    fun decide(id: String, approved: Boolean, note: String) = run {
        decide.invoke(id, approved, note); _message.value =
        if (approved) "Approved. Attendance and staff notification updated." else "Rejected. Staff notified."
    }

    fun calendar(date: String, type: DayType, holidayName: String) = run {
        saveCalendar(LocalDate.parse(date), type, holidayName); _message.value = "Calendar updated."
    }

    fun generate(year: String) =
        run { ensureCalendar(year.toInt()); _message.value = "Missing calendar dates generated." }

    fun changeZone(zone: String) = run {
        setZone(zone); _message.value =
        "Office time zone updated. Historical capture zones are unchanged."
    }

    fun updateOfficeLocationFromCurrent() = run {
        val location = locations.current(); setOffice(
        location.latitude,
        location.longitude,
        location.label
    ); _message.value = "Office attendance location updated from current location."
    }

    private fun run(block: suspend () -> Unit) {
        if (!_busy.compareAndSet(false, true)) return; viewModelScope.launch {
            clearMessage(); try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value = e.message ?: "Unable to complete action."
        } finally {
            _busy.value = false
        }
        }
    }
}
