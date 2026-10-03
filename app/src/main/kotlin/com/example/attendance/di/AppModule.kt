package com.example.attendance.di
import android.content.Context
import com.example.attendance.core.domain.*
import com.example.attendance.core.data.*
import com.example.attendance.core.database.*
import com.example.attendance.core.device.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import java.time.Clock

@Module @InstallIn(SingletonComponent::class) object AppModule {
 @Provides @Singleton fun clock(): Clock = Clock.tick(Clock.systemUTC(), java.time.Duration.ofMillis(1))
 @Provides @Singleton fun database(@ApplicationContext context: Context): AttendanceDatabase = createAttendanceDatabase(context)
 @Provides @Singleton fun repository(db: AttendanceDatabase): WorkspaceRepository = RoomWorkspaceRepository(db)
 @Provides @Singleton fun sessions(@ApplicationContext context: Context): SessionStore = PersistentSessionStore(context)
 @Provides @Singleton fun passwords(): PasswordHasher = Pbkdf2PasswordHasher()
 @Provides @Singleton fun settings(@ApplicationContext context: Context): OfficeSettings = LocalOfficeSettings(context)
 @Provides @Singleton fun photos(@ApplicationContext context: Context): PhotoStore = PrivatePhotoStore(context)
 @Provides @Singleton fun faces(@ApplicationContext context: Context, photos: PhotoStore): FaceVerifier = OpenCvFaceVerifier(context, photos)
 @Provides @Singleton fun locations(@ApplicationContext context: Context): LocationProvider = AndroidLocationProvider(context)
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
