package com.example.attendance.core.domain

import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.Workspace
import com.example.attendance.core.testing.Fixture
import com.example.attendance.core.testing.expectDomainError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalculationTest {

    @Test
    fun onlyCompletedWorkingDaysCountAndTodayIsProvisional() = runTest {
        val f = Fixture(); f.asStaff();
        val month = YearMonth.of(2026, 10)
        fun summary(w: Workspace) = f.calculate(w, "e1", month.atDay(1), month.atEndOfMonth())
        assertEquals(2, summary(f.repo.read()).eligible); f.punch(); assertEquals(
        0,
        summary(f.repo.read()).attended
    ); assertEquals(2, summary(f.repo.read()).eligible)
        f.clock.advanceSeconds(60); f.punch(PunchAction.CHECK_OUT);
        val s = summary(f.repo.read()); assertEquals(3, s.eligible); assertEquals(
        1,
        s.attended
    ); assertEquals(100.0 / 3, s.percentage!!, 0.001)
        assertEquals(
            LocalDate.of(2026, 10, 5),
            s.days.first().date
        ); assertNotNull(s.days.first().checkIn); assertNotNull(s.days.first().checkOut)
    }

    @Test
    fun zeroEligibleDaysIsNotZeroPercent() = runTest {
        val f = Fixture();
        val w = f.repo.read();
        val result = f.calculate(
            w,
            "e1",
            LocalDate.of(2026, 10, 10),
            LocalDate.of(2026, 10, 12)
        ); assertNull(result.percentage); assertEquals(0, result.eligible)
    }

    @Test
    fun holidaysAndWeekendOverridesChangeDenominator() = runTest {
        val f = Fixture(); f.asAdmin(); f.calendar(
        LocalDate.of(2026, 10, 1),
        DayType.HOLIDAY,
        "Holiday"
    )
        var s = f.calculate(
            f.repo.read(),
            "e1",
            LocalDate.of(2026, 10, 1),
            LocalDate.MAX
        ); assertEquals(1, s.eligible)
        f.calendar(LocalDate.of(2026, 10, 3), DayType.WORKING, ""); s =
        f.calculate(f.repo.read(), "e1", LocalDate.of(2026, 10, 1), LocalDate.MAX); assertEquals(
        2,
        s.eligible
    )
    }

    @Test
    fun joiningAndEndDatesBoundHistory() = runTest {
        val f = Fixture(); f.repo.update { w ->
        w.copy(employees = w.employees.map {
            if (it.id == "e1") it.copy(
                joiningDate = LocalDate.of(2026, 10, 2),
                endDate = LocalDate.of(2026, 10, 3)
            ) else it
        }) to Unit
    }
        val s =
            f.calculate(f.repo.read(), "e1", LocalDate.of(2026, 1, 1), LocalDate.MAX); assertEquals(
        1,
        s.eligible
    ); assertEquals(2, s.days.size)
    }

    @Test
    fun incompleteCalendarIsExplicitError() = runTest {
        val f = Fixture();
        val w = f.repo.read().copy(calendar = emptyList()); expectDomainError {
        f.calculate(
            w,
            "e1",
            LocalDate.of(2026, 10, 1),
            LocalDate.MAX
        )
    }
    }

    @Test
    fun pendingReviewAppearsWithoutIncreasingAttendance() = runTest {
        val f = Fixture(); f.asStaff(); f.submit(f.punch(score = 7000).id);
        val s = f.history("e1", YearMonth.of(2026, 10))
            .first(); assertTrue(s.days.first().pending); assertEquals(0, s.attended)
    }

    @Test
    fun leapYearAndCrossMonthHistoryAreComplete() = runTest {
        val f = Fixture(); f.asAdmin(); f.ensureCalendar(2024); f.repo.update { w ->
        w.copy(
            employees = w.employees.map {
                if (it.id == "e1") it.copy(
                    joiningDate = LocalDate.of(
                        2024,
                        2,
                        1
                    )
                ) else it
            }) to Unit
    }
        val s = f.history("e1", YearMonth.of(2024, 2)).first(); assertEquals(
        29,
        s.days.size
    ); assertEquals(21, s.eligible)
    }

}
