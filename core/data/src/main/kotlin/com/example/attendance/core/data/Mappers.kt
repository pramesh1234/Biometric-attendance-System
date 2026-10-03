package com.example.attendance.core.data

import com.example.attendance.core.database.AccountEntity
import com.example.attendance.core.database.AttendanceRecordEntity
import com.example.attendance.core.database.CalendarDayEntity
import com.example.attendance.core.database.EmployeeEntity
import com.example.attendance.core.database.EmployeePhotoEntity
import com.example.attendance.core.database.HolidayEntity
import com.example.attendance.core.database.InboxNotificationEntity
import com.example.attendance.core.database.NoticeEntity
import com.example.attendance.core.database.NoticeRecipientEntity
import com.example.attendance.core.database.ReviewRequestEntity
import com.example.attendance.core.database.VerificationAttemptEntity
import com.example.attendance.core.model.Account
import com.example.attendance.core.model.AccountStatus
import com.example.attendance.core.model.AttendanceRecord
import com.example.attendance.core.model.CalendarDay
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.Employee
import com.example.attendance.core.model.EmployeePhoto
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.Holiday
import com.example.attendance.core.model.InboxNotification
import com.example.attendance.core.model.Notice
import com.example.attendance.core.model.NoticeRecipient
import com.example.attendance.core.model.NotificationType
import com.example.attendance.core.model.Outcome
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.ReviewRequest
import com.example.attendance.core.model.ReviewStatus
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.VerificationAttempt
import java.time.Instant
import java.time.LocalDate

internal fun EmployeeEntity.domain() = Employee(
    id = id,
    code = code,
    name = name,
    email = email,
    phone = phone,
    jobTitle = jobTitle,
    gender = gender,
    joiningDate = joiningDate.let { LocalDate.parse(it) },
    endDate = endDate?.let { LocalDate.parse(it) },
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    updatedAt = updatedAt.let { Instant.ofEpochMilli(it) },
    deletedAt = deletedAt?.let { Instant.ofEpochMilli(it) })

