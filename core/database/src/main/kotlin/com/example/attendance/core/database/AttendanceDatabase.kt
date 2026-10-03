package com.example.attendance.core.database
import androidx.room.*
@Database(entities = [EmployeeEntity::class, AccountEntity::class, EmployeePhotoEntity::class, VerificationAttemptEntity::class, ReviewRequestEntity::class, AttendanceRecordEntity::class, NoticeEntity::class, NoticeRecipientEntity::class, InboxNotificationEntity::class, HolidayEntity::class, CalendarDayEntity::class], version = 1, exportSchema = true)
abstract class AttendanceDatabase : RoomDatabase() { abstract fun dao(): WorkspaceDao }

fun createAttendanceDatabase(context: android.content.Context, name: String = "attendance.db"): AttendanceDatabase = Room.databaseBuilder(context, AttendanceDatabase::class.java, name)
    .addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("CREATE UNIQUE INDEX active_reference_photo ON employee_photos(employeeId) WHERE retiredAt IS NULL")
            val checks = mapOf(
                "employees" to "NEW.endDate IS NOT NULL AND NEW.endDate < NEW.joiningDate",
                "accounts" to "NEW.role NOT IN ('ADMIN','STAFF') OR NEW.status NOT IN ('ACTIVE','BLOCKED','DISABLED') OR (NEW.role = 'STAFF' AND NEW.employeeId IS NULL)",
                "verification_attempts" to "NEW.confidenceBps < 0 OR NEW.confidenceBps > 10000 OR NEW.action NOT IN ('CHECK_IN','CHECK_OUT') OR NEW.outcome NOT IN ('VERIFIED','REVIEW_REQUIRED','FAILED') OR NOT EXISTS (SELECT 1 FROM employee_photos WHERE id = NEW.referencePhotoId AND employeeId = NEW.employeeId)",
                "review_requests" to "NOT EXISTS (SELECT 1 FROM verification_attempts WHERE id = NEW.attemptId AND outcome = 'REVIEW_REQUIRED') OR NEW.status NOT IN ('PENDING','APPROVED','REJECTED') OR (NEW.status = 'PENDING' AND (NEW.reviewedBy IS NOT NULL OR NEW.reviewedAt IS NOT NULL)) OR (NEW.status <> 'PENDING' AND (NEW.reviewedBy IS NULL OR NEW.reviewedAt IS NULL))",
                "notifications" to "NEW.type NOT IN ('NOTICE','REVIEW_SUBMITTED','REVIEW_APPROVED','REVIEW_REJECTED') OR (NEW.type = 'NOTICE' AND (NEW.noticeRecipientId IS NULL OR NEW.reviewRequestId IS NOT NULL)) OR (NEW.type <> 'NOTICE' AND (NEW.reviewRequestId IS NULL OR NEW.noticeRecipientId IS NOT NULL))",
                "work_calendar_days" to "NEW.type NOT IN ('WORKING','WEEKLY_OFF','HOLIDAY') OR (NEW.type = 'HOLIDAY' AND NEW.holidayId IS NULL) OR (NEW.type <> 'HOLIDAY' AND NEW.holidayId IS NOT NULL)"
            )
            checks.forEach { (table, condition) -> listOf("INSERT", "UPDATE").forEach { op -> db.execSQL("CREATE TRIGGER validate_${table}_${op.lowercase()} BEFORE $op ON $table WHEN $condition BEGIN SELECT RAISE(ABORT, 'Invalid $table record'); END") } }
        }
    }).build()
