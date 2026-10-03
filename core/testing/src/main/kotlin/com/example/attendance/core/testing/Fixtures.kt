package com.example.attendance.core.testing

import com.example.attendance.core.domain.CalculateAttendanceUseCase
import com.example.attendance.core.domain.CaptureAttendanceUseCase
import com.example.attendance.core.domain.ChangePasswordUseCase
import com.example.attendance.core.domain.DecideReviewUseCase
import com.example.attendance.core.domain.DomainException
import com.example.attendance.core.domain.EnsureCalendarUseCase
import com.example.attendance.core.domain.FaceScore
import com.example.attendance.core.domain.FaceVerifier
import com.example.attendance.core.domain.ImportReferencePhotoUseCase
import com.example.attendance.core.domain.LocationProvider
import com.example.attendance.core.domain.LoginUseCase
import com.example.attendance.core.domain.MarkNotificationReadUseCase
import com.example.attendance.core.domain.ObserveAdminUseCase
import com.example.attendance.core.domain.ObserveAttendanceUseCase
import com.example.attendance.core.domain.ObserveGeofenceUseCase
import com.example.attendance.core.domain.ObserveHolidaysUseCase
import com.example.attendance.core.domain.ObserveInboxUseCase
import com.example.attendance.core.domain.ObserveStaffUseCase
import com.example.attendance.core.domain.OfficeSettings
import com.example.attendance.core.domain.PasswordHasher
import com.example.attendance.core.domain.PhotoStore
import com.example.attendance.core.domain.ResetStaffPasswordUseCase
import com.example.attendance.core.domain.SaveCalendarDayUseCase
import com.example.attendance.core.domain.SaveStaffUseCase
import com.example.attendance.core.domain.SendNoticeUseCase
import com.example.attendance.core.domain.SessionStore
import com.example.attendance.core.domain.SetOfficeLocationUseCase
import com.example.attendance.core.domain.SetOfficeZoneUseCase
import com.example.attendance.core.domain.SetStaffAccessUseCase
import com.example.attendance.core.domain.SetupAdminUseCase
import com.example.attendance.core.domain.SubmitReviewUseCase
import com.example.attendance.core.domain.VerificationPolicy
import com.example.attendance.core.domain.WorkspaceRepository
import com.example.attendance.core.model.Account
import com.example.attendance.core.model.AccountStatus
import com.example.attendance.core.model.CalendarDay
import com.example.attendance.core.model.DayType
import com.example.attendance.core.model.Employee
import com.example.attendance.core.model.EmployeePhoto
import com.example.attendance.core.model.GeoPoint
import com.example.attendance.core.model.OfficeLocation
import com.example.attendance.core.model.PunchAction
import com.example.attendance.core.model.Role
import com.example.attendance.core.model.Session
import com.example.attendance.core.model.StaffDraft
import com.example.attendance.core.model.VerificationAttempt
import com.example.attendance.core.model.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class MutableClock(
    var now: Instant = Instant.parse("2026-10-05T12:00:00Z"),
    private val zone: ZoneId = ZoneOffset.UTC
) : Clock() {
    override fun instant() = now
    override fun getZone() = zone
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    fun advanceSeconds(seconds: Long = 1) {
        now = now.plusSeconds(seconds)
    }
}

class MemoryRepository(initial: Workspace = Workspace()) : WorkspaceRepository {
    val state = MutableStateFlow(initial)
    private val mutex = Mutex()
    override fun observe() = state.asStateFlow()
    override suspend fun read() = state.value
    override suspend fun <T> update(reducer: (Workspace) -> Pair<Workspace, T>): T =
        mutex.withLock {
            val (next, result) = reducer(state.value); state.value = next; result
        }
}

class TestSessions : SessionStore {
    private val state = MutableStateFlow<Session?>(null)
    override val session = state.asStateFlow()
    override fun set(session: Session?) {
        state.value = session
    }
}

class TestSettings : OfficeSettings {
    private val state = MutableStateFlow<ZoneId>(ZoneOffset.UTC)
    private val office = MutableStateFlow<OfficeLocation?>(OfficeLocation(12.9, 77.5))
    override val zone = state.asStateFlow()
    override val officeLocation = office.asStateFlow()
    override fun setZone(zone: ZoneId) {
        state.value = zone
    }

    override fun setOfficeLocation(location: OfficeLocation) {
        office.value = location
    }
}

class TestPasswords : PasswordHasher {
    var beforeMatch: (suspend () -> Unit)? = null
    override suspend fun hash(password: String) = "test-only:$password"
    override suspend fun matches(password: String, encoded: String): Boolean {
        beforeMatch?.invoke(); return encoded == "test-only:$password"
    }
}

class TestPhotos : PhotoStore {
    val imported = mutableListOf<String>();
    val deleted = mutableListOf<String>()
    override suspend fun importImage(uri: String): String =
        "image-${imported.size}".also { imported += it }

    override fun path(key: String) = "/test/$key"
    override suspend fun delete(key: String) {
        deleted += key
    }
}

class TestFaces : FaceVerifier {
    var score = 8000;
    var invalidReference = false;
    var failure: Exception? = null
    var beforeCompare: (suspend () -> Unit)? = null
    override suspend fun validateReference(key: String) {
        if (invalidReference) throw DomainException("Invalid reference")
    }

    override suspend fun compare(referenceKey: String, capturedKey: String): FaceScore {
        beforeCompare?.invoke(); failure?.let { throw it }; return FaceScore(score, "test-only")
    }
}

