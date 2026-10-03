package com.example.attendance.feature.onboarding

import androidx.lifecycle.ViewModelStore
import com.example.attendance.core.domain.ObserveSetupUseCase
import com.example.attendance.core.model.Role
import com.example.attendance.core.testing.Fixture
import com.example.attendance.core.testing.Fixture.Companion.PASSWORD
import com.example.attendance.core.testing.MainDispatcherRule
import com.example.attendance.core.testing.MemoryRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun vm(f: Fixture) =
        OnboardingViewModel(ObserveSetupUseCase(f.repo), f.setup, f.login, f.change, f.settings)

    @Test
    fun loginFailureShowsErrorAndCanRecover() = runTest {
        val f = Fixture();
        val vm = vm(f)
        vm.signIn(
            "staff",
            "wrong",
            false
        ); runCurrent(); assertNotNull(vm.error.value); assertFalse(vm.busy.value)
        vm.signIn("staff", PASSWORD, false); runCurrent(); assertNull(vm.error.value); assertEquals(
        "e1",
        f.sessions.session.value!!.employeeId
    )
    }

    @Test
    fun setupAndPasswordChangeReachSession() = runTest {
        val f = Fixture(MemoryRepository());
        val vm = vm(f)
        vm.createAdmin("admin", PASSWORD, "UTC"); runCurrent(); assertEquals(
        Role.ADMIN,
        f.sessions.session.value!!.role
    )
        vm.change(
            PASSWORD,
            "New-password-123"
        ); runCurrent(); assertNull(vm.error.value); assertFalse(vm.busy.value)
    }

    @Test
    fun repeatedTapWhilePendingDoesNotLaunchTwoLogins() = runTest {
        val f = Fixture();
        val vm = vm(f);
        val gate = CompletableDeferred<Unit>();
        var calls = 0
        f.passwords.beforeMatch = { calls++; gate.await() }
        vm.signIn("staff", PASSWORD, false); vm.signIn("staff", PASSWORD, false); runCurrent()
        assertTrue(vm.busy.value); assertEquals(
        1,
        calls
    ); gate.complete(Unit); runCurrent(); assertFalse(vm.busy.value)
    }

    @Test
    fun subscribedSetupStateUpdatesAndCancellationDoesNotShowError() = runTest {
        val f = Fixture(MemoryRepository());
        val vm = vm(f);
        val store = ViewModelStore(); store.put("vm", vm)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.needsSetup.collect() }; runCurrent(); assertEquals(
        true,
        vm.needsSetup.value
    )
        vm.createAdmin("admin", PASSWORD, "UTC"); runCurrent(); assertEquals(
        false,
        vm.needsSetup.value
    )
        f.passwords.beforeMatch = { awaitCancellation() }; vm.signIn(
        "admin",
        PASSWORD,
        true
    ); runCurrent(); store.clear(); runCurrent(); assertNull(vm.error.value); assertFalse(vm.busy.value)
    }

}
