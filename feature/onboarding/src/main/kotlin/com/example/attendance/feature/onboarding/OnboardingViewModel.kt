package com.example.attendance.feature.onboarding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.core.domain.*
import com.example.attendance.core.model.Role
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel class OnboardingViewModel @Inject constructor(observeSetup: ObserveSetupUseCase, private val setup: SetupAdminUseCase, private val login: LoginUseCase, private val changePassword: ChangePasswordUseCase, settings: OfficeSettings) : ViewModel() {
    val needsSetup = observeSetup().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val initialZone = settings.zone.value.id
    private val _busy = MutableStateFlow(false); val busy = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null); val error = _error.asStateFlow()
    fun signIn(username: String, password: String, admin: Boolean) = run { login(username, password, if (admin) Role.ADMIN else Role.STAFF) }
    fun createAdmin(username: String, password: String, zone: String) = run { setup(username, password, zone) }
    fun change(old: String, new: String) = run { changePassword(old, new) }
    private fun run(block: suspend () -> Unit) { if (!_busy.compareAndSet(false, true)) return; viewModelScope.launch { _error.value = null; try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) { _error.value = e.message ?: "Unable to complete this action." } finally { _busy.value = false } } }
}
