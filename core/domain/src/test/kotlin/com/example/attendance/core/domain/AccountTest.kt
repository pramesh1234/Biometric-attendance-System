package com.example.attendance.core.domain
import com.example.attendance.core.model.*
import com.example.attendance.core.testing.*
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class AccountTest {

    @Test fun setupNormalizesAndCreatesOnlyOneAdmin() = runTest {
        val f=Fixture(MemoryRepository()); assertTrue(ObserveSetupUseCase(f.repo)().first())
        f.setup("  OWNER  ",PASSWORD,"Asia/Kolkata")
        val w=f.repo.read(); assertEquals("owner",w.accounts.single().username); assertEquals(Role.ADMIN,f.sessions.session.value!!.role)
        assertNotEquals(PASSWORD,w.accounts.single().passwordHash); assertFalse(ObserveSetupUseCase(f.repo)().first())
        assertEquals(ZoneId.of("Asia/Kolkata"),f.settings.zone.value); assertEquals(730,w.calendar.size)
        assertEquals(DayType.WEEKLY_OFF,w.calendar.first{it.date==LocalDate.of(2026,10,3)}.type)
        expectDomainError { f.setup("second",PASSWORD,"UTC") }; assertEquals(1,f.repo.read().accounts.size)
    }
    @Test fun invalidSetupDoesNotWriteAnything() = runTest {
        for ((u,p,z) in listOf(Triple("ab",PASSWORD,"UTC"),Triple("x".repeat(101),PASSWORD,"UTC"),Triple("admin","short","UTC"),Triple("admin","x".repeat(257),"UTC"),Triple("admin",PASSWORD,"invalid"))) {
            val f=Fixture(MemoryRepository()); expectDomainError { f.setup(u,p,z) }; assertEquals(Workspace(),f.repo.read()); assertNull(f.sessions.session.value)
        }
    }
    @Test fun loginNormalizesButEnforcesPasswordAndRole() = runTest {
        val f=Fixture(); f.login(" STAFF ",PASSWORD,Role.STAFF); assertEquals("e1",f.sessions.session.value!!.employeeId)
        SignOutUseCase(f.sessions)(); assertNull(ObserveSessionUseCase(f.sessions)().value)
        for ((u,p,r) in listOf(Triple("missing",PASSWORD,Role.STAFF),Triple("staff","wrong",Role.STAFF),Triple("staff",PASSWORD,Role.ADMIN))) {
            expectDomainError { f.login(u,p,r) }; assertNull(f.sessions.session.value)
        }
    }
    @Test fun blockedDisabledDeletedAndUnlinkedAccountsCannotLogin() = runTest {
        for (status in listOf(AccountStatus.BLOCKED,AccountStatus.DISABLED)) {
            val f=Fixture(); f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(status=status)else it}) to Unit}
            expectDomainError { f.asStaff() }
        }
        val f=Fixture(); f.repo.update{w->w.copy(employees=w.employees.map{it.copy(deletedAt=f.clock.instant())}) to Unit}; expectDomainError { f.asStaff() }
    }
    @Test fun loginRejectsAccessOrPasswordChangeDuringVerification() = runTest {
        val f=Fixture(); f.passwords.beforeMatch={f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(passwordHash="changed")else it}) to Unit}}
        expectDomainError { f.asStaff() }; assertNull(f.sessions.session.value)
    }
    @Test fun loginRejectsAccountRemovalDuringVerification() = runTest {
        val f=Fixture(); f.passwords.beforeMatch={f.repo.update{w->w.copy(accounts=w.accounts.filterNot{it.id=="a1"}) to Unit}}
        expectDomainError { f.asStaff() }
    }
    @Test fun temporaryPasswordMustChangeBeforeProtectedOperations() = runTest {
        val f=Fixture(); f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(mustChangePassword=true)else it}) to Unit}
        f.asStaff(); assertTrue(f.sessions.session.value!!.mustChangePassword); expectDomainError { f.punch() }
        f.clock.advanceSeconds(); f.change(PASSWORD,"New-password-123")
        assertFalse(f.sessions.session.value!!.mustChangePassword); assertEquals(f.clock.instant(),f.sessions.session.value!!.passwordVersion)
        SignOutUseCase(f.sessions)(); expectDomainError { f.asStaff() }; f.login("staff","New-password-123",Role.STAFF)
        f.punch(); assertEquals(1,f.repo.read().attendance.size)
    }
    @Test fun changePasswordRejectsWrongOldSameWeakAndLoggedOut() = runTest {
        val f=Fixture(); expectDomainError { f.change(PASSWORD,"New-password-123") }; f.asStaff()
        for ((old,new) in listOf("wrong" to "New-password-123",PASSWORD to PASSWORD,PASSWORD to "short")) expectDomainError { f.change(old,new) }
        assertEquals("test-only:$PASSWORD",f.repo.read().accounts.first{it.id=="a1"}.passwordHash)
    }
    @Test fun staleSessionCannotChangePassword() = runTest {
        val f=Fixture(); f.asStaff(); f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(passwordChangedAt=f.clock.instant())else it}) to Unit}
        expectDomainError { f.change(PASSWORD,"New-password-123") }
    }
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun sessionInvalidatesOnBlockAndKeepsFreshLogins() = runTest {
        val f=Fixture(); val job=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){ValidateSessionUseCase(f.repo,f.sessions)().collect()}
        f.asStaff(); runCurrent(); assertNotNull(f.sessions.session.value)
        f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(status=AccountStatus.BLOCKED)else it}) to Unit}; runCurrent(); assertNull(f.sessions.session.value)
        f.asAdmin(); runCurrent(); assertEquals(Role.ADMIN,f.sessions.session.value!!.role); job.cancel()
    }
    @Test fun authorizationChecksRoleVersionAndDeletedProfile() = runTest {
        val f=Fixture(); expectDomainError { f.punch() }; f.asStaff(); expectDomainError { f.save(f.draft()) }
        val original=f.sessions.session.value!!; f.sessions.set(original.copy(passwordVersion=f.clock.instant())); expectDomainError { f.punch() }
        f.sessions.set(original); f.repo.update{w->w.copy(employees=w.employees.map{if(it.id=="e1")it.copy(deletedAt=f.clock.instant())else it}) to Unit}; expectDomainError { f.punch() }
    }

}
