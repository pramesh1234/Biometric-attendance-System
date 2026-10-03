package com.example.attendance

import com.example.attendance.core.domain.CalculateAttendanceUseCase
import com.example.attendance.core.domain.CaptureAttendanceUseCase
import com.example.attendance.core.domain.ChangePasswordUseCase
import com.example.attendance.core.domain.DecideReviewUseCase
import com.example.attendance.core.domain.EnsureCalendarUseCase
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
import com.example.attendance.core.domain.ObserveSessionUseCase
import com.example.attendance.core.domain.ObserveSetupUseCase
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
import com.example.attendance.core.domain.SignOutUseCase
import com.example.attendance.core.domain.SubmitReviewUseCase
import com.example.attendance.core.domain.ValidateSessionUseCase
import com.example.attendance.core.domain.VerificationPolicy
import com.example.attendance.core.domain.WorkspaceRepository
import com.example.attendance.core.testing.Fixture
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

object TestGraph {
    lateinit var fixture: Fixture
}

@Module
@InstallIn(SingletonComponent::class)
object TestModule {
    @Provides
    @Singleton
    fun clock(): Clock = TestGraph.fixture.clock

    @Provides
    @Singleton
    fun repository(): WorkspaceRepository = TestGraph.fixture.repo

    @Provides
    @Singleton
    fun sessions(): SessionStore = TestGraph.fixture.sessions

    @Provides
    @Singleton
    fun passwords(): PasswordHasher = TestGraph.fixture.passwords

    @Provides
    @Singleton
    fun settings(): OfficeSettings = TestGraph.fixture.settings

    @Provides
    @Singleton
    fun photos(): PhotoStore = TestGraph.fixture.photos

    @Provides
    @Singleton
    fun faces(): FaceVerifier = TestGraph.fixture.faces

    @Provides
    @Singleton
    fun locations(): LocationProvider = TestGraph.fixture.locations

    @Provides
    fun verificationPolicy() = VerificationPolicy()

    @Provides
    fun importReferencePhotoUseCase(
        store: PhotoStore,
        verifier: FaceVerifier
    ): ImportReferencePhotoUseCase = ImportReferencePhotoUseCase(store, verifier)

    @Provides
    fun saveStaffUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        passwords: PasswordHasher,
        clock: Clock
    ): SaveStaffUseCase = SaveStaffUseCase(repo, sessions, passwords, clock)

    @Provides
    fun setStaffAccessUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): SetStaffAccessUseCase = SetStaffAccessUseCase(repo, sessions, clock)

    @Provides
    fun resetStaffPasswordUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        passwords: PasswordHasher,
        clock: Clock
    ): ResetStaffPasswordUseCase = ResetStaffPasswordUseCase(repo, sessions, passwords, clock)

    @Provides
    fun captureAttendanceUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        photos: PhotoStore,
        faces: FaceVerifier,
        policy: VerificationPolicy,
        clock: Clock,
        settings: OfficeSettings
    ): CaptureAttendanceUseCase =
        CaptureAttendanceUseCase(repo, sessions, photos, faces, policy, clock, settings)

    @Provides
    fun submitReviewUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): SubmitReviewUseCase = SubmitReviewUseCase(repo, sessions, clock)

    @Provides
    fun decideReviewUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): DecideReviewUseCase = DecideReviewUseCase(repo, sessions, clock)

    @Provides
    fun calculateAttendanceUseCase(
        clock: Clock,
        settings: OfficeSettings
    ): CalculateAttendanceUseCase = CalculateAttendanceUseCase(clock, settings)

    @Provides
    fun observeAdminUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock,
        settings: OfficeSettings
    ): ObserveAdminUseCase = ObserveAdminUseCase(repo, sessions, clock, settings)

    @Provides
    fun observeStaffUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock,
        settings: OfficeSettings,
        calculate: CalculateAttendanceUseCase
    ): ObserveStaffUseCase = ObserveStaffUseCase(repo, sessions, clock, settings, calculate)

    @Provides
    fun observeAttendanceUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        calculate: CalculateAttendanceUseCase
    ): ObserveAttendanceUseCase = ObserveAttendanceUseCase(repo, sessions, calculate)

    @Provides
    fun observeInboxUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore
    ): ObserveInboxUseCase = ObserveInboxUseCase(repo, sessions)

    @Provides
    fun observeHolidaysUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore
    ): ObserveHolidaysUseCase = ObserveHolidaysUseCase(repo, sessions)

    @Provides
    fun sendNoticeUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): SendNoticeUseCase = SendNoticeUseCase(repo, sessions, clock)

    @Provides
    fun markNotificationReadUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): MarkNotificationReadUseCase = MarkNotificationReadUseCase(repo, sessions, clock)

    @Provides
    fun saveCalendarDayUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): SaveCalendarDayUseCase = SaveCalendarDayUseCase(repo, sessions, clock)

    @Provides
    fun ensureCalendarUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        clock: Clock
    ): EnsureCalendarUseCase = EnsureCalendarUseCase(repo, sessions, clock)

    @Provides
    fun setOfficeZoneUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        settings: OfficeSettings
    ): SetOfficeZoneUseCase = SetOfficeZoneUseCase(repo, sessions, settings)

    @Provides
    fun setOfficeLocationUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore,
        settings: OfficeSettings
    ): SetOfficeLocationUseCase = SetOfficeLocationUseCase(repo, sessions, settings)

    @Provides
    fun observeGeofenceUseCase(
        locations: LocationProvider,
        settings: OfficeSettings,
        clock: Clock
    ): ObserveGeofenceUseCase = ObserveGeofenceUseCase(locations, settings, clock)

    @Provides
    fun observeSessionUseCase(sessions: SessionStore): ObserveSessionUseCase =
        ObserveSessionUseCase(sessions)

    @Provides
    fun signOutUseCase(sessions: SessionStore): SignOutUseCase = SignOutUseCase(sessions)

    @Provides
    fun observeSetupUseCase(repo: WorkspaceRepository): ObserveSetupUseCase =
        ObserveSetupUseCase(repo)

    @Provides
    fun setupAdminUseCase(
        repo: WorkspaceRepository,
        passwords: PasswordHasher,
        sessions: SessionStore,
        clock: Clock,
        settings: OfficeSettings
    ): SetupAdminUseCase = SetupAdminUseCase(repo, passwords, sessions, clock, settings)

    @Provides
    fun loginUseCase(
        repo: WorkspaceRepository,
        passwords: PasswordHasher,
        sessions: SessionStore
    ): LoginUseCase = LoginUseCase(repo, passwords, sessions)

    @Provides
    fun changePasswordUseCase(
        repo: WorkspaceRepository,
        passwords: PasswordHasher,
        sessions: SessionStore,
        clock: Clock
    ): ChangePasswordUseCase = ChangePasswordUseCase(repo, passwords, sessions, clock)

    @Provides
    fun validateSessionUseCase(
        repo: WorkspaceRepository,
        sessions: SessionStore
    ): ValidateSessionUseCase = ValidateSessionUseCase(repo, sessions)
}
