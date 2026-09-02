package com.capturo.app.data.repository

import com.capturo.app.data.api.PaymentApiService
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
class PaymentRepository @Inject constructor(
    private val api: PaymentApiService
) {
    fun createRazorpayOrder(bookingId: String): Flow<Resource<PaymentOrderResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.createRazorpayOrder(CreatePaymentOrderRequest(bookingId))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Order creation failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun verifyPaymentSignature(orderId: String, paymentId: String, signature: String): Flow<Resource<VerifyPaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.verifyPaymentSignature(VerifyPaymentRequest(orderId, paymentId, signature))
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Payment verification failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getPaymentHistory(page: Int? = null, perPage: Int? = null): Flow<Resource<PaginatedResponse<PaymentResponse>>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getPaymentHistory(page, perPage)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch payment history"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)

    fun getPaymentDetails(paymentId: String): Flow<Resource<PaymentResponse>> = flow {
        emit(Resource.Loading)
        try {
            val response = api.getPaymentDetails(paymentId)
            if (response.isSuccessful && response.body()?.success == true) {
                emit(Resource.Success(response.body()!!.data!!))
            } else {
                emit(Resource.Error(response.body()?.message ?: "Failed to fetch payment details"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Connection error"))
        }
    }.flowOn(Dispatchers.IO)
}
