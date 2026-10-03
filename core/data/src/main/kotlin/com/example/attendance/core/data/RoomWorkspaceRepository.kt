package com.example.attendance.core.data
import com.example.attendance.core.domain.WorkspaceRepository
import com.example.attendance.core.model.*
import com.example.attendance.core.database.*
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*

class RoomWorkspaceRepository(private val db: AttendanceDatabase) : WorkspaceRepository {
    private val dao = db.dao()
    override fun observe(): Flow<Workspace> = dao.changes().map { read() }.flowOn(Dispatchers.IO)
    override suspend fun read(): Workspace = db.withTransaction { load() }
    private suspend fun load() = Workspace(
        employees = dao.employees().map { it.domain() },
        accounts = dao.accounts().map { it.domain() },
        photos = dao.employee_photos().map { it.domain() },
        attempts = dao.verification_attempts().map { it.domain() },
        reviews = dao.review_requests().map { it.domain() },
        attendance = dao.attendance_records().map { it.domain() },
        notices = dao.notices().map { it.domain() },
        recipients = dao.notice_recipients().map { it.domain() },
        notifications = dao.notifications().map { it.domain() },
        holidays = dao.holidays().map { it.domain() },
        calendar = dao.work_calendar_days().map { it.domain() })
    override suspend fun <T> update(reducer: (Workspace) -> Pair<Workspace, T>): T = withContext(Dispatchers.IO) {
        db.withTransaction {
            val before = load()
            val (after, result) = reducer(before)
            // Explicit INSERT/UPDATE avoids Room @Upsert silently accepting a conflict
            // on a secondary unique key when the new primary key has no update target.
            // Update retired photo rows before inserting their replacement.
            dao.updateEmployee(after.employees.filter { row -> before.employees.any { it.id == row.id } && row !in before.employees }.map { it.entity() })
            dao.insertEmployee(after.employees.filter { row -> before.employees.none { it.id == row.id } }.map { it.entity() })
            dao.updateAccount(after.accounts.filter { row -> before.accounts.any { it.id == row.id } && row !in before.accounts }.map { it.entity() })
            dao.insertAccount(after.accounts.filter { row -> before.accounts.none { it.id == row.id } }.map { it.entity() })
            dao.updateEmployeePhoto(after.photos.filter { row -> before.photos.any { it.id == row.id } && row !in before.photos }.map { it.entity() })
            dao.insertEmployeePhoto(after.photos.filter { row -> before.photos.none { it.id == row.id } }.map { it.entity() })
            dao.updateVerificationAttempt(after.attempts.filter { row -> before.attempts.any { it.id == row.id } && row !in before.attempts }.map { it.entity() })
            dao.insertVerificationAttempt(after.attempts.filter { row -> before.attempts.none { it.id == row.id } }.map { it.entity() })
            dao.updateReviewRequest(after.reviews.filter { row -> before.reviews.any { it.id == row.id } && row !in before.reviews }.map { it.entity() })
            dao.insertReviewRequest(after.reviews.filter { row -> before.reviews.none { it.id == row.id } }.map { it.entity() })
            dao.updateAttendanceRecord(after.attendance.filter { row -> before.attendance.any { it.id == row.id } && row !in before.attendance }.map { it.entity() })
            dao.insertAttendanceRecord(after.attendance.filter { row -> before.attendance.none { it.id == row.id } }.map { it.entity() })
            dao.updateNotice(after.notices.filter { row -> before.notices.any { it.id == row.id } && row !in before.notices }.map { it.entity() })
            dao.insertNotice(after.notices.filter { row -> before.notices.none { it.id == row.id } }.map { it.entity() })
            dao.updateNoticeRecipient(after.recipients.filter { row -> before.recipients.any { it.id == row.id } && row !in before.recipients }.map { it.entity() })
            dao.insertNoticeRecipient(after.recipients.filter { row -> before.recipients.none { it.id == row.id } }.map { it.entity() })
            dao.updateInboxNotification(after.notifications.filter { row -> before.notifications.any { it.id == row.id } && row !in before.notifications }.map { it.entity() })
            dao.insertInboxNotification(after.notifications.filter { row -> before.notifications.none { it.id == row.id } }.map { it.entity() })
            dao.updateHoliday(after.holidays.filter { row -> before.holidays.any { it.id == row.id } && row !in before.holidays }.map { it.entity() })
            dao.insertHoliday(after.holidays.filter { row -> before.holidays.none { it.id == row.id } }.map { it.entity() })
            dao.updateCalendarDay(after.calendar.filter { row -> before.calendar.any { it.id == row.id } && row !in before.calendar }.map { it.entity() })
            dao.insertCalendarDay(after.calendar.filter { row -> before.calendar.none { it.id == row.id } }.map { it.entity() })
            result
        }
    }
}
