package com.example.attendance
import androidx.lifecycle.*
import com.example.attendance.core.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
@HiltViewModel class SessionViewModel @Inject constructor(observe:ObserveSessionUseCase,private val signOut:SignOutUseCase,validate:ValidateSessionUseCase):ViewModel(){
    val session=observe()
    init{validate().launchIn(viewModelScope)}
    fun signOut()=signOut.invoke()
}
