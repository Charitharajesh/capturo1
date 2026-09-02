package com.capturo.app.domain.usecase.booking

import com.capturo.app.data.model.request.CreateBookingRequest
import com.capturo.app.data.repository.BookingRepository
import javax.inject.Inject

class CreateBookingUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    operator fun invoke(request: CreateBookingRequest) = repository.createBooking(request)
}
