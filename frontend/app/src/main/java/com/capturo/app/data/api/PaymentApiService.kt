package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface PaymentApiService {
    @POST("payments/create-order")
    suspend fun createRazorpayOrder(
        @Body request: CreatePaymentOrderRequest
    ): Response<ApiResponse<PaymentOrderResponse>>

    @POST("payments/verify")
    suspend fun verifyPaymentSignature(
        @Body request: VerifyPaymentRequest
    ): Response<ApiResponse<VerifyPaymentResponse>>

    @GET("payments/history")
    suspend fun getPaymentHistory(
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<PaymentResponse>>

    @GET("payments/{payment_id}")
    suspend fun getPaymentDetails(
        @Path("payment_id") paymentId: String
    ): Response<ApiResponse<PaymentResponse>>
}
