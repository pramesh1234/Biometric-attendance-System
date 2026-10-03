package com.example.attendance.core.domain
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.*
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class CommunicationAndObservationTest {

    @Test fun noticesSnapshotRecipientsAndReadStateIsIdempotent() = runTest {
        val f=Fixture(); f.asAdmin(); f.send(setOf("e1","e2")," Title "," Body "); f.save(f.draft())
        val w=f.repo.read(); assertEquals(2,w.recipients.size); assertEquals(2,w.notifications.size); assertEquals("Title",w.notices.single().title)
        f.asStaff(); val n=f.inbox().first().single(); assertFalse(n.read); assertEquals("Body",n.body); f.readNotification(n.id); f.clock.advanceSeconds(50); f.readNotification(n.id)
        assertTrue(f.inbox().first().single().read); assertEquals(w.notifications.first().createdAt,f.repo.read().notifications.first().readAt)
    }
    @Test fun invalidNoticeSelectionsNeverPartiallyWrite() = runTest {
        val f=Fixture(); f.asAdmin(); expectDomainError { f.send(emptySet(),"x","y") }; expectDomainError { f.send(setOf("e1")," ","y") }; expectDomainError { f.send(setOf("e1"),"x"," ") }; expectDomainError { f.send(setOf("e1","missing"),"x","y") }
        assertTrue(f.repo.read().notices.isEmpty()); f.asStaff(); expectDomainError { f.send(setOf("e1"),"x","y") }
    }
    @Test fun inboxAndHistoryAreRestrictedToOwnEmployee() = runTest {
        val f=Fixture(); f.asAdmin(); f.send(setOf("e2"),"Private","Other staff"); val n=f.repo.read().notifications.single()
        f.asStaff(); assertTrue(f.inbox().first().isEmpty()); expectDomainError { f.readNotification(n.id) }; expectDomainError { f.history("e2",null).first() }
        f.asAdmin(); assertNotNull(f.history("e2",null).first()); expectDomainError { f.history("missing",null).first() }
    }
    @Test fun holidayReplacementAndCalendarGenerationPreserveOverrides() = runTest {
        val f=Fixture(); f.asAdmin(); val date=LocalDate.of(2026,10,6)
        f.calendar(date,DayType.HOLIDAY,"Original"); val id=f.holidays().first().single().id
        f.calendar(date,DayType.HOLIDAY,"Renamed"); assertEquals(id,f.holidays().first().single().id)
        f.ensureCalendar(2026); assertEquals("Renamed",f.holidays().first().single().name)
        f.calendar(date,DayType.WORKING,""); assertTrue(f.holidays().first().isEmpty())
        expectDomainError { f.calendar(date,DayType.HOLIDAY,"") }; expectDomainError { f.ensureCalendar(1969) }; expectDomainError { f.ensureCalendar(2201) }
    }
    @Test fun onlyAdminMayChangeZone() = runTest {
        val f=Fixture(); f.asStaff(); expectDomainError { f.setZone("UTC") }; f.asAdmin(); expectDomainError { f.setZone("invalid") }; f.setZone(" Asia/Kolkata "); assertEquals("Asia/Kolkata",f.settings.zone.value.id)
    }
    @Test fun homeProjectionsAndLogoutAreRoleScoped() = runTest {
        val f=Fixture(); assertNull(f.admin().first()); assertNull(f.staff().first()); assertTrue(f.inbox().first().isEmpty())
        f.asAdmin(); assertEquals(2,f.admin().first()!!.staff.size); assertNull(f.staff().first()); f.calendar(LocalDate.of(2026,10,6),DayType.HOLIDAY,"Upcoming"); f.send(setOf("e1"),"Hello","Body")
        f.asStaff(); val s=f.staff().first()!!; assertEquals("e1",s.profile.employee.id); assertEquals(1,s.unreadCount); assertEquals("Upcoming",s.nextHoliday!!.name); assertNull(s.record)
        f.punch(); assertNotNull(f.staff().first()!!.checkIn); f.punch(PunchAction.CHECK_OUT); assertNotNull(f.staff().first()!!.checkOut)
        f.asAdmin(); assertEquals(1,f.admin().first()!!.checkedIn); f.access("e2",true,true); assertEquals(1,f.admin().first()!!.staff.size)
    }
    @Test fun reviewInboxMessagesFollowDecisionWithoutDuplicatingNoticeContent() = runTest {
        val f=Fixture(); f.asStaff(); val id=f.submit(f.punch(score=6000).id); assertTrue(f.inbox().first().single().title.contains("sent for review")); assertNotNull(f.staff().first()!!.pending)
        f.asAdmin(); f.decide(id,false,"Try better light"); f.asStaff(); val items=f.inbox().first(); assertEquals(2,items.size); assertTrue(items.any{it.title.contains("rejected")&&it.body.contains("Try better light")})
        val next=f.submit(f.punch(score=6000).id); f.asAdmin(); f.decide(next,true,""); f.asStaff(); assertTrue(f.inbox().first().any{it.title.contains("approved")})
    }
    @Test fun mustChangePasswordDoesNotExposeProjections() = runTest {
        val f=Fixture(); f.asAdmin(); f.reset("e1","Reset-password-123"); f.login("staff","Reset-password-123",Role.STAFF)
        assertNull(f.staff().first()); assertNull(f.admin().first()); assertTrue(f.inbox().first().isEmpty())
    }

}
