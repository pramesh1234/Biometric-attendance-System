package com.example.attendance
import androidx.lifecycle.ViewModelStore
import com.example.attendance.core.testing.*
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
 @get:Rule val main=MainDispatcherRule()
 @Test fun blockedSessionIsClearedAndSignOutClearsFreshSession()=runTest {
  val f=Fixture();f.asStaff();val vm=SessionViewModel(ObserveSessionUseCase(f.sessions),SignOutUseCase(f.sessions),ValidateSessionUseCase(f.repo,f.sessions));val store=ViewModelStore();store.put("vm",vm);runCurrent();assertNotNull(vm.session.value)
  f.repo.update{w->w.copy(accounts=w.accounts.map{if(it.id=="a1")it.copy(status=AccountStatus.BLOCKED)else it}) to Unit};runCurrent();assertNull(vm.session.value)
  f.asAdmin();runCurrent();assertNotNull(vm.session.value);vm.signOut();runCurrent();assertNull(vm.session.value);store.clear()
 }
}
