package com.capturo.app.ui.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {
    private val _currentPage = MutableLiveData<Int>(0)
    val currentPage: LiveData<Int> = _currentPage

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }
}
