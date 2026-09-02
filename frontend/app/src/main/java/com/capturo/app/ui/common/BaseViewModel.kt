package com.capturo.app.ui.common

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseViewModel : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    protected fun showLoading() {
        _isLoading.value = true
    }

    protected fun hideLoading() {
        _isLoading.value = false
    }

    protected fun publishError(message: String) {
        _errorMessage.value = message
    }

    protected fun publishSuccess(message: String) {
        _successMessage.value = message
    }

    fun clearAlerts() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    // Coroutine scope execution wrapper that automatically manages loading flags and alert publishers
    protected fun launchWithLoading(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            showLoading()
            try {
                block()
            } catch (e: Exception) {
                publishError(e.localizedMessage ?: "An unexpected error occurred")
            } finally {
                hideLoading()
            }
        }
    }
}
