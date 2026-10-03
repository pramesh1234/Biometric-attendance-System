package com.example.attendance.core.domain
import com.example.attendance.core.model.*
import java.time.*
import java.util.UUID

class ImportReferencePhotoUseCase(private val store: PhotoStore, private val verifier: FaceVerifier) {
    suspend operator fun invoke(uri: String): String {
        val key = store.importImage(uri)
        try { verifier.validateReference(key); return key } catch (e: Exception) { store.delete(key); throw e }
    }
}
class SaveStaffUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val passwords: PasswordHasher, private val clock: Clock) {
    suspend operator fun invoke(draft: StaffDraft): String {
        requireRule(draft.name.trim().isNotEmpty() && draft.code.trim().isNotEmpty(), "Name and employee ID are required.")
        requireRule(draft.endDate == null || !draft.endDate!!.isBefore(draft.joiningDate), "End date cannot be before joining date.")
        requireRule(draft.joiningDate.year in 1970..(LocalDate.now(clock).year + 1), "Joining date must be between 1970 and next year.")
        val username = normalizeUsername(draft.username); requireRule(username.length in 3..100, "Username must be 3–100 characters.")
        if (draft.id == null) { validPassword(draft.temporaryPassword); requireRule(draft.newPhotoKey != null, "Add a clear reference photo before creating staff.") }
        val hash = if (draft.temporaryPassword.isNotBlank()) { validPassword(draft.temporaryPassword); passwords.hash(draft.temporaryPassword) } else null
        val now = clock.instant()
        return repo.update { w ->
            val actor = w.authorize(sessions.session.value, Role.ADMIN)
            val existing = draft.id?.let { id -> w.employees.firstOrNull { it.id == id && it.deletedAt == null } ?: throw DomainException("Employee no longer available.") }
            requireRule(w.employees.none { it.id != draft.id && it.code.equals(draft.code.trim(), true) }, "That employee ID is already in use.")
            val oldAccount = w.accounts.firstOrNull { it.employeeId == draft.id && draft.id != null }
            requireRule(w.accounts.none { it.id != oldAccount?.id && it.username == username }, "That username is already in use.")
            val employee = Employee(existing?.id ?: UUID.randomUUID().toString(), draft.code.trim(), draft.name.trim(), draft.email.trim(), draft.phone.trim(), draft.jobTitle.trim(), draft.gender, draft.joiningDate, draft.endDate, existing?.createdAt ?: now, now)
            val account = if (oldAccount == null) Account(UUID.randomUUID().toString(), employee.id, username, hash!!, Role.STAFF, AccountStatus.ACTIVE, true, now, now, now) else oldAccount.copy(username = username, passwordHash = hash ?: oldAccount.passwordHash, mustChangePassword = if (hash != null) true else oldAccount.mustChangePassword, passwordChangedAt = if (hash != null) now else oldAccount.passwordChangedAt, updatedAt = now)
            val photos = if (draft.newPhotoKey == null) w.photos else w.photos.map { if (it.employeeId == employee.id && it.retiredAt == null) it.copy(retiredAt = now) else it } + EmployeePhoto(UUID.randomUUID().toString(), employee.id, draft.newPhotoKey!!, actor.id, now)
            val calendar = w.calendar + calendarRange(employee.joiningDate.year, LocalDate.now(clock).year + 1, now).filter { day -> w.calendar.none { it.date == day.date } }
            w.copy(employees = w.employees.filterNot { it.id == employee.id } + employee, accounts = w.accounts.filterNot { it.id == account.id } + account, photos = photos, calendar = calendar) to employee.id
        }
    }
}
class SetStaffAccessUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(employeeId: String, blocked: Boolean, delete: Boolean = false) {
        repo.update { w ->
            w.authorize(sessions.session.value, Role.ADMIN)
            requireRule(w.employees.any { it.id == employeeId && it.deletedAt == null }, "Employee no longer available.")
            val now = clock.instant()
            w.copy(employees = w.employees.map { if (it.id == employeeId && delete) it.copy(deletedAt = now, updatedAt = now) else it }, accounts = w.accounts.map { if (it.employeeId == employeeId) it.copy(status = if (delete) AccountStatus.DISABLED else if (blocked) AccountStatus.BLOCKED else AccountStatus.ACTIVE, updatedAt = now) else it }) to Unit
        }
    }
}
class ResetStaffPasswordUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore, private val passwords: PasswordHasher, private val clock: Clock) {
    suspend operator fun invoke(employeeId: String, password: String) {
        validPassword(password); val hash = passwords.hash(password)
        repo.update { w -> w.authorize(sessions.session.value, Role.ADMIN)
            requireRule(w.employees.any { it.id == employeeId && it.deletedAt == null }, "Employee no longer available.")
            w.copy(accounts = w.accounts.map { if (it.employeeId == employeeId) it.copy(passwordHash = hash, mustChangePassword = true, passwordChangedAt = clock.instant(), updatedAt = clock.instant()) else it }) to Unit
        }
    }
}
