package com.example.attendance.core.domain

import com.example.attendance.core.model.AccountStatus
import com.example.attendance.core.model.Role
import com.example.attendance.core.testing.Fixture
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import com.example.attendance.core.testing.expectDomainError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StaffManagementTest {

    @Test
    fun createTrimsFieldsAndMakesLinkedTemporaryAccount() = runTest {
        val f = Fixture(); f.asAdmin();
        val id = f.save(
            f.draft().copy(name = "  New Employee  ", username = " THIRD ", code = " EMP-003 ")
        )
        val w = f.repo.read(); assertEquals("New Employee", w.employees.first { it.id == id }.name)
        val a = w.accounts.first { it.employeeId == id }; assertEquals(
        "third",
        a.username
    ); assertTrue(a.mustChangePassword); assertEquals(Role.STAFF, a.role)
        assertEquals("new-reference", w.photos.first { it.employeeId == id }.fileKey)
    }

    @Test
    fun staffValidationDoesNotPartiallyWrite() = runTest {
        val f = Fixture(); f.asAdmin();
        val d = f.draft();
        val before = f.repo.read()
        for (bad in listOf(
            d.copy(name = " "),
            d.copy(code = " "),
            d.copy(username = "x"),
            d.copy(newPhotoKey = null),
            d.copy(temporaryPassword = "short"),
            d.copy(endDate = d.joiningDate.minusDays(1)),
            d.copy(joiningDate = LocalDate.of(1969, 1, 1)),
            d.copy(joiningDate = LocalDate.of(2030, 1, 1)),
            d.copy(code = "emp-001"),
            d.copy(username = "STAFF"),
            d.copy(id = "missing")
        )) expectDomainError { f.save(bad) }
        assertEquals(before, f.repo.read())
    }

    @Test
    fun editPreservesPasswordAndReplacesPhotoWithHistory() = runTest {
        val f = Fixture(); f.asAdmin();
        val before = f.repo.read();
        val old = before.accounts.first { it.employeeId == "e1" };
        val created = before.employees.first().createdAt
        f.save(f.draft("e1", "EMP-001", "staff").copy(temporaryPassword = "", name = "Edited"))
        val w = f.repo.read(); assertEquals(
        old.passwordHash,
        w.accounts.first { it.employeeId == "e1" }.passwordHash
    )
        assertEquals(created, w.employees.first { it.id == "e1" }.createdAt); assertEquals(
        "Edited",
        w.employees.first { it.id == "e1" }.name
    )
        assertNotNull(w.photos.first { it.id == "pe1" }.retiredAt); assertEquals(
        1,
        w.photos.count { it.employeeId == "e1" && it.retiredAt == null })
        f.save(
            f.draft("e1", "EMP-001", "staff").copy(temporaryPassword = "", newPhotoKey = null)
        ); assertEquals(3, f.repo.read().photos.size)
    }

    @Test
    fun editPasswordForcesChangeAndUpdatesVersion() = runTest {
        val f = Fixture(); f.asAdmin(); f.save(
        f.draft("e1", "EMP-001", "staff")
            .copy(temporaryPassword = "New-password-123", newPhotoKey = null)
    )
        val a =
            f.repo.read().accounts.first { it.employeeId == "e1" }; assertTrue(a.mustChangePassword); assertEquals(
        f.clock.instant(),
        a.passwordChangedAt
    )
    }

    @Test
    fun blockUnblockAndSoftDeleteRetainEvidence() = runTest {
        val f = Fixture(); f.asStaff(); f.punch(); f.asAdmin(); f.access("e1", true)
        assertEquals(
            AccountStatus.BLOCKED,
            f.repo.read().accounts.first { it.id == "a1" }.status
        ); expectDomainError { f.asStaff() }
        f.access("e1", false); f.asStaff(); f.asAdmin(); f.access("e1", true, true)
        val w =
            f.repo.read(); assertNotNull(w.employees.first { it.id == "e1" }.deletedAt); assertEquals(
        1,
        w.attendance.size
    ); assertEquals(1, w.attempts.size)
        assertEquals(
            AccountStatus.DISABLED,
            w.accounts.first { it.id == "a1" }.status
        ); expectDomainError { f.access("e1", false) }; expectDomainError { f.asStaff() }
    }

    @Test
    fun passwordResetInvalidatesOldCredentialsAndRejectsUnknownStaff() = runTest {
        val f = Fixture(); f.asAdmin(); expectDomainError {
        f.reset(
            "missing",
            PASSWORD
        )
    }; expectDomainError { f.reset("e1", "tiny") }
        f.reset("e1", "Reset-password-123"); expectDomainError { f.asStaff() }; f.login(
        "staff",
        "Reset-password-123",
        Role.STAFF
    ); assertTrue(f.sessions.session.value!!.mustChangePassword)
    }

    @Test
    fun invalidReferenceDeletesImportedFile() = runTest {
        val f = Fixture();
        val key = f.importPhoto("uri"); assertTrue(key in f.photos.imported)
        f.faces.invalidReference = true; expectDomainError { f.importPhoto("bad") }; assertEquals(
        listOf("image-1"),
        f.photos.deleted
    )
    }

}
