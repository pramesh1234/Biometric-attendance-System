package com.example.attendance.core.domain
import com.example.attendance.core.model.*
import kotlinx.coroutines.flow.*
import java.time.*
import java.util.UUID

class SendNoticeUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(employeeIds: Set<String>, title: String, body: String) {
        requireRule(employeeIds.isNotEmpty(), "Select at least one recipient.")
        requireRule(title.trim().isNotEmpty() && body.trim().isNotEmpty(), "Enter a notice title and message.")
        repo.update { w ->
            val actor = w.authorize(sessions.session.value, Role.ADMIN)
            requireRule(employeeIds.all { id -> w.employees.any { it.id == id && it.deletedAt == null } }, "A selected employee is no longer active. Refresh recipients.")
            val now = clock.instant(); val notice = Notice(UUID.randomUUID().toString(), actor.id, title.trim(), body.trim(), now)
            val recipients = employeeIds.map { NoticeRecipient(UUID.randomUUID().toString(), notice.id, it) }
            val notifications = recipients.map { InboxNotification(UUID.randomUUID().toString(), it.employeeId, NotificationType.NOTICE, noticeRecipientId = it.id, createdAt = now) }
            w.copy(notices = w.notices + notice, recipients = w.recipients + recipients, notifications = w.notifications + notifications) to Unit
        }
    }
}
class MarkNotificationReadUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(id: String) { repo.update { w ->
        val actor = w.authorize(sessions.session.value, Role.STAFF)
        requireRule(w.notifications.any { it.id == id && it.employeeId == actor.employeeId }, "Notification not found.")
        w.copy(notifications = w.notifications.map { if (it.id == id && it.readAt == null) it.copy(readAt = clock.instant()) else it }) to Unit
    } }
}
class SaveCalendarDayUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(date: LocalDate, type: DayType, holidayName: String) {
        if (type == DayType.HOLIDAY) requireRule(holidayName.trim().isNotEmpty(), "Enter a holiday name.")
        repo.update { w ->
            w.authorize(sessions.session.value, Role.ADMIN); val now = clock.instant()
            val existing = w.calendar.firstOrNull { it.date == date }
            val oldHoliday = w.holidays.firstOrNull { it.id == existing?.holidayId }
            val holiday = if (type == DayType.HOLIDAY) Holiday(oldHoliday?.id ?: UUID.randomUUID().toString(), holidayName.trim(), date, "", oldHoliday?.createdAt ?: now, now) else null
            val day = CalendarDay(existing?.id ?: date.toString(), date, type, holiday?.id, existing?.createdAt ?: now, now)
            w.copy(calendar = w.calendar.filterNot { it.date == date } + day, holidays = if (holiday == null) w.holidays else w.holidays.filterNot { it.id == holiday.id } + holiday) to Unit
        }
    }
}
class EnsureCalendarUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(year: Int) { requireRule(year in 1970..2200, "Enter a year between 1970 and 2200."); repo.update { w ->
        w.authorize(sessions.session.value, Role.ADMIN)
        w.copy(calendar = w.calendar + calendarRange(year, year, clock.instant()).filter { day -> w.calendar.none { it.date == day.date } }) to Unit
    } }
}
class SetOfficeZoneUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val settings: OfficeSettings) {
    suspend operator fun invoke(zone: String) { repo.read().authorize(sessions.session.value, Role.ADMIN); val id = runCatching { ZoneId.of(zone.trim()) }.getOrElse { throw DomainException("Invalid office time zone.") }; settings.setZone(id) }
}