class TestLocation(private val clock: Clock) : LocationProvider {
    var failure: Exception? = null
    var point = GeoPoint(12.9, 77.5, 5f, "Test office", clock.instant())
    val live = MutableStateFlow<GeoPoint?>(null)
    override suspend fun current(): GeoPoint {
        failure?.let { throw it }; return (live.value ?: point).copy(measuredAt = clock.instant())
    }

    override fun updates(): Flow<GeoPoint> =
        live.map { it ?: point.copy(measuredAt = clock.instant()) }
}

class Fixture(
    val repo: WorkspaceRepository = MemoryRepository(seed()),
    val clock: MutableClock = MutableClock()
) {
    val sessions = TestSessions();
    val passwords = TestPasswords();
    val settings = TestSettings();
    val photos = TestPhotos();
    val faces = TestFaces();
    val locations = TestLocation(clock)
    val policy = VerificationPolicy()
    val setup = SetupAdminUseCase(repo, passwords, sessions, clock, settings)
    val login = LoginUseCase(repo, passwords, sessions)
    val change = ChangePasswordUseCase(repo, passwords, sessions, clock)
    val save = SaveStaffUseCase(repo, sessions, passwords, clock)
    val access = SetStaffAccessUseCase(repo, sessions, clock)
    val reset = ResetStaffPasswordUseCase(repo, sessions, passwords, clock)
    val importPhoto = ImportReferencePhotoUseCase(photos, faces)
    val capture = CaptureAttendanceUseCase(repo, sessions, photos, faces, policy, clock, settings)
    val submit = SubmitReviewUseCase(repo, sessions, clock)
    val decide = DecideReviewUseCase(repo, sessions, clock)
    val calculate = CalculateAttendanceUseCase(clock, settings)
    val send = SendNoticeUseCase(repo, sessions, clock)
    val readNotification = MarkNotificationReadUseCase(repo, sessions, clock)
    val calendar = SaveCalendarDayUseCase(repo, sessions, clock)
    val ensureCalendar = EnsureCalendarUseCase(repo, sessions, clock)
    val setZone = SetOfficeZoneUseCase(repo, sessions, settings)
    val setOffice = SetOfficeLocationUseCase(repo, sessions, settings)
    val geofence = ObserveGeofenceUseCase(locations, settings, clock)
    val admin = ObserveAdminUseCase(repo, sessions, clock, settings)
    val staff = ObserveStaffUseCase(repo, sessions, clock, settings, calculate)
    val history = ObserveAttendanceUseCase(repo, sessions, calculate)
    val inbox = ObserveInboxUseCase(repo, sessions)
    val holidays = ObserveHolidaysUseCase(repo, sessions)
    suspend fun asAdmin() {
        login("admin", PASSWORD, Role.ADMIN)
    }

    suspend fun asStaff(id: String = "e1") {
        login(if (id == "e1") "staff" else "staff2", PASSWORD, Role.STAFF)
    }

    suspend fun punch(
        action: PunchAction = PunchAction.CHECK_IN,
        score: Int = 8000
    ): VerificationAttempt {
        faces.score = score; return capture(action, "test://image", locations.current())
    }

    fun draft(id: String? = null, code: String = "EMP-003", username: String = "third") =
        StaffDraft(
            id,
            code,
            "New Employee",
            "new@example.test",
            "123",
            "Engineer",
            "Female",
            LocalDate.of(2026, 10, 1),
            username = username,
            temporaryPassword = PASSWORD,
            newPhotoKey = "new-reference"
        )

    companion object {
        const val PASSWORD = "Test-password-123"
        fun seed(): Workspace {
            val now = Instant.parse("2026-10-01T00:00:00Z")
            val employees = (1..2).map {
                Employee(
                    "e$it",
                    "EMP-00$it",
                    if (it == 1) "Alex Staff" else "Jamie Staff",
                    "staff$it@example.test",
                    "123",
                    "Engineer",
                    if (it == 1) "Female" else "Male",
                    LocalDate.of(2026, 10, 1),
                    createdAt = now,
                    updatedAt = now
                )
            }
            val accounts = listOf(
                Account(
                    "admin",
                    null,
                    "admin",
                    "test-only:$PASSWORD",
                    Role.ADMIN,
                    AccountStatus.ACTIVE,
                    false,
                    now,
                    now,
                    now
                )
            ) + employees.mapIndexed { i, e ->
                Account(
                    "a${i + 1}",
                    e.id,
                    if (i == 0) "staff" else "staff2",
                    "test-only:$PASSWORD",
                    Role.STAFF,
                    AccountStatus.ACTIVE,
                    false,
                    now,
                    now,
                    now
                )
            }
            val photos = employees.map {
                EmployeePhoto(
                    "p${it.id}",
                    it.id,
                    "reference-${it.id}",
                    "admin",
                    now
                )
            }
            val calendar = generateSequence(
                LocalDate.of(
                    2026,
                    1,
                    1
                )
            ) { it.plusDays(1) }.takeWhile { it.year <= 2027 }.map {
                CalendarDay(
                    it.toString(),
                    it,
                    if (it.dayOfWeek.value >= 6) DayType.WEEKLY_OFF else DayType.WORKING,
                    createdAt = now,
                    updatedAt = now
                )
            }.toList()
            return Workspace(
                employees = employees,
                accounts = accounts,
                photos = photos,
                calendar = calendar
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

suspend fun expectDomainError(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: DomainException) {
        return
    }
    throw AssertionError("Expected DomainException")
}
