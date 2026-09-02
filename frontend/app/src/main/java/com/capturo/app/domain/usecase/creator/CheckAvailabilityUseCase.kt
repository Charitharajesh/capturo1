package com.capturo.app.domain.usecase.creator

import com.capturo.app.data.repository.CreatorRepository
import javax.inject.Inject

class CheckAvailabilityUseCase @Inject constructor(
    private val repository: CreatorRepository
) {
    operator fun invoke(id: String, date: String, startTime: String, duration: Double) =
        repository.checkAvailability(id, date, startTime, duration)
}
