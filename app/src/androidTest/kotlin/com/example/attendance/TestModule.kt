package com.example.attendance
import com.example.attendance.core.domain.*
import com.example.attendance.core.testing.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import java.time.Clock
object TestGraph { lateinit var fixture: Fixture }
@Module @InstallIn(SingletonComponent::class) object TestModule {
 @Provides @Singleton fun clock():Clock=TestGraph.fixture.clock
 @Provides @Singleton fun repository():WorkspaceRepository=TestGraph.fixture.repo
 @Provides @Singleton fun sessions():SessionStore=TestGraph.fixture.sessions
 @Provides @Singleton fun passwords():PasswordHasher=TestGraph.fixture.passwords
 @Provides @Singleton fun settings():OfficeSettings=TestGraph.fixture.settings
 @Provides @Singleton fun photos():PhotoStore=TestGraph.fixture.photos
 @Provides @Singleton fun faces():FaceVerifier=TestGraph.fixture.faces
 @Provides @Singleton fun locations():LocationProvider=TestGraph.fixture.locations
 @Provides fun verificationPolicy() = VerificationPolicy()
 @Provides fun importReferencePhotoUseCase(store: PhotoStore, verifier: FaceVerifier): ImportReferencePhotoUseCase = ImportReferencePhotoUseCase(store, verifier)
 @Provides fun saveStaffUseCase(repo: WorkspaceRepository, sessions: SessionStore, passwords: PasswordHasher, clock: Clock): SaveStaffUseCase = SaveStaffUseCase(repo, sessions, passwords, clock)
 @Provides fun setStaffAccessUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): SetStaffAccessUseCase = SetStaffAccessUseCase(repo, sessions, clock)
 @Provides fun resetStaffPasswordUseCase(repo: WorkspaceRepository, sessions: SessionStore, passwords: PasswordHasher, clock: Clock): ResetStaffPasswordUseCase = ResetStaffPasswordUseCase(repo, sessions, passwords, clock)
 @Provides fun captureAttendanceUseCase(repo: WorkspaceRepository, sessions: SessionStore, photos: PhotoStore, faces: FaceVerifier, policy: VerificationPolicy, clock: Clock, settings: OfficeSettings): CaptureAttendanceUseCase = CaptureAttendanceUseCase(repo, sessions, photos, faces, policy, clock, settings)
 @Provides fun submitReviewUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): SubmitReviewUseCase = SubmitReviewUseCase(repo, sessions, clock)
 @Provides fun decideReviewUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): DecideReviewUseCase = DecideReviewUseCase(repo, sessions, clock)
 @Provides fun calculateAttendanceUseCase(clock: Clock, settings: OfficeSettings): CalculateAttendanceUseCase = CalculateAttendanceUseCase(clock, settings)
 @Provides fun observeAdminUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock, settings: OfficeSettings): ObserveAdminUseCase = ObserveAdminUseCase(repo, sessions, clock, settings)
 @Provides fun observeStaffUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock, settings: OfficeSettings, calculate: CalculateAttendanceUseCase): ObserveStaffUseCase = ObserveStaffUseCase(repo, sessions, clock, settings, calculate)
 @Provides fun observeAttendanceUseCase(repo: WorkspaceRepository, sessions: SessionStore, calculate: CalculateAttendanceUseCase): ObserveAttendanceUseCase = ObserveAttendanceUseCase(repo, sessions, calculate)
 @Provides fun observeInboxUseCase(repo: WorkspaceRepository, sessions: SessionStore): ObserveInboxUseCase = ObserveInboxUseCase(repo, sessions)
 @Provides fun observeHolidaysUseCase(repo: WorkspaceRepository, sessions: SessionStore): ObserveHolidaysUseCase = ObserveHolidaysUseCase(repo, sessions)
 @Provides fun sendNoticeUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): SendNoticeUseCase = SendNoticeUseCase(repo, sessions, clock)
 @Provides fun markNotificationReadUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): MarkNotificationReadUseCase = MarkNotificationReadUseCase(repo, sessions, clock)
 @Provides fun saveCalendarDayUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): SaveCalendarDayUseCase = SaveCalendarDayUseCase(repo, sessions, clock)
 @Provides fun ensureCalendarUseCase(repo: WorkspaceRepository, sessions: SessionStore, clock: Clock): EnsureCalendarUseCase = EnsureCalendarUseCase(repo, sessions, clock)
 @Provides fun setOfficeZoneUseCase(repo: WorkspaceRepository, sessions: SessionStore, settings: OfficeSettings): SetOfficeZoneUseCase = SetOfficeZoneUseCase(repo, sessions, settings)
 @Provides fun setOfficeLocationUseCase(repo: WorkspaceRepository, sessions: SessionStore, settings: OfficeSettings): SetOfficeLocationUseCase = SetOfficeLocationUseCase(repo, sessions, settings)
 @Provides fun observeGeofenceUseCase(locations: LocationProvider, settings: OfficeSettings, clock: Clock): ObserveGeofenceUseCase = ObserveGeofenceUseCase(locations, settings, clock)
 @Provides fun observeSessionUseCase(sessions: SessionStore): ObserveSessionUseCase = ObserveSessionUseCase(sessions)
 @Provides fun signOutUseCase(sessions: SessionStore): SignOutUseCase = SignOutUseCase(sessions)
 @Provides fun observeSetupUseCase(repo: WorkspaceRepository): ObserveSetupUseCase = ObserveSetupUseCase(repo)
 @Provides fun setupAdminUseCase(repo: WorkspaceRepository, passwords: PasswordHasher, sessions: SessionStore, clock: Clock, settings: OfficeSettings): SetupAdminUseCase = SetupAdminUseCase(repo, passwords, sessions, clock, settings)
 @Provides fun loginUseCase(repo: WorkspaceRepository, passwords: PasswordHasher, sessions: SessionStore): LoginUseCase = LoginUseCase(repo, passwords, sessions)
 @Provides fun changePasswordUseCase(repo: WorkspaceRepository, passwords: PasswordHasher, sessions: SessionStore, clock: Clock): ChangePasswordUseCase = ChangePasswordUseCase(repo, passwords, sessions, clock)
 @Provides fun validateSessionUseCase(repo: WorkspaceRepository, sessions: SessionStore): ValidateSessionUseCase = ValidateSessionUseCase(repo, sessions)
}
