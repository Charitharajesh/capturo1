package com.capturo.app.utils

sealed class Resource<out T> {
    
    data class Success<out T>(val data: T) : Resource<T>()
    
    data class Error<out T>(
        val message: String,
        val exception: Throwable? = null,
        val data: T? = null
    ) : Resource<T>()
    
    object Loading : Resource<Nothing>()

    companion object {
        fun <T> success(data: T): Resource<T> = Success(data)
        
        fun <T> error(
            message: String,
            exception: Throwable? = null,
            data: T? = null
        ): Resource<T> = Error(message, exception, data)
        
        fun loading(): Resource<Nothing> = Loading
    }
}

// Extension function for fluent Success observers in Fragments
inline fun <T> Resource<T>.onSuccess(action: (T) -> Unit): Resource<T> {
    if (this is Resource.Success) {
        action(data)
    }
    return this
}

// Extension function for fluent Error observers in Fragments
inline fun <T> Resource<T>.onError(action: (String) -> Unit): Resource<T> {
    if (this is Resource.Error) {
        action(message)
    }
    return this
}
