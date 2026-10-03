package com.example.attendance.core.domain

import com.example.attendance.core.model.Account
import com.example.attendance.core.model.AccountStatus
import com.example.attendance.core.model.Employee
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.OfficeLocation
import com.example.attendance.core.model.ReviewDetail
import com.example.attendance.core.model.ReviewRequest
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.Session
import com.example.attendance.core.model.StaffProfile
import com.example.attendance.core.model.Workspace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

/** A transactional unit of work. The reducer contains domain rules; implementations only load/map/persist. */
interface WorkspaceRepository {
    fun observe(): Flow<Workspace>
    suspend fun read(): Workspace
    suspend fun <T> update(reducer: (Workspace) -> Pair<Workspace, T>): T
}

interface SessionStore {
    val session: StateFlow<Session?>;
    fun set(session: Session?)
}

interface PasswordHasher {
    suspend fun hash(password: String): String;
    suspend fun matches(password: String, encoded: String): Boolean
}

interface PhotoStore {
    suspend fun importImage(uri: String): String;
    fun path(key: String): String;
    suspend fun delete(key: String)
}

data class FaceScore(val basisPoints: Int, val version: String)
interface FaceVerifier {
    suspend fun validateReference(key: String);
    suspend fun compare(referenceKey: String, capturedKey: String): FaceScore
}

interface LocationProvider {
    suspend fun current(): GeoPoint;
    fun updates(): Flow<GeoPoint>
}

interface OfficeSettings {
    val zone: StateFlow<ZoneId>;
    val officeLocation: StateFlow<OfficeLocation?>;
    fun setZone(zone: ZoneId);
    fun setOfficeLocation(location: OfficeLocation)
}

open class DomainException(message: String) : IllegalArgumentException(message)

internal fun requireRule(condition: Boolean, message: String) {
    if (!condition) throw DomainException(message)
}

internal fun Workspace.authorize(session: Session?, role: Role? = null): Account {
    val actor = accounts.firstOrNull { it.id == session?.accountId }
        ?: throw DomainException("Please sign in again.")
    requireRule(
        actor.status == AccountStatus.ACTIVE && actor.passwordChangedAt == session?.passwordVersion,
        "Your account access changed. Please sign in again."
    )
    requireRule(role == null || actor.role == role, "You don’t have permission for this action.")
    requireRule(!actor.mustChangePassword, "Please change your temporary password first.")
    if (actor.role == Role.STAFF) requireRule(
        employees.any { it.id == actor.employeeId && it.deletedAt == null },
        "This employee account is no longer available."
    )
    return actor
}

internal fun Account.toSession() = Session(
    id,
    role,
    employeeId,
    passwordChangedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS),
    mustChangePassword
)

internal fun Workspace.profile(employee: Employee) = StaffProfile(
    employee,
    accounts.first { it.employeeId == employee.id }.username,
    accounts.first { it.employeeId == employee.id }.status,
    photos.firstOrNull { it.employeeId == employee.id && it.retiredAt == null }?.fileKey
)

internal fun Workspace.detail(review: ReviewRequest): ReviewDetail {
    val attempt = attempts.first { it.id == review.attemptId }
    return ReviewDetail(
        review,
        attempt,
        employees.first { it.id == attempt.employeeId },
        photos.first { it.id == attempt.referencePhotoId }.fileKey
    )
}

internal fun normalizeUsername(s: String) = s.trim().lowercase(java.util.Locale.ROOT)
internal fun validPassword(s: String) {
    requireRule(s.length in 10..256, "Use a password with 10–256 characters.")
}
