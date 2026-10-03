package com.example.attendance.feature.admin

import androidx.lifecycle.ViewModelStore
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.testing.Fixture
import com.example.attendance.core.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun vm(f: Fixture) = AdminViewModel(
        f.admin,
        f.save,
        f.access,
        f.reset,
        f.send,
        f.decide,
        f.importPhoto,
        f.photos,
        f.history,
        f.calendar,
        f.ensureCalendar,
        f.setZone,
        f.setOffice,
        f.locations,
        f.settings
    )

    @Test
    fun saveBlockResetDeleteAndCallbacksReflectCommittedState() = runTest {
        val f = Fixture(); f.asAdmin();
        val vm = vm(f);
        var callbacks = 0
        vm.save(f.draft()) { callbacks++ }; runCurrent(); assertEquals(
        1,
        callbacks
    ); assertEquals(3, f.repo.read().employees.size)
        vm.access("e1", true) { callbacks++ }; runCurrent(); assertTrue(
        vm.message.value!!.contains(
            "blocked"
        )
    )
        vm.resetPassword(
            "e1",
            "Reset-password-123"
        ) { callbacks++ }; runCurrent(); assertTrue(vm.message.value!!.contains("password"))
        vm.access("e1", true, true) { callbacks++ }; runCurrent(); assertEquals(
        4,
        callbacks
    ); assertNotNull(f.repo.read().employees.first { it.id == "e1" }.deletedAt)
    }

    @Test
    fun invalidActionsShowErrorsWithoutSuccessCallback() = runTest {
        val f = Fixture(); f.asAdmin();
        val vm = vm(f);
        var done = false
        vm.save(f.draft().copy(name = "")) {
            done = true
        }; runCurrent(); assertFalse(done); assertNotNull(vm.error.value); assertFalse(vm.busy.value)
        vm.clearMessage(); assertNull(vm.error.value); vm.calendar(
        "bad-date",
        DayType.WORKING,
        ""
    ); runCurrent(); assertNotNull(vm.error.value)
        vm.generate("not-a-year"); runCurrent(); assertNotNull(vm.error.value)
    }

    @Test
    fun noticePhotoCalendarAndZoneCommandsReachUseCases() = runTest {
        val f = Fixture(); f.asAdmin();
        val vm = vm(f);
        var key = "";
        var sent = false
        vm.importPhoto("uri") { key = it }; runCurrent(); assertTrue(vm.path(key).endsWith(key))
        vm.send(setOf("e1"), "Title", "Body") {
            sent = true
        }; runCurrent(); assertTrue(sent); assertEquals(1, f.repo.read().notices.size)
        vm.calendar(
            "2026-10-06",
            DayType.HOLIDAY,
            "Holiday"
        ); runCurrent(); vm.generate("2028"); runCurrent(); vm.changeZone("Asia/Kolkata"); runCurrent(); assertEquals(
        "Asia/Kolkata",
        vm.zone.value.id
    )
        assertTrue(f.repo.read().calendar.any { it.date.year == 2028 })
    }

    @Test
    fun adminCanUpdateOfficeAttendanceLocation() = runTest {
        val f = Fixture(); f.asAdmin();
        val vm = vm(f)
        f.locations.point = GeoPoint(12.9716, 77.5946, 5f, "Office", f.clock.instant())
        vm.updateOfficeLocationFromCurrent(); runCurrent()
        assertEquals(12.9716, vm.officeLocation.value!!.latitude, 0.00001); assertEquals(
        "Office",
        vm.officeLocation.value!!.address
    ); assertTrue(vm.message.value!!.contains("current location"))
        f.locations.failure =
            DomainException("GPS unavailable"); vm.updateOfficeLocationFromCurrent(); runCurrent(); assertNotNull(
        vm.error.value
    )
    }

    @Test
    fun reviewDecisionUpdatesMessagesAndHomeFlow() = runTest {
        val f = Fixture(); f.asStaff();
        val id = f.submit(f.punch(score = 7000).id); f.asAdmin();
        val vm = vm(f);
        val store = ViewModelStore(); store.put("vm", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.data.collect() }; runCurrent(); assertEquals(
        1,
        vm.data.value!!.reviews.size
    )
        vm.decide(
            id,
            true,
            "OK"
        ); runCurrent(); assertTrue(vm.message.value!!.startsWith("Approved")); assertEquals(
        1,
        vm.data.value!!.checkedIn
    )
        assertNotNull(vm.history("e1", null).first());
        val missing = vm.history("missing", null)
            .first(); assertNull(missing.percentage); assertNotNull(vm.error.value); store.clear()
    }

}
