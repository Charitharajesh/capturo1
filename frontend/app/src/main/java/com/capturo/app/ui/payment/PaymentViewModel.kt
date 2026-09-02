package com.capturo.app.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.data.model.request.PaymentVerifyRequest
import com.capturo.app.data.model.response.BookingResponse
import com.capturo.app.data.repository.BookingRepository
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _paymentConfirmationState = MutableLiveData<Resource<BookingResponse>>()
    val paymentConfirmationState: LiveData<Resource<BookingResponse>> = _paymentConfirmationState

    fun verifyAndConfirmPayment(bookingId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            val request = PaymentVerifyRequest(paymentId, signature)
            bookingRepository.confirmBooking(bookingId, request).collectLatest { resource ->
                _paymentConfirmationState.value = resource
            }
        }
    }
}
