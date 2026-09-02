package com.capturo.app.data.api

import com.capturo.app.data.model.request.*
import com.capturo.app.data.model.response.*
import retrofit2.Response
import retrofit2.http.*

interface BookingApiService {
    @POST("bookings")
    suspend fun createBooking(
        @Body request: CreateBookingRequest
    ): Response<ApiResponse<BookingCreatedResponse>>

    @GET("bookings")
    suspend fun getMyBookings(
        @Query("status") status: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): Response<PaginatedResponse<BookingResponse>>

    @GET("bookings/{id}")
    suspend fun getBookingById(
        @Path("id") id: String
    ): Response<ApiResponse<BookingResponse>>

    @PATCH("bookings/{id}")
    suspend fun updateBooking(
        @Path("id") id: String,
        @Body request: UpdateBookingRequest
    ): Response<ApiResponse<BookingResponse>>

    @POST("bookings/{id}/confirm")
    suspend fun confirmBooking(
        @Path("id") id: String,
        @Body request: PaymentVerifyRequest
    ): Response<ApiResponse<BookingResponse>>

    @POST("bookings/{id}/cancel")
    suspend fun cancelBooking(
        @Path("id") id: String,
        @Body request: CancelRequest
    ): Response<ApiResponse<CancelBookingResponse>>

    @POST("bookings/{id}/complete")
    suspend fun completeBooking(
        @Path("id") id: String
    ): Response<ApiResponse<BookingResponse>>

    @POST("bookings/{id}/dispute")
    suspend fun disputeBooking(
        @Path("id") id: String,
        @Body request: DisputeBookingRequest
    ): Response<ApiResponse<BookingResponse>>

    @Streaming
    @GET("bookings/{id}/invoice")
    suspend fun downloadInvoice(
        @Path("id") id: String
    ): Response<okhttp3.ResponseBody>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @POST("ai/booking/price-suggest")
    suspend fun aiSuggestPrice(
        @Body request: PriceSuggestRequest
    ): Response<ApiResponse<PriceSuggestResponse>>

    @Streaming
    @GET("bookings/statement")
    suspend fun downloadStatement(): Response<okhttp3.ResponseBody>

    @GET("bookings/statement/summary")
    suspend fun getStatementSummary(): Response<ApiResponse<Map<String, String>>>
}
