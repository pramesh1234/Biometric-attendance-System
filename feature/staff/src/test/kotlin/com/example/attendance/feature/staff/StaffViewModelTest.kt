package com.example.attendance.feature.staff
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.*
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {
 @get:Rule val main=MainDispatcherRule()

    private fun vm(f:Fixture)=StaffViewModel(f.staff,f.inbox,f.holidays,f.history,f.capture,f.submit,f.readNotification,f.locations,f.photos,f.settings,f.clock,f.geofence)
    @Test fun captureUsesInjectedClockAndUpdatesCheckinThenCheckout() = runTest {
        val f=Fixture();f.asStaff();val vm=vm(f)
        vm.capture("uri",false);runCurrent();assertEquals(Outcome.VERIFIED,vm.attempt.value!!.outcome);assertEquals(f.clock.instant(),vm.attempt.value!!.capturedAt)
        vm.retry();assertNull(vm.attempt.value);f.clock.advanceSeconds(60);vm.capture("uri",true);runCurrent();assertEquals(PunchAction.CHECK_OUT,vm.attempt.value!!.action)
        assertNotNull(f.repo.read().attendance.single().checkOutAttemptId)
    }
    @Test fun reviewAndRetryHaveDistinctStates() = runTest {
        val f=Fixture();f.asStaff();f.faces.score=7999;val vm=vm(f)
        vm.capture("uri",false);runCurrent();assertEquals(Outcome.REVIEW_REQUIRED,vm.attempt.value!!.outcome)
        vm.submit();runCurrent();assertTrue(vm.submitted.value);assertEquals(1,f.repo.read().reviews.size)
        vm.retry();assertFalse(vm.submitted.value);assertNull(vm.attempt.value);assertNull(vm.error.value)
    }
    @Test fun failedCaptureAndMissingAttemptNeverMarkAttendance() = runTest {
        val f=Fixture();f.asStaff();val vm=vm(f);vm.submit();runCurrent();assertNotNull(vm.error.value)
        f.faces.score=3999;vm.capture("uri",false);runCurrent();assertEquals(Outcome.FAILED,vm.attempt.value!!.outcome);assertTrue(f.repo.read().attendance.isEmpty())
    }
    @Test fun locationFailureResetsBusyWithoutCallingVerifier() = runTest {
        val f=Fixture();f.asStaff();val vm=vm(f);f.locations.failure=DomainException("GPS unavailable")
        vm.capture("uri",false);runCurrent();assertEquals("GPS unavailable",vm.error.value);assertFalse(vm.busy.value);assertTrue(f.photos.imported.isEmpty())
        f.locations.failure=null;vm.refreshLocation();runCurrent();assertNotNull(vm.location.value);assertNull(vm.error.value)
    }
    @Test fun geofenceFailureShowsWarningAndSkipsVerifier() = runTest {
        val f=Fixture();f.asStaff();val vm=vm(f);f.settings.setOfficeLocation(OfficeLocation(0.0,0.0))
        vm.capture("uri",false);runCurrent();assertTrue(vm.geofenceWarning.value!!.contains("100-meter"));assertFalse(vm.busy.value);assertTrue(f.photos.imported.isEmpty())
        vm.dismissGeofenceWarning();assertNull(vm.geofenceWarning.value)
    }
    @Test fun homeInboxHolidayAndHistoryStateFollowRepository() = runTest {
        val f=Fixture();f.asAdmin();f.send(setOf("e1"),"Notice","Body");f.calendar(LocalDate.of(2026,10,6),DayType.HOLIDAY,"Holiday");f.asStaff();val vm=vm(f);val store=ViewModelStore();store.put("vm",vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.data.collect()}
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.notifications.collect()}
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){vm.holidays.collect()};runCurrent()
        assertEquals(1,vm.data.value!!.unreadCount);assertEquals(1,vm.holidays.value.size)
        vm.markRead(vm.notifications.value.single().id);runCurrent();assertTrue(vm.notifications.value.single().read);assertEquals(0,vm.data.value!!.unreadCount)
        assertEquals(2,vm.history("e1",YearMonth.of(2026,10)).first().eligible)
        assertNull(vm.history("e2",YearMonth.of(2026,10)).first().percentage);assertNotNull(vm.error.value);store.clear()
    }
    @Test fun repeatedCaptureTapIsSerializedAndCancellationCleansState() = runTest {
        val f=Fixture();f.asStaff();val vm=vm(f);val store=ViewModelStore();store.put("vm",vm);var calls=0
        f.faces.beforeCompare={calls++;awaitCancellation()};vm.capture("uri",false);vm.capture("uri",false);runCurrent()
        assertEquals(1,calls);assertTrue(vm.busy.value);store.clear();runCurrent();assertFalse(vm.busy.value);assertNull(vm.error.value)
    }

}
