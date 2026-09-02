package com.capturo.app.domain.usecase.booking

import com.capturo.app.data.repository.BookingRepository
import javax.inject.Inject

class GetBookingsUseCase @Inject constructor(
    private val repository: BookingRepository
) {
    operator fun invoke(status: String? = null, page: Int? = null, perPage: Int? = null) =
        repository.getMyBookings(status, page, perPage)
}
