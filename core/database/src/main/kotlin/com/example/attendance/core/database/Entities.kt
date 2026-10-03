package com.example.attendance.core.database
import androidx.room.*

@Entity(tableName = "employees", indices = [Index(value = ["code"], unique = true)], foreignKeys = [])
data class EmployeeEntity(@PrimaryKey val id: String, val code: String, val name: String, val email: String, val phone: String, val jobTitle: String, val gender: String, val joiningDate: String, val endDate: String?, val createdAt: Long, val updatedAt: Long, val deletedAt: Long?)

@Entity(tableName = "accounts", indices = [Index(value = ["employeeId"], unique = true), Index(value = ["username"], unique = true)], foreignKeys = [ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT)])
data class AccountEntity(@PrimaryKey val id: String, val employeeId: String?, val username: String, val passwordHash: String, val role: String, val status: String, val mustChangePassword: Boolean, val passwordChangedAt: Long, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "employee_photos", indices = [Index("employeeId"), Index("createdBy")], foreignKeys = [ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["createdBy"], onDelete = ForeignKey.RESTRICT)])
data class EmployeePhotoEntity(@PrimaryKey val id: String, val employeeId: String, val fileKey: String, val createdBy: String, val createdAt: Long, val retiredAt: Long?)

@Entity(tableName = "verification_attempts", indices = [Index("employeeId"), Index("referencePhotoId")], foreignKeys = [ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = EmployeePhotoEntity::class, parentColumns = ["id"], childColumns = ["referencePhotoId"], onDelete = ForeignKey.RESTRICT)])
data class VerificationAttemptEntity(@PrimaryKey val id: String, val employeeId: String, val referencePhotoId: String, val action: String, val capturedAt: Long, val attendanceDate: String, val timeZoneId: String, val imageKey: String, val latitude: Double, val longitude: Double, val accuracyM: Float, val locationLabel: String, val measuredAt: Long, val confidenceBps: Int, val outcome: String, val verifierVersion: String, val policyVersion: String, val createdAt: Long)

@Entity(tableName = "review_requests", indices = [Index(value = ["attemptId"], unique = true), Index("reviewedBy")], foreignKeys = [ForeignKey(entity = VerificationAttemptEntity::class, parentColumns = ["id"], childColumns = ["attemptId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["reviewedBy"], onDelete = ForeignKey.RESTRICT)])
data class ReviewRequestEntity(@PrimaryKey val id: String, val attemptId: String, val status: String, val submittedAt: Long, val reviewedBy: String?, val reviewedAt: Long?, val note: String)

@Entity(tableName = "attendance_records", indices = [Index(value = ["employeeId", "date"], unique = true), Index(value = ["checkInAttemptId"], unique = true), Index(value = ["checkOutAttemptId"], unique = true), Index("employeeId")], foreignKeys = [ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = VerificationAttemptEntity::class, parentColumns = ["id"], childColumns = ["checkInAttemptId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = VerificationAttemptEntity::class, parentColumns = ["id"], childColumns = ["checkOutAttemptId"], onDelete = ForeignKey.RESTRICT)])
data class AttendanceRecordEntity(@PrimaryKey val id: String, val employeeId: String, val date: String, val timeZoneId: String, val checkInAttemptId: String, val checkOutAttemptId: String?, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "notices", indices = [Index("senderId")], foreignKeys = [ForeignKey(entity = AccountEntity::class, parentColumns = ["id"], childColumns = ["senderId"], onDelete = ForeignKey.RESTRICT)])
data class NoticeEntity(@PrimaryKey val id: String, val senderId: String, val title: String, val body: String, val sentAt: Long)

@Entity(tableName = "notice_recipients", indices = [Index(value = ["noticeId", "employeeId"], unique = true), Index("noticeId"), Index("employeeId")], foreignKeys = [ForeignKey(entity = NoticeEntity::class, parentColumns = ["id"], childColumns = ["noticeId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT)])
data class NoticeRecipientEntity(@PrimaryKey val id: String, val noticeId: String, val employeeId: String)

@Entity(tableName = "notifications", indices = [Index(value = ["type", "noticeRecipientId"], unique = true), Index(value = ["type", "reviewRequestId"], unique = true), Index("employeeId"), Index("noticeRecipientId"), Index("reviewRequestId")], foreignKeys = [ForeignKey(entity = EmployeeEntity::class, parentColumns = ["id"], childColumns = ["employeeId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = NoticeRecipientEntity::class, parentColumns = ["id"], childColumns = ["noticeRecipientId"], onDelete = ForeignKey.RESTRICT), ForeignKey(entity = ReviewRequestEntity::class, parentColumns = ["id"], childColumns = ["reviewRequestId"], onDelete = ForeignKey.RESTRICT)])
data class InboxNotificationEntity(@PrimaryKey val id: String, val employeeId: String, val type: String, val noticeRecipientId: String?, val reviewRequestId: String?, val createdAt: Long, val readAt: Long?)

@Entity(tableName = "holidays", indices = [], foreignKeys = [])
data class HolidayEntity(@PrimaryKey val id: String, val name: String, val date: String, val description: String, val createdAt: Long, val updatedAt: Long)

@Entity(tableName = "work_calendar_days", indices = [Index(value = ["date"], unique = true), Index("holidayId")], foreignKeys = [ForeignKey(entity = HolidayEntity::class, parentColumns = ["id"], childColumns = ["holidayId"], onDelete = ForeignKey.RESTRICT)])
data class CalendarDayEntity(@PrimaryKey val id: String, val date: String, val type: String, val holidayId: String?, val createdAt: Long, val updatedAt: Long)

