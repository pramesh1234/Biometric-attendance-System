package com.example.attendance.core.domain
import com.example.attendance.core.model.*
import java.time.*
import java.util.UUID
import kotlinx.coroutines.flow.*

class ObserveSessionUseCase(private val sessions: SessionStore) { operator fun invoke() = sessions.session }
class SignOutUseCase(private val sessions: SessionStore) { operator fun invoke() = sessions.set(null) }
class ObserveSetupUseCase(private val repo: WorkspaceRepository) { operator fun invoke() = repo.observe().map { w -> w.accounts.none { it.role == Role.ADMIN } }.distinctUntilChanged() }
class SetupAdminUseCase(private val repo: WorkspaceRepository, private val passwords: PasswordHasher, private val sessions: SessionStore, private val clock: Clock, private val settings: OfficeSettings) {
    suspend operator fun invoke(username: String, password: String, zone: String) {
        val name = normalizeUsername(username); requireRule(name.length in 3..100, "Enter a username with 3–100 characters."); validPassword(password)
        val timeZone = runCatching { ZoneId.of(zone.trim()) }.getOrElse { throw DomainException("Enter a valid time zone, such as Asia/Kolkata.") }
        val hash = passwords.hash(password); val now = clock.instant()
        val account = Account(UUID.randomUUID().toString(), null, name, hash, Role.ADMIN, AccountStatus.ACTIVE, false, now, now, now)
        repo.update { w -> requireRule(w.accounts.none { it.role == Role.ADMIN }, "An admin already exists."); w.copy(accounts = w.accounts + account, calendar = calendarRange(LocalDate.now(clock.withZone(timeZone)).year, LocalDate.now(clock.withZone(timeZone)).year + 1, now)) to Unit }
        settings.setZone(timeZone); sessions.set(account.toSession())
    }
}
class LoginUseCase(private val repo: WorkspaceRepository, private val passwords: PasswordHasher, private val sessions: SessionStore) {
    suspend operator fun invoke(username: String, password: String, role: Role) {
        val account = repo.read().accounts.firstOrNull { it.username == normalizeUsername(username) && it.role == role }
        requireRule(account != null && passwords.matches(password, account.passwordHash), "Username or password is incorrect.")
        val current = repo.read().accounts.firstOrNull { it.id == account!!.id } ?: throw DomainException("Account unavailable.")
        requireRule(current.passwordHash == account!!.passwordHash && current.status == AccountStatus.ACTIVE, "This account is blocked or disabled. Contact your admin.")
        if (current.role == Role.STAFF) requireRule(repo.read().employees.any { it.id == current.employeeId && it.deletedAt == null }, "This employee is no longer active.")
        sessions.set(current.toSession())
    }
}
class ChangePasswordUseCase(private val repo: WorkspaceRepository, private val passwords: PasswordHasher, private val sessions: SessionStore, private val clock: Clock) {
    suspend operator fun invoke(old: String, new: String) {
        validPassword(new); requireRule(old != new, "Choose a different password.")
        val session = sessions.session.value ?: throw DomainException("Please sign in again.")
        val account = repo.read().accounts.first { it.id == session.accountId }
        requireRule(passwords.matches(old, account.passwordHash), "Current password is incorrect.")
        val hash = passwords.hash(new)
        val updated = repo.update { w ->
            val a = w.accounts.first { it.id == session.accountId }
            requireRule(a.status == AccountStatus.ACTIVE && a.passwordChangedAt == session.passwordVersion, "Please sign in again.")
            val next = a.copy(passwordHash = hash, mustChangePassword = false, passwordChangedAt = clock.instant(), updatedAt = clock.instant())
            w.copy(accounts = w.accounts.map { if (it.id == a.id) next else it }) to next
        }
        sessions.set(updated.toSession())
    }
}
class ValidateSessionUseCase(private val repo: WorkspaceRepository, private val sessions: SessionStore) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Unit> = sessions.session.flatMapLatest { s ->
        if (s == null) flowOf(Unit) else repo.observe().map { w ->
            if (sessions.session.value == s && w.accounts.none { it.id == s.accountId && it.status == AccountStatus.ACTIVE && it.passwordChangedAt == s.passwordVersion }) sessions.set(null)
            Unit
        }
    }
}
internal fun calendarRange(from: Int, to: Int, now: Instant): List<CalendarDay> = generateSequence(LocalDate.of(from, 1, 1)) { it.plusDays(1) }.takeWhile { it < LocalDate.of(to + 1, 1, 1) }.toList().map { d -> CalendarDay(d.toString(), d, if (d.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) DayType.WEEKLY_OFF else DayType.WORKING, createdAt = now, updatedAt = now) }
