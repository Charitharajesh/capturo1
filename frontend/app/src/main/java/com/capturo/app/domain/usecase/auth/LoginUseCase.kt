package com.capturo.app.domain.usecase.auth

import com.capturo.app.data.model.request.LoginRequest
import com.capturo.app.data.repository.AuthRepository
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(email: String, psw: String) = repository.login(LoginRequest(email, psw))
}
