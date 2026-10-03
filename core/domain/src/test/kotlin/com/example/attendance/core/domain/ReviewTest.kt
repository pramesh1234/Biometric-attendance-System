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

class ReviewTest {

    @Test fun submittingIsIdempotentAndBlocksDuplicateAction() = runTest {
        val f=Fixture(); f.asStaff(); val a=f.punch(score=7999); val id=f.submit(a.id); assertEquals(id,f.submit(a.id))
        assertEquals(1,f.repo.read().reviews.size); assertEquals(1,f.repo.read().notifications.size); expectDomainError { f.punch() }
    }
    @Test fun failedVerifiedOtherEmployeeAndMissingAttemptsCannotBeSubmitted() = runTest {
        val f=Fixture(); f.asStaff(); val failed=f.punch(score=0); expectDomainError { f.submit(failed.id) }; expectDomainError { f.submit("missing") }
        val review=f.punch(score=6000); f.asStaff("e2"); expectDomainError { f.submit(review.id) }; val verified=f.punch(); expectDomainError { f.submit(verified.id) }
    }
    @Test fun approveAtomicallyRecordsCaptureTimeAndNotifiesOnlyOnce() = runTest {
        val f=Fixture(); f.asStaff(); val a=f.punch(score=7000); val id=f.submit(a.id); f.clock.advanceSeconds(600); f.asAdmin(); f.decide(id,true," Accepted "); f.decide(id,true,"Again")
        val w=f.repo.read(); assertEquals(a.id,w.attendance.single().checkInAttemptId); assertEquals(a.capturedAt,w.attempts.single().capturedAt)
        assertEquals("Accepted",w.reviews.single().note); assertEquals(ReviewStatus.APPROVED,w.reviews.single().status); assertEquals(2,w.notifications.size)
        expectDomainError { f.decide(id,false,"") }
    }
    @Test fun rejectLeavesAttendanceUnmarkedAndAllowsRetry() = runTest {
        val f=Fixture(); f.asStaff(); val id=f.submit(f.punch(score=6000).id); f.asAdmin(); f.decide(id,false,"Blurry"); f.decide(id,false,"again")
        assertTrue(f.repo.read().attendance.isEmpty()); assertEquals(NotificationType.REVIEW_REJECTED,f.repo.read().notifications.last().type)
        expectDomainError { f.decide(id,true,"") }; f.asStaff(); f.punch(); assertEquals(1,f.repo.read().attendance.size)
    }
    @Test fun approveCheckoutCompletesExistingRecord() = runTest {
        val f=Fixture(); f.asStaff(); f.punch(); f.clock.advanceSeconds(60); val out=f.punch(PunchAction.CHECK_OUT,7900); val id=f.submit(out.id)
        f.asAdmin(); f.decide(id,true,""); assertEquals(out.id,f.repo.read().attendance.single().checkOutAttemptId)
    }
    @Test fun onlyAdminMayDecideAndMissingReviewFails() = runTest {
        val f=Fixture(); f.asStaff(); val id=f.submit(f.punch(score=6000).id); expectDomainError { f.decide(id,true,"") }
        f.asAdmin(); expectDomainError { f.decide("missing",true,"") }
    }
    @Test fun approvalCannotOverwriteExistingPunch() = runTest {
        val f=Fixture(); f.asStaff(); val a=f.punch(score=6000); val id=f.submit(a.id)
        f.repo.update{w->w.copy(attendance=listOf(AttendanceRecord("existing","e1",a.attendanceDate,a.timeZoneId,"different",createdAt=a.capturedAt,updatedAt=a.capturedAt))) to Unit}
        f.asAdmin(); expectDomainError { f.decide(id,true,"") }; assertEquals(ReviewStatus.PENDING,f.repo.read().reviews.single().status)
    }

}
