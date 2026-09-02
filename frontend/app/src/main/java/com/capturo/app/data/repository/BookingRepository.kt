package com.capturo.app.data.repository

import com.capturo.app.data.api.BookingApiService
import com.capturo.app.data.local.dao.BookingDao
import com.capturo.app.data.local.entity.BookingEntity
import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import com.capturo.app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val api: BookingApiService,
    private val bookingDao: BookingDao
) {
    fun createBooking(request: CreateBookingRequest): Flow<Resource<BookingCreatedResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.createBooking(request)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                
                // Cache newly created booking in database
                try {
                    bookingDao.insertBookings(listOf(
                        BookingEntity(
                            id = data.bookingId,
                            creatorId = request.creatorId,
                            status = data.status,
                            totalAmount = data.totalAmount,
                            date = request.eventDate
                        )
                    ))
                } catch (dbEx: Exception) {
                    // Silently ignore cache writing exceptions
                }

                emit(Resource.Success(data))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to create booking"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getMyBookings(status: String? = null, page: Int? = null, perPage: Int? = null): Flow<Resource<PaginatedResponse<BookingResponse>>> = flow {
        emit(Resource.Loading)
        
        // Offline Cache Fallback
        try {
            val cachedEntities = bookingDao.getBookings()
            if (cachedEntities.isNotEmpty()) {
                val mockBriefs = cachedEntities.map { entity ->
                    BookingResponse(
                        id = entity.id,
                        eventType = "photography",
                        location = "Cached Location",
                        eventDate = entity.date,
                        startTime = "00:00:00",
                        durationHours = 2.0,
                        totalAmount = entity.totalAmount,
                        status = entity.status,
                        createdAt = "",
                        attendeeId = "",
                        creatorId = entity.creatorId,
                        updatedAt = ""
                    )
                }
                emit(Resource.Success(
                    PaginatedResponse(
                        items = mockBriefs,
                        total = mockBriefs.size,
                        page = 1,
                        perPage = 50,
                        hasNext = false,
                        hasPrev = false
                    )
                ))
            }
        } catch (dbEx: Exception) {
            // Silently ignore cache reading exceptions
        }

        // Fetch fresh bookings from API
        try {
            val response = api.getMyBookings(status, page, perPage)
            if (response.isSuccessful && response.body() != null) {
                val paginatedResponse = response.body()!!
                emit(Resource.Success(paginatedResponse))

                // Overwrite cache with fresh items
                try {
                    val freshEntities = paginatedResponse.items.map { fresh ->
                        BookingEntity(
                            id = fresh.id,
                            creatorId = fresh.creatorId ?: fresh.creator?.id ?: "",
                            status = fresh.status,
                            totalAmount = fresh.totalAmount,
                            date = fresh.eventDate
                        )
                    }
                    bookingDao.deleteBookings()
                    bookingDao.insertBookings(freshEntities)
                } catch (dbEx: Exception) {
                    // Silently ignore cache writing exceptions
                }
            } else {
                emit(Resource.Error("Failed to fetch bookings list"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getBookingById(id: String): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getBookingById(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to load booking details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun updateBooking(id: String, request: UpdateBookingRequest): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.updateBooking(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to update booking"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun confirmBooking(id: String, request: PaymentVerifyRequest): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.confirmBooking(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to confirm payment"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun cancelBooking(id: String, request: CancelRequest): Flow<Resource<CancelBookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.cancelBooking(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to cancel booking"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun completeBooking(id: String): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.completeBooking(id)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to complete booking"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun disputeBooking(id: String, request: DisputeBookingRequest): Flow<Resource<BookingResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.disputeBooking(id, request)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to file dispute"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadInvoice(id: String): Flow<Resource<okhttp3.ResponseBody>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.downloadInvoice(id)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to download invoice PDF"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun downloadStatement(): Flow<Resource<okhttp3.ResponseBody>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.downloadStatement()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to download statement PDF"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getStatementSummary(): Flow<Resource<Map<String, String>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getStatementSummary()
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to load statement summary"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
