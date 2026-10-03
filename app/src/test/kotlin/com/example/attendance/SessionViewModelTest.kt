package com.example.attendance

import androidx.lifecycle.ViewModelStore
import com.example.attendance.core.domain.ObserveSessionUseCase
import com.example.attendance.core.domain.SignOutUseCase
import com.example.attendance.core.domain.ValidateSessionUseCase
import com.example.attendance.core.model.AccountStatus
import com.example.attendance.core.testing.Fixture
import com.example.attendance.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun blockedSessionIsClearedAndSignOutClearsFreshSession() = runTest {
        val f = Fixture(); f.asStaff();
        val vm = SessionViewModel(
            ObserveSessionUseCase(f.sessions),
            SignOutUseCase(f.sessions),
            ValidateSessionUseCase(f.repo, f.sessions)
        );
        val store = ViewModelStore(); store.put(
        "vm",
        vm
    ); runCurrent(); assertNotNull(vm.session.value)
        f.repo.update { w -> w.copy(accounts = w.accounts.map { if (it.id == "a1") it.copy(status = AccountStatus.BLOCKED) else it }) to Unit }; runCurrent(); assertNull(
        vm.session.value
    )
        f.asAdmin(); runCurrent(); assertNotNull(vm.session.value); vm.signOut(); runCurrent(); assertNull(
        vm.session.value
    ); store.clear()
    }
}
