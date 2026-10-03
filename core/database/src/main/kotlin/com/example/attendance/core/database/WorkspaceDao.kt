package com.example.attendance.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM employees")
    suspend fun employees(): List<EmployeeEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmployee(rows: List<EmployeeEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateEmployee(rows: List<EmployeeEntity>)
    @Query("SELECT * FROM accounts")
    suspend fun accounts(): List<AccountEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(rows: List<AccountEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateAccount(rows: List<AccountEntity>)
    @Query("SELECT * FROM employee_photos")
    suspend fun employee_photos(): List<EmployeePhotoEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmployeePhoto(rows: List<EmployeePhotoEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateEmployeePhoto(rows: List<EmployeePhotoEntity>)
    @Query("SELECT * FROM verification_attempts")
    suspend fun verification_attempts(): List<VerificationAttemptEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVerificationAttempt(rows: List<VerificationAttemptEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateVerificationAttempt(rows: List<VerificationAttemptEntity>)
    @Query("SELECT * FROM review_requests")
    suspend fun review_requests(): List<ReviewRequestEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReviewRequest(rows: List<ReviewRequestEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateReviewRequest(rows: List<ReviewRequestEntity>)
    @Query("SELECT * FROM attendance_records")
    suspend fun attendance_records(): List<AttendanceRecordEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttendanceRecord(rows: List<AttendanceRecordEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateAttendanceRecord(rows: List<AttendanceRecordEntity>)
    @Query("SELECT * FROM notices")
    suspend fun notices(): List<NoticeEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNotice(rows: List<NoticeEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateNotice(rows: List<NoticeEntity>)
    @Query("SELECT * FROM notice_recipients")
    suspend fun notice_recipients(): List<NoticeRecipientEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertNoticeRecipient(rows: List<NoticeRecipientEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateNoticeRecipient(rows: List<NoticeRecipientEntity>)
    @Query("SELECT * FROM notifications")
    suspend fun notifications(): List<InboxNotificationEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInboxNotification(rows: List<InboxNotificationEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateInboxNotification(rows: List<InboxNotificationEntity>)
    @Query("SELECT * FROM holidays")
    suspend fun holidays(): List<HolidayEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertHoliday(rows: List<HolidayEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateHoliday(rows: List<HolidayEntity>)
    @Query("SELECT * FROM work_calendar_days")
    suspend fun work_calendar_days(): List<CalendarDayEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCalendarDay(rows: List<CalendarDayEntity>)
    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun updateCalendarDay(rows: List<CalendarDayEntity>)
    @Query("SELECT (SELECT COUNT(*) FROM employees) + (SELECT COUNT(*) FROM accounts) + (SELECT COUNT(*) FROM employee_photos) + (SELECT COUNT(*) FROM verification_attempts) + (SELECT COUNT(*) FROM review_requests) + (SELECT COUNT(*) FROM attendance_records) + (SELECT COUNT(*) FROM notices) + (SELECT COUNT(*) FROM notice_recipients) + (SELECT COUNT(*) FROM notifications) + (SELECT COUNT(*) FROM holidays) + (SELECT COUNT(*) FROM work_calendar_days)")
    fun changes(): Flow<Long>
}
