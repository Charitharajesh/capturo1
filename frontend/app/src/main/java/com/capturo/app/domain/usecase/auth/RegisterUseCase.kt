package com.capturo.app.domain.usecase.auth

import com.capturo.app.data.model.request.RegisterRequest
import com.capturo.app.data.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(request: RegisterRequest) = repository.register(request)
}
