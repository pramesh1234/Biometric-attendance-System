package com.example.attendance.core.domain

import com.example.attendance.core.model.AdminData
import com.example.attendance.core.model.AttendanceSummary
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.Holiday
import com.example.attendance.core.model.InboxItem
import com.example.attendance.core.model.NotificationType
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.ReviewStatus
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.StaffData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun minuteTicks() = flow {
    while (true) {
        emit(Unit); delay(60_000)
    }
}

class ObserveAdminUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val clock: Clock,
    private val settings: OfficeSettings
) {
    operator fun invoke(): Flow<AdminData?> =
        combine(repo.observe(), sessions.session, settings.zone, minuteTicks()) { w, s, zone, _ ->
            if (s?.role != Role.ADMIN || s.mustChangePassword) null else {
                w.authorize(s, Role.ADMIN)
                AdminData(
                    w.employees.filter { it.deletedAt == null }.sortedBy { it.name }
                        .map(w::profile),
                    w.reviews.sortedByDescending { it.submittedAt }.map(w::detail),
                    w.attendance.count { it.date == LocalDate.now(clock.withZone(zone)) },
                    w.holidays.filter { h -> w.calendar.any { it.holidayId == h.id && it.type == DayType.HOLIDAY } }
                        .sortedBy { it.date })
            }
        }
}

class ObserveStaffUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val clock: Clock,
    private val settings: OfficeSettings,
    private val calculate: CalculateAttendanceUseCase
) {
    operator fun invoke(): Flow<StaffData?> =
        combine(repo.observe(), sessions.session, settings.zone, minuteTicks()) { w, s, zone, _ ->
            if (s?.role != Role.STAFF || s.mustChangePassword) null else {
                val actor = w.authorize(s, Role.STAFF);
                val employee = w.employees.first { it.id == actor.employeeId };
                val today = LocalDate.now(clock.withZone(zone))
                val record = w.openRecord(employee.id)
                    ?: w.attendance.firstOrNull { it.employeeId == employee.id && it.date == today }
                val pending = w.reviews.filter { it.status == ReviewStatus.PENDING }.map(w::detail)
                    .firstOrNull { it.employee.id == employee.id }
                StaffData(
                    w.profile(employee),
                    record,
                    w.attempts.firstOrNull { it.id == record?.checkInAttemptId }?.capturedAt,
                    w.attempts.firstOrNull { it.id == record?.checkOutAttemptId }?.capturedAt,
                    pending,
                    calculate(w, employee.id, today.withDayOfMonth(1), today),
                    w.holidays.filter { h -> h.date >= today && w.calendar.any { it.holidayId == h.id && it.type == DayType.HOLIDAY } }
                        .minByOrNull { it.date },
                    w.notifications.count { it.employeeId == employee.id && it.readAt == null })
            }
        }
}

class ObserveAttendanceUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore,
    private val calculate: CalculateAttendanceUseCase
) {
    operator fun invoke(employeeId: String, month: YearMonth?): Flow<AttendanceSummary> =
        repo.observe().map { w ->
            val actor = w.authorize(sessions.session.value)
            requireRule(
                actor.role == Role.ADMIN || actor.employeeId == employeeId,
                "You can only view your own attendance."
            )
            val employee = w.employees.firstOrNull { it.id == employeeId } ?: throw DomainException(
                "Employee not found."
            )
            calculate(
                w,
                employeeId,
                month?.atDay(1) ?: employee.joiningDate,
                month?.atEndOfMonth() ?: LocalDate.MAX
            )
        }
}

class ObserveInboxUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore
) {
    operator fun invoke(): Flow<List<InboxItem>> =
        combine(repo.observe(), sessions.session) { w, s ->
            if (s?.role != Role.STAFF || s.mustChangePassword) emptyList() else {
                val actor = w.authorize(s, Role.STAFF)
                w.notifications.filter { it.employeeId == actor.employeeId }
                    .sortedByDescending { it.createdAt }.map { n ->
                    if (n.type == NotificationType.NOTICE) {
                        val recipient = w.recipients.first { it.id == n.noticeRecipientId };
                        val notice = w.notices.first { it.id == recipient.noticeId }
                        InboxItem(
                            n.id,
                            n.type,
                            notice.title,
                            notice.body,
                            n.createdAt,
                            n.readAt != null
                        )
                    } else {
                        val r = w.reviews.first { it.id == n.reviewRequestId };
                        val a = w.attempts.first { it.id == r.attemptId }
                        val action =
                            if (a.action == PunchAction.CHECK_IN) "Check In" else "Check Out"
                        val time = DateTimeFormatter.ofPattern("d MMM, hh:mm a")
                            .withZone(ZoneId.of(a.timeZoneId)).format(a.capturedAt)
                        val title = when (n.type) {
                            NotificationType.REVIEW_SUBMITTED -> "$action sent for review"; NotificationType.REVIEW_APPROVED -> "$action approved"; else -> "$action rejected"
                        }
                        val body = when (n.type) {
                            NotificationType.REVIEW_SUBMITTED -> "Your capture at $time was submitted for review. Current request status: ${r.status.name.lowercase()}."; NotificationType.REVIEW_APPROVED -> "Your capture at $time has been recorded."; else -> "Your capture at $time was not marked. ${r.note}"
                        }
                        InboxItem(n.id, n.type, title, body, n.createdAt, n.readAt != null)
                    }
                }
            }
        }
}

class ObserveHolidaysUseCase(
    private val repo: WorkspaceRepository,
    private val sessions: SessionStore
) {
    operator fun invoke(): Flow<List<Holiday>> = repo.observe().map { w ->
        w.authorize(sessions.session.value); w.holidays.filter { h -> w.calendar.any { it.type == DayType.HOLIDAY && it.holidayId == h.id } }
        .sortedBy { it.date }
    }
}
