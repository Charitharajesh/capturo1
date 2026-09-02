package com.capturo.app.ui.creatorDashboard

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capturo.app.domain.usecase.gallery.UploadMediaUseCase
import com.capturo.app.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

data class UploadProgress(
    val uri: Uri,
    val fileName: String,
    val progress: Int,
    val isCompleted: Boolean,
    val isFailed: Boolean
)

@HiltViewModel
class LiveUploadViewModel @Inject constructor(
    private val uploadUseCase: UploadMediaUseCase
) : ViewModel() {

    private val _uploadList = MutableLiveData<List<UploadProgress>>(emptyList())
    val uploadList: LiveData<List<UploadProgress>> = _uploadList

    private val _totalUploadedCount = MutableLiveData(0)
    val totalUploadedCount: LiveData<Int> = _totalUploadedCount

    private val _allCompleted = MutableLiveData<Boolean>(false)
    val allCompleted: LiveData<Boolean> = _allCompleted

    fun uploadFiles(context: Context, bookingId: String, uris: List<Uri>, titleStr: String = "Delivery", descStr: String = "") {
        _allCompleted.value = false
        _totalUploadedCount.value = 0
        
        val initialList = uris.map { uri ->
            UploadProgress(
                uri = uri,
                fileName = getFileName(context, uri),
                progress = 0,
                isCompleted = false,
                isFailed = false
            )
        }
        _uploadList.value = initialList

        viewModelScope.launch {
            initialList.forEach { item ->
                uploadSingleFile(context, bookingId, item, titleStr, descStr)
            }
        }
    }

    private suspend fun uploadSingleFile(context: Context, bookingId: String, item: UploadProgress, titleStr: String, descStr: String) {
        val file = getFileFromUri(context, item.uri) ?: return
        
        val filePart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )
        
        val isRealBooking = bookingId.isNotEmpty() && bookingId != "booking_mock_id"
        
        val bookingIdPart = if (isRealBooking) {
            bookingId.toRequestBody("text/plain".toMediaTypeOrNull())
        } else {
            null
        }
        
        val isClientDeliveryPart = if (isRealBooking) {
            "true".toRequestBody("text/plain".toMediaTypeOrNull())
        } else {
            "false".toRequestBody("text/plain".toMediaTypeOrNull())
        }

        val isPortfolioPart = if (isRealBooking) {
            "false".toRequestBody("text/plain".toMediaTypeOrNull())
        } else {
            "true".toRequestBody("text/plain".toMediaTypeOrNull())
        }

        val titlePart = titleStr.ifEmpty { file.name }.toRequestBody("text/plain".toMediaTypeOrNull())
        val descPart = descStr.ifEmpty { "Uploaded media" }.toRequestBody("text/plain".toMediaTypeOrNull())

        // Simulate progressive upload progress callback flow alongside repository flow
        viewModelScope.launch {
            for (p in 10..90 step 15) {
                delay(150)
                updateItemProgress(item.uri, p)
            }
        }

        uploadUseCase(filePart, titlePart, descPart, bookingIdPart, isPortfolioPart, isClientDeliveryPart).collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    updateItemComplete(item.uri, true)
                    _totalUploadedCount.value = (_totalUploadedCount.value ?: 0) + 1
                    checkAllCompleted()
                }
                is Resource.Error -> {
                    updateItemComplete(item.uri, false, isFailed = true)
                    checkAllCompleted()
                }
                is Resource.Loading -> {}
            }
        }
    }

    private fun updateItemProgress(uri: Uri, progress: Int) {
        val current = _uploadList.value.orEmpty().toMutableList()
        val index = current.indexOfFirst { it.uri == uri }
        if (index != -1) {
            current[index] = current[index].copy(progress = progress)
            _uploadList.postValue(current)
        }
    }

    private fun updateItemComplete(uri: Uri, isSuccess: Boolean, isFailed: Boolean = false) {
        val current = _uploadList.value.orEmpty().toMutableList()
        val index = current.indexOfFirst { it.uri == uri }
        if (index != -1) {
            current[index] = current[index].copy(
                progress = if (isSuccess) 100 else current[index].progress,
                isCompleted = isSuccess,
                isFailed = isFailed
            )
            _uploadList.postValue(current)
        }
    }

    private fun checkAllCompleted() {
        val current = _uploadList.value.orEmpty()
        val allDone = current.all { it.isCompleted || it.isFailed }
        if (allDone && current.isNotEmpty()) {
            _allCompleted.postValue(true)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "file"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = File(context.cacheDir, getFileName(context, uri))
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
