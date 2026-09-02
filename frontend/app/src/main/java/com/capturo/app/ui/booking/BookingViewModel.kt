package com.capturo.app.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.request.CancelRequest
import com.capturo.app.data.model.request.CreateBookingRequest
import com.capturo.app.data.model.request.ReviewRequest
import com.capturo.app.data.model.response.BookingCreatedResponse
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.model.response.CancelBookingResponse
import com.capturo.app.data.model.response.PaginatedResponse
import com.capturo.app.data.model.response.ReviewResponse
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.data.repository.ReviewRepository
import com.capturo.app.domain.usecase.booking.CreateBookingUseCase
import com.capturo.app.domain.usecase.booking.GetBookingsUseCase
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val createBookingUseCase: CreateBookingUseCase,
    private val getBookingsUseCase: GetBookingsUseCase,
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {

    private val _createBookingState = MutableLiveData<Resource<BookingCreatedResponse>>()
    val createBookingState: LiveData<Resource<BookingCreatedResponse>> = _createBookingState

    private val _bookingsState = MutableLiveData<Resource<PaginatedResponse<BookingResponse>>>()
    val bookingsState: LiveData<Resource<PaginatedResponse<BookingResponse>>> = _bookingsState

    private val _currentBooking = MutableLiveData<Resource<BookingResponse>>()
    val currentBooking: LiveData<Resource<BookingResponse>> = _currentBooking

    private val _cancelState = MutableLiveData<Resource<CancelBookingResponse>>()
    val cancelState: LiveData<Resource<CancelBookingResponse>> = _cancelState

    private val _submitReviewState = MutableLiveData<Resource<ReviewResponse>>()
    val submitReviewState: LiveData<Resource<ReviewResponse>> = _submitReviewState

    private val _completeBookingState = MutableLiveData<Resource<BookingResponse>>()
    val completeBookingState: LiveData<Resource<BookingResponse>> = _completeBookingState

    private val _invoiceState = MutableLiveData<Resource<okhttp3.ResponseBody>?>()
    val invoiceState: LiveData<Resource<okhttp3.ResponseBody>?> = _invoiceState

    fun createBooking(request: CreateBookingRequest) {
        viewModelScope.launch {
            createBookingUseCase(request).collectLatest { resource ->
                _createBookingState.value = resource
            }
        }
    }

    fun loadBookings(status: String) {
        viewModelScope.launch {
            getBookingsUseCase(status = status).collectLatest { resource ->
                _bookingsState.value = resource
            }
        }
    }

    fun loadBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.getBookingById(bookingId).collectLatest { resource ->
                _currentBooking.value = resource
            }
        }
    }

    fun cancelBooking(bookingId: String, reason: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId, CancelRequest(reason)).collectLatest { resource ->
                _cancelState.value = resource
            }
        }
    }

    fun submitReview(request: ReviewRequest) {
        viewModelScope.launch {
            reviewRepository.submitReview(request).collectLatest { resource ->
                _submitReviewState.value = resource
            }
        }
    }

    fun completeBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.completeBooking(bookingId).collectLatest { resource ->
                _completeBookingState.value = resource
            }
        }
    }

    fun downloadInvoice(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.downloadInvoice(bookingId).collectLatest { resource ->
                _invoiceState.value = resource
            }
        }
    }

    fun resetInvoiceState() {
        _invoiceState.value = null
    }
}
