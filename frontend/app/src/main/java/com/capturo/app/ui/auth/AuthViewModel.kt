package com.capturo.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.request.RegisterRequest
import com.capturo.app.data.model.response.AuthResponse
import com.capturo.app.data.model.response.UserResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.domain.usecase.auth.LoginUseCase
import com.capturo.app.domain.usecase.auth.RegisterUseCase
import com.capturo.app.data.repository.AuthRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<AuthResponse>>()
    val loginState: LiveData<Resource<AuthResponse>> get() = _loginState

    private val _registerState = MutableLiveData<Resource<UserResponse>>()
    val registerState: LiveData<Resource<UserResponse>> get() = _registerState

    private val _forgotPasswordState = MutableLiveData<Resource<Unit>>()
    val forgotPasswordState: LiveData<Resource<Unit>> get() = _forgotPasswordState

    private val _resetPasswordState = MutableLiveData<Resource<Unit>>()
    val resetPasswordState: LiveData<Resource<Unit>> get() = _resetPasswordState

    val currentUserRole: UserRole
         get() = sessionManager.getUserRole()

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            loginUseCase(email, pass).collect {
                _loginState.value = it
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            registerUseCase(request).collect {
                _registerState.value = it
            }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            authRepository.forgotPassword(email).collect {
                _forgotPasswordState.value = it
            }
        }
    }

    fun resetPassword(token: String, newPass: String) {
        viewModelScope.launch {
            authRepository.resetPassword(token, newPass).collect {
                _resetPasswordState.value = it
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