internal fun Employee.entity() = EmployeeEntity(
    id = id,
    code = code,
    name = name,
    email = email,
    phone = phone,
    jobTitle = jobTitle,
    gender = gender,
    joiningDate = joiningDate.toString(),
    endDate = endDate?.toString(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    deletedAt = deletedAt?.toEpochMilli()
)

internal fun AccountEntity.domain() = Account(
    id = id,
    employeeId = employeeId,
    username = username,
    passwordHash = passwordHash,
    role = Role.valueOf(role),
    status = AccountStatus.valueOf(status),
    mustChangePassword = mustChangePassword,
    passwordChangedAt = passwordChangedAt.let { Instant.ofEpochMilli(it) },
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    updatedAt = updatedAt.let { Instant.ofEpochMilli(it) })

internal fun Account.entity() = AccountEntity(
    id = id,
    employeeId = employeeId,
    username = username,
    passwordHash = passwordHash,
    role = role.name,
    status = status.name,
    mustChangePassword = mustChangePassword,
    passwordChangedAt = passwordChangedAt.toEpochMilli(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

internal fun EmployeePhotoEntity.domain() = EmployeePhoto(
    id = id,
    employeeId = employeeId,
    fileKey = fileKey,
    createdBy = createdBy,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    retiredAt = retiredAt?.let { Instant.ofEpochMilli(it) })

internal fun EmployeePhoto.entity() = EmployeePhotoEntity(
    id = id,
    employeeId = employeeId,
    fileKey = fileKey,
    createdBy = createdBy,
    createdAt = createdAt.toEpochMilli(),
    retiredAt = retiredAt?.toEpochMilli()
)

internal fun VerificationAttemptEntity.domain() = VerificationAttempt(
    id = id,
    employeeId = employeeId,
    referencePhotoId = referencePhotoId,
    action = PunchAction.valueOf(action),
    capturedAt = capturedAt.let { Instant.ofEpochMilli(it) },
    attendanceDate = attendanceDate.let { LocalDate.parse(it) },
    timeZoneId = timeZoneId,
    imageKey = imageKey,
    location = GeoPoint(
        latitude,
        longitude,
        accuracyM,
        locationLabel,
        Instant.ofEpochMilli(measuredAt)
    ),
    confidenceBps = confidenceBps,
    outcome = Outcome.valueOf(outcome),
    verifierVersion = verifierVersion,
    policyVersion = policyVersion,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) })

internal fun VerificationAttempt.entity() = VerificationAttemptEntity(
    id = id,
    employeeId = employeeId,
    referencePhotoId = referencePhotoId,
    action = action.name,
    capturedAt = capturedAt.toEpochMilli(),
    attendanceDate = attendanceDate.toString(),
    timeZoneId = timeZoneId,
    imageKey = imageKey,
    latitude = location.latitude,
    longitude = location.longitude,
    accuracyM = location.accuracyM,
    locationLabel = location.label,
    measuredAt = location.measuredAt.toEpochMilli(),
    confidenceBps = confidenceBps,
    outcome = outcome.name,
    verifierVersion = verifierVersion,
    policyVersion = policyVersion,
    createdAt = createdAt.toEpochMilli()
)

internal fun ReviewRequestEntity.domain() = ReviewRequest(
    id = id,
    attemptId = attemptId,
    status = ReviewStatus.valueOf(status),
    submittedAt = submittedAt.let { Instant.ofEpochMilli(it) },
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt?.let { Instant.ofEpochMilli(it) },
    note = note
)

internal fun ReviewRequest.entity() = ReviewRequestEntity(
    id = id,
    attemptId = attemptId,
    status = status.name,
    submittedAt = submittedAt.toEpochMilli(),
    reviewedBy = reviewedBy,
    reviewedAt = reviewedAt?.toEpochMilli(),
    note = note
)

internal fun AttendanceRecordEntity.domain() = AttendanceRecord(
    id = id,
    employeeId = employeeId,
    date = date.let { LocalDate.parse(it) },
    timeZoneId = timeZoneId,
    checkInAttemptId = checkInAttemptId,
    checkOutAttemptId = checkOutAttemptId,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    updatedAt = updatedAt.let { Instant.ofEpochMilli(it) })

internal fun AttendanceRecord.entity() = AttendanceRecordEntity(
    id = id,
    employeeId = employeeId,
    date = date.toString(),
    timeZoneId = timeZoneId,
    checkInAttemptId = checkInAttemptId,
    checkOutAttemptId = checkOutAttemptId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

internal fun NoticeEntity.domain() = Notice(
    id = id,
    senderId = senderId,
    title = title,
    body = body,
    sentAt = sentAt.let { Instant.ofEpochMilli(it) })

internal fun Notice.entity() = NoticeEntity(
    id = id,
    senderId = senderId,
    title = title,
    body = body,
    sentAt = sentAt.toEpochMilli()
)

internal fun NoticeRecipientEntity.domain() =
    NoticeRecipient(id = id, noticeId = noticeId, employeeId = employeeId)

internal fun NoticeRecipient.entity() =
    NoticeRecipientEntity(id = id, noticeId = noticeId, employeeId = employeeId)

internal fun InboxNotificationEntity.domain() = InboxNotification(
    id = id,
    employeeId = employeeId,
    type = NotificationType.valueOf(type),
    noticeRecipientId = noticeRecipientId,
    reviewRequestId = reviewRequestId,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    readAt = readAt?.let { Instant.ofEpochMilli(it) })

internal fun InboxNotification.entity() = InboxNotificationEntity(
    id = id,
    employeeId = employeeId,
    type = type.name,
    noticeRecipientId = noticeRecipientId,
    reviewRequestId = reviewRequestId,
    createdAt = createdAt.toEpochMilli(),
    readAt = readAt?.toEpochMilli()
)

internal fun HolidayEntity.domain() = Holiday(
    id = id,
    name = name,
    date = date.let { LocalDate.parse(it) },
    description = description,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    updatedAt = updatedAt.let { Instant.ofEpochMilli(it) })

internal fun Holiday.entity() = HolidayEntity(
    id = id,
    name = name,
    date = date.toString(),
    description = description,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

internal fun CalendarDayEntity.domain() = CalendarDay(
    id = id,
    date = date.let { LocalDate.parse(it) },
    type = DayType.valueOf(type),
    holidayId = holidayId,
    createdAt = createdAt.let { Instant.ofEpochMilli(it) },
    updatedAt = updatedAt.let { Instant.ofEpochMilli(it) })

internal fun CalendarDay.entity() = CalendarDayEntity(
    id = id,
    date = date.toString(),
    type = type.name,
    holidayId = holidayId,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

