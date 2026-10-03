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

class AttendanceTest {

    @Test fun allBasisPointValuesFollowInclusiveEightyPercentBoundary() {
        val policy=VerificationPolicy()
        for(score in 0..3999) assertEquals(Outcome.FAILED,policy.decide(score))
        for(score in 4000..7999) assertEquals(Outcome.REVIEW_REQUIRED,policy.decide(score))
        for(score in 8000..10000) assertEquals(Outcome.VERIFIED,policy.decide(score))
        assertEquals("face-score-v2-4000-8000-inclusive",policy.version)
    }
    @Test fun invalidScoresAreRejected() = runTest { val p=VerificationPolicy(); expectDomainError { p.decide(-1) }; expectDomainError { p.decide(10001) } }
    @Test fun exactlyEightyMarksBothPunchesAndRejectsDuplicates() = runTest {
        val f=Fixture(); f.asStaff(); val first=f.punch(score=8000); assertEquals(Outcome.VERIFIED,first.outcome); assertEquals(f.policy.version,first.policyVersion)
        expectDomainError { f.punch() }; assertEquals(1,f.repo.read().attempts.size)
        f.clock.advanceSeconds(3600); val out=f.punch(PunchAction.CHECK_OUT,10000); val record=f.repo.read().attendance.single()
        assertEquals(first.id,record.checkInAttemptId); assertEquals(out.id,record.checkOutAttemptId); expectDomainError { f.punch(PunchAction.CHECK_OUT) }
    }
    @Test fun retryKeepsAttemptsAndDoesNotMarkFailedOrReviewRequired() = runTest {
        val f=Fixture(); f.asStaff(); f.punch(score=3999); f.punch(score=4000); f.punch(score=7999)
        assertEquals(listOf(Outcome.FAILED,Outcome.REVIEW_REQUIRED,Outcome.REVIEW_REQUIRED),f.repo.read().attempts.map{it.outcome}); assertTrue(f.repo.read().attendance.isEmpty())
    }
    @Test fun checkoutRequiresEarlierCheckin() = runTest {
        val f=Fixture(); f.asStaff(); expectDomainError { f.punch(PunchAction.CHECK_OUT) }; f.punch(); f.clock.advanceSeconds(-1); expectDomainError { f.punch(PunchAction.CHECK_OUT) }
    }
    @Test fun overnightCheckoutUsesOriginalDateAndTimeZone() = runTest {
        val f=Fixture(); f.asStaff(); f.clock.now=Instant.parse("2026-10-05T23:55:00Z"); val first=f.punch()
        f.clock.now=Instant.parse("2026-10-06T01:00:00Z"); f.settings.setZone(ZoneId.of("Asia/Kolkata")); expectDomainError { f.punch() }
        val out=f.punch(PunchAction.CHECK_OUT); assertEquals(first.attendanceDate,out.attendanceDate); assertEquals(first.timeZoneId,out.timeZoneId)
        f.punch(); assertEquals(2,f.repo.read().attendance.size)
    }
    @Test fun punchRejectsEmploymentBoundsMissingPhotoAndInvalidLocation() = runTest {
        val f=Fixture(); f.asStaff(); f.clock.now=Instant.parse("2026-09-30T12:00:00Z"); expectDomainError { f.punch() }
        f.clock.now=Instant.parse("2026-10-05T12:00:00Z"); val loc=f.locations.current()
        for(bad in listOf(loc.copy(latitude=91.0),loc.copy(longitude=181.0),loc.copy(latitude=Double.NaN),loc.copy(accuracyM=-1f),loc.copy(accuracyM=Float.NaN),loc.copy(measuredAt=loc.measuredAt.minusSeconds(121)))) expectDomainError { f.capture(PunchAction.CHECK_IN,"uri",bad) }
        f.repo.update{w->w.copy(photos=emptyList()) to Unit}; expectDomainError { f.punch() }; assertTrue(f.photos.imported.isEmpty())
    }
    @Test fun geofenceBlocksPunchesOutsideOneHundredMetersBeforeImportingPhoto() = runTest {
        val f=Fixture(); f.asStaff(); f.settings.setOfficeLocation(OfficeLocation(0.0,0.0))
        expectDomainError { f.punch() }
        assertTrue(f.photos.imported.isEmpty()); assertTrue(f.repo.read().attempts.isEmpty()); assertTrue(f.repo.read().attendance.isEmpty())
    }
    @Test fun geofenceAllowsExactBoundaryAndRejectsJustOutside() = runTest {
        val office=OfficeLocation(0.0,0.0)
        val exact=GeoPoint(Math.toDegrees(100.0 / 6371008.8),0.0,5f,"boundary",Instant.parse("2026-10-05T12:00:00Z"))
        val outside=GeoPoint(Math.toDegrees(100.02 / 6371008.8),0.0,5f,"outside",exact.measuredAt)
        assertEquals(GeofenceStatus.INSIDE,evaluateGeofence(office,exact,exact.measuredAt).status)
        assertEquals(GeofenceStatus.OUTSIDE,evaluateGeofence(office,outside,outside.measuredAt).status)
        assertEquals(GeofenceStatus.NOT_CONFIGURED,evaluateGeofence(null,exact,exact.measuredAt).status)
    }
    @Test fun verifierFailureCleansFileAndWritesNothing() = runTest {
        val f=Fixture(); f.asStaff(); f.faces.failure=DomainException("model error"); expectDomainError { f.punch() }
        assertEquals(f.photos.imported,f.photos.deleted); assertTrue(f.repo.read().attempts.isEmpty())
    }
    @Test fun referenceReplacementDuringInferenceRejectsStaleEvidence() = runTest {
        val f=Fixture(); f.asStaff(); f.faces.beforeCompare={f.repo.update{w->w.copy(photos=w.photos.map{it.copy(retiredAt=f.clock.instant())}) to Unit}}
        expectDomainError { f.punch() }; assertEquals(1,f.photos.deleted.size); assertTrue(f.repo.read().attendance.isEmpty())
    }
    @Test fun accountBlockDuringInferenceRejectsAndCleansCapture() = runTest {
        val f=Fixture(); f.asStaff(); f.faces.beforeCompare={f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(status=AccountStatus.BLOCKED)else it}) to Unit}}
        expectDomainError { f.punch() }; assertEquals(1,f.photos.deleted.size)
    }
    @Test fun concurrentCheckinsProduceExactlyOneRecord() = runTest {
        val f=Fixture(); f.asStaff(); val results=(1..2).map{async {runCatching{f.punch()}}}.awaitAll()
        assertEquals(1,results.count{it.isSuccess}); assertEquals(1,f.repo.read().attendance.size); assertEquals(1,f.repo.read().attempts.size)
    }

}
