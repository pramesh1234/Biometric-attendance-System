package com.example.attendance.core.domain

import com.example.attendance.core.model.AttendanceDay
import com.example.attendance.core.model.AttendanceRecord
import com.example.attendance.core.model.AttendanceSummary
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.GeofenceStatus
import com.example.attendance.core.model.InboxNotification
import com.example.attendance.core.model.NotificationType
import com.example.attendance.core.model.Outcome
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.ReviewRequest
import com.example.attendance.core.model.ReviewStatus
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.VerificationAttempt
import com.example.attendance.core.model.Workspace
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class VerificationPolicy {
    val version = "face-score-v2-4000-8000-inclusive"
    fun decide(basisPoints: Int): Outcome {
        requireRule(basisPoints in 0..10000, "Invalid face verification score.")
        return when {
            basisPoints >= 8000 -> Outcome.VERIFIED; basisPoints >= 4000 -> Outcome.REVIEW_REQUIRED; else -> Outcome.FAILED
        }
    }
}

internal fun Workspace.openRecord(employeeId: String) =
    attendance.filter { it.employeeId == employeeId && it.checkOutAttemptId == null }
        .maxByOrNull { it.date }

internal fun Workspace.validatePunch(
    employeeId: String,
    action: PunchAction,
    date: LocalDate,
    now: Instant
) {
    val employee = employees.first { it.id == employeeId }
    requireRule(
        !date.isBefore(employee.joiningDate) && (employee.endDate == null || !date.isAfter(
            employee.endDate
        )), "This date is outside your employment period."
    )
    val pending =
        reviews.any { r -> r.status == ReviewStatus.PENDING && attempts.any { it.id == r.attemptId && it.employeeId == employeeId && it.action == action && it.attendanceDate == date } }
    requireRule(!pending, "This attendance action is already awaiting admin review.")
    val record = attendance.firstOrNull { it.employeeId == employeeId && it.date == date }
    if (action == PunchAction.CHECK_IN) {
        requireRule(record == null, "You have already checked in for this day.")
        requireRule(openRecord(employeeId) == null, "Complete your previous Check Out first.")
    } else {
        requireRule(
            record != null && record.checkOutAttemptId == null,
            "There is no open Check In for this day."
        )
        requireRule(
            !now.isBefore(attempts.first { it.id == record!!.checkInAttemptId }.capturedAt),
            "Check Out cannot be before Check In."
        )
    }
}

internal fun Workspace.applyAttendance(attempt: VerificationAttempt, now: Instant): Workspace {
    val existing =
        attendance.firstOrNull { it.employeeId == attempt.employeeId && it.date == attempt.attendanceDate }
    if (attempt.action == PunchAction.CHECK_IN) {
        requireRule(
            existing == null || existing.checkInAttemptId == attempt.id,
            "A Check In is already recorded for this day. Reject this duplicate request."
        )
        if (existing != null) return this
        val record = AttendanceRecord(
            UUID.randomUUID().toString(),
            attempt.employeeId,
            attempt.attendanceDate,
            attempt.timeZoneId,
            attempt.id,
            createdAt = now,
            updatedAt = now
        )
        return copy(attendance = attendance + record)
    }
    requireRule(existing != null, "Check In must be recorded before approving Check Out.")
    requireRule(
        existing!!.checkOutAttemptId == null || existing.checkOutAttemptId == attempt.id,
        "A Check Out is already recorded. Reject this duplicate request."
    )
    requireRule(
        !attempt.capturedAt.isBefore(attempts.first { it.id == existing.checkInAttemptId }.capturedAt),
        "Check Out cannot be before Check In."
    )
    return copy(attendance = attendance.map {
        if (it.id == existing.id) it.copy(
            checkOutAttemptId = attempt.id,
            updatedAt = now
        ) else it
    })
}

class CaptureAttendanceUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val photos: PhotoStore,
    private val faces: FaceVerifier,
    private val policy: VerificationPolicy,
    private val clock: Clock,
    private val settings: OfficeSettings
) {
    suspend operator fun invoke(
        action: PunchAction,
        imageUri: String,
        location: GeoPoint,
        capturedAt: Instant = clock.instant()
    ): VerificationAttempt {
        val original = repo.read();
        val session = sessions.session.value
        val actor = original.authorize(session, Role.STAFF);
        val employeeId = actor.employeeId!!
        val now = capturedAt;
        val open = original.openRecord(employeeId)
        val zone =
            if (action == PunchAction.CHECK_OUT && open != null) ZoneId.of(open.timeZoneId) else settings.zone.value
        val date =
            if (action == PunchAction.CHECK_OUT && open != null) open.date else LocalDate.ofInstant(
                now,
                zone
            )
        original.validatePunch(employeeId, action, date, now)
        requireRule(
            location.latitude in -90.0..90.0 && location.longitude in -180.0..180.0 && location.accuracyM >= 0f && location.accuracyM.isFinite(),
            "Invalid location. Please try again."
        )
        requireRule(
            Duration.between(location.measuredAt, now).abs() <= Duration.ofMinutes(2),
            "Location is stale. Please refresh and try again."
        )
        val geofence = evaluateGeofence(settings.officeLocation.value, location, now)
        if (geofence.status != GeofenceStatus.INSIDE) throw GeofenceException(geofence)
        val reference =
            original.photos.firstOrNull { it.employeeId == employeeId && it.retiredAt == null }
                ?: throw DomainException("No reference photo. Please contact your admin.")
        val key = photos.importImage(imageUri)
        try {
            val score = faces.compare(reference.fileKey, key)
            val attempt = VerificationAttempt(
                UUID.randomUUID().toString(),
                employeeId,
                reference.id,
                action,
                now,
                date,
                zone.id,
                key,
                location,
                score.basisPoints,
                policy.decide(score.basisPoints),
                score.version,
                policy.version,
                now
            )
            repo.update { w ->
                w.authorize(session, Role.STAFF); w.validatePunch(employeeId, action, date, now)
                val latestGeofence = evaluateGeofence(settings.officeLocation.value, location, now)
                if (latestGeofence.status != GeofenceStatus.INSIDE) throw GeofenceException(
                    latestGeofence
                )
                requireRule(
                    w.photos.any { it.id == reference.id && it.retiredAt == null },
                    "Your reference photo changed. Please retry."
                )
                val next = w.copy(attempts = w.attempts + attempt)
                (if (attempt.outcome == Outcome.VERIFIED) next.applyAttendance(
                    attempt,
                    now
                ) else next) to Unit
            }
            return attempt
        } catch (e: Exception) {
            photos.delete(key); throw e
        }
    }
}

class SubmitReviewUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val clock: Clock
) {
    suspend operator fun invoke(attemptId: String): String = repo.update { w ->
        val actor = w.authorize(sessions.session.value, Role.STAFF)
        val attempt =
            w.attempts.firstOrNull { it.id == attemptId && it.employeeId == actor.employeeId }
                ?: throw DomainException("Attendance attempt not found.")
        requireRule(
            attempt.outcome == Outcome.REVIEW_REQUIRED,
            "This attempt cannot be submitted for review."
        )
        val existing = w.reviews.firstOrNull { it.attemptId == attemptId }
        if (existing != null) w to existing.id else {
            w.validatePunch(
                attempt.employeeId,
                attempt.action,
                attempt.attendanceDate,
                attempt.capturedAt
            )
            val review = ReviewRequest(
                UUID.randomUUID().toString(),
                attemptId,
                ReviewStatus.PENDING,
                clock.instant()
            )
            val notification = InboxNotification(
                UUID.randomUUID().toString(),
                attempt.employeeId,
                NotificationType.REVIEW_SUBMITTED,
                reviewRequestId = review.id,
                createdAt = clock.instant()
            )
            w.copy(
                reviews = w.reviews + review,
                notifications = w.notifications + notification
            ) to review.id
        }
    }
}

class DecideReviewUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val clock: Clock
) {
    suspend operator fun invoke(reviewId: String, approve: Boolean, note: String) {
        repo.update { w ->
            val actor = w.authorize(sessions.session.value, Role.ADMIN)
            val review = w.reviews.firstOrNull { it.id == reviewId }
                ?: throw DomainException("Review request not found.")
            val desired = if (approve) ReviewStatus.APPROVED else ReviewStatus.REJECTED
            if (review.status == desired) w to Unit else {
                requireRule(
                    review.status == ReviewStatus.PENDING,
                    "This request has already been decided."
                )
                val now = clock.instant();
                val attempt = w.attempts.first { it.id == review.attemptId }
                val next = if (approve) w.applyAttendance(attempt, now) else w
                val updated = review.copy(
                    status = desired,
                    reviewedBy = actor.id,
                    reviewedAt = now,
                    note = note.trim()
                )
                val notification = InboxNotification(
                    UUID.randomUUID().toString(),
                    attempt.employeeId,
                    if (approve) NotificationType.REVIEW_APPROVED else NotificationType.REVIEW_REJECTED,
                    reviewRequestId = reviewId,
                    createdAt = now
                )
                next.copy(
                    reviews = next.reviews.map { if (it.id == reviewId) updated else it },
                    notifications = next.notifications + notification
                ) to Unit
            }
        }
    }
}

class CalculateAttendanceUseCase(private val clock: Clock, private val settings: OfficeSettings) {
    operator fun invoke(
        w: Workspace,
        employeeId: String,
        from: LocalDate,
        until: LocalDate
    ): AttendanceSummary {
        val employee = w.employees.first { it.id == employeeId };
        val today = LocalDate.now(clock.withZone(settings.zone.value))
        val start = maxOf(from, employee.joiningDate);
        val end = minOf(until, today, employee.endDate ?: today)
        if (end < start) return AttendanceSummary(null, 0, 0, emptyList())
        val calendar = w.calendar.associateBy { it.date };
        val attempts = w.attempts.associateBy { it.id }
        val records = w.attendance.filter { it.employeeId == employeeId }.associateBy { it.date }
        val pending = w.reviews.filter { it.status == ReviewStatus.PENDING }
            .mapNotNull { attempts[it.attemptId] }.filter { it.employeeId == employeeId }
            .map { it.attendanceDate }.toSet()
        val days = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= end }.map { date ->
            val day = calendar[date]
                ?: throw DomainException("Working calendar is incomplete for $date. Ask your admin to generate it.")
            val record = records[date]
            AttendanceDay(
                date,
                day.type,
                record?.checkInAttemptId?.let { attempts[it]?.capturedAt },
                record?.checkOutAttemptId?.let { attempts[it]?.capturedAt },
                date in pending,
                record?.timeZoneId ?: settings.zone.value.id
            )
        }.toList()
        // Current workday enters the denominator when complete, or after its date has ended.
        val eligible =
            days.filter { it.type == DayType.WORKING && (it.date < today || (it.checkIn != null && it.checkOut != null)) }
        val attended = eligible.count { it.checkIn != null && it.checkOut != null }
        return AttendanceSummary(
            if (eligible.isEmpty()) null else attended * 100.0 / eligible.size,
            attended,
            eligible.size,
            days.reversed()
        )
    }
}
