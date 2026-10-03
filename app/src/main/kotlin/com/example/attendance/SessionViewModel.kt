package com.example.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendance.core.domain.ObserveSessionUseCase
import com.example.attendance.core.domain.SignOutUseCase
import com.example.attendance.core.domain.ValidateSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    observe: ObserveSessionUseCase,
    private val signOut: SignOutUseCase,
    validate: ValidateSessionUseCase
) : ViewModel() {
    val session = observe()

    init {
        validate().launchIn(viewModelScope)
    }

    fun signOut() = signOut.invoke()
}
