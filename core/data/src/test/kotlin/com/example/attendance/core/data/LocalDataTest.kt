package com.example.attendance.core.data
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.*
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.*

class LocalDataTest {
    @Test fun hashesAreSaltedAndOnlyCorrectPasswordsMatch() = runTest {
        val hash=Pbkdf2PasswordHasher();val a=hash.hash(PASSWORD);val b=hash.hash(PASSWORD)
        assertNotEquals(a,b);assertFalse(a.contains(PASSWORD));assertTrue(hash.matches(PASSWORD,a));assertFalse(hash.matches("wrong",a))
    }
    @Test fun corruptHashEncodingFailsClosed() = runTest {
        val hash=Pbkdf2PasswordHasher()
        for(value in listOf("","bad","other:600000:AA==:AA==","pbkdf2-sha256:1:AA==:AA==","pbkdf2-sha256:2000001:AA==:AA==","pbkdf2-sha256:invalid:AA==:AA==","pbkdf2-sha256:600000:!:!")) assertFalse(value,hash.matches(PASSWORD,value))
    }
    @Test fun sessionIsMemoryOnlyAndCanBeCleared() = runTest {
        val sessions=MemorySessionStore();val f=Fixture();f.asStaff();assertNull(sessions.session.value);sessions.set(f.sessions.session.value);assertEquals("e1",sessions.session.value!!.employeeId);sessions.set(null);assertNull(sessions.session.value);assertNull(MemorySessionStore().session.value)
    }
    @Test fun allElevenTableModelsRoundTripIncludingEvidenceAndReadState() = runTest {
        val f=Fixture();f.asStaff();val attempt=f.punch(score=7999);val review=f.submit(attempt.id);f.asAdmin();f.decide(review,true,"Verified");f.send(setOf("e1"),"Notice","Body");f.calendar(LocalDate.of(2026,10,6),DayType.HOLIDAY,"Holiday");f.asStaff();f.readNotification(f.repo.read().notifications.last().id)
        val w=f.repo.read()
        w.employees.forEach{assertEquals(it,it.entity().domain())};w.accounts.forEach{assertEquals(it,it.entity().domain())};w.photos.forEach{assertEquals(it,it.entity().domain())}
        w.attempts.forEach{assertEquals(it,it.entity().domain())};w.reviews.forEach{assertEquals(it,it.entity().domain())};w.attendance.forEach{assertEquals(it,it.entity().domain())}
        w.notices.forEach{assertEquals(it,it.entity().domain())};w.recipients.forEach{assertEquals(it,it.entity().domain())};w.notifications.forEach{assertEquals(it,it.entity().domain())}
        w.holidays.forEach{assertEquals(it,it.entity().domain())};w.calendar.forEach{assertEquals(it,it.entity().domain())}
    }
}
