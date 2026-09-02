package com.capturo.app.data.model.response

import com.capturo.app.utils.Resource
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.Response

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)

data class PaginatedResponse<T>(
    @SerializedName("items") val items: List<T>,
    @SerializedName("total") val total: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_prev") val hasPrev: Boolean
)

data class ErrorResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("message") val message: String
)

fun <T> Response<ApiResponse<T>>.toResource(): Resource<T> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null && body.success) {
                if (body.data != null) {
                    Resource.Success(body.data)
                } else {
                    Resource.Error(body.message)
                }
            } else {
                Resource.Error(body?.message ?: "Unsuccessful API call")
            }
        } else {
            val errorBodyString = errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBodyString, ErrorResponse::class.java)
            Resource.Error(errorResponse?.message ?: "HTTP Error: ${code()}")
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network or parsing exception", e)
    }
}

fun <T> Response<PaginatedResponse<T>>.toPaginatedResource(): Resource<PaginatedResponse<T>> {
    return try {
        if (isSuccessful) {
            val body = body()
            if (body != null) {
                Resource.Success(body)
            } else {
                Resource.Error("Empty paginated response")
            }
        } else {
            val errorBodyString = errorBody()?.string()
            val errorResponse = Gson().fromJson(errorBodyString, ErrorResponse::class.java)
            Resource.Error(errorResponse?.message ?: "HTTP Error: ${code()}")
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network or parsing exception", e)
    }
}
