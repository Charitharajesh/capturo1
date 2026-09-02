package com.capturo.app.domain.usecase.booking

import com.capturo.app.data.model.request.CancelRequest
import com.capturo.app.data.repository.BookingRepository
import javax.inject.Inject

class CancelBookingUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    operator fun invoke(id: String, reason: String) =
        repository.cancelBooking(id, CancelRequest(reason))
}
