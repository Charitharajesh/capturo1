package com.capturo.app.ui.creatorDashboard

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.capturo.app.R
import com.capturo.app.databinding.DialogUploadGalleryBinding
import com.capturo.app.domain.usecase.gallery.UploadMediaUseCase
import com.capturo.app.utils.Resource
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class UploadGalleryDialogFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var uploadUseCase: UploadMediaUseCase

    private var _binding: DialogUploadGalleryBinding? = null
    private val binding get() = _binding!!

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    
    private var bookingId: String? = null
    private var isClientDelivery: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookingId = arguments?.getString("booking_id")
        isClientDelivery = arguments?.getBoolean("is_client_delivery", false) ?: false
    }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedFileUri = uri
            selectedFileName = getFileName(requireContext(), uri)
            binding.tvSelectedFile.text = selectedFileName
            validateForm()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogUploadGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etTitle.doOnTextChanged { _, _, _, _ -> validateForm() }
        binding.etDescription.doOnTextChanged { _, _, _, _ -> validateForm() }

        binding.btnSelectFile.setOnClickListener {
            pickMedia.launch("image/*") // could be "video/*" or "*/*"
        }

        binding.btnUpload.setOnClickListener {
            uploadToGallery()
        }
    }

    private fun validateForm() {
        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDescription.text.toString().trim()
        binding.btnUpload.isEnabled = title.isNotEmpty() && desc.isNotEmpty() && selectedFileUri != null
    }

    private fun uploadToGallery() {
        val titleStr = binding.etTitle.text.toString().trim()
        val descStr = binding.etDescription.text.toString().trim()
        val uri = selectedFileUri ?: return

        binding.btnUpload.text = "Uploading..."
        binding.btnUpload.isEnabled = false
        binding.layoutUploadProgress.visibility = View.VISIBLE
        binding.progressBarHorizontal.progress = 0
        binding.tvProgressPercentage.text = "Uploading... 0%"

        val file = getFileFromUri(requireContext(), uri) ?: return

        val filePart = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )
        
        val titlePart = titleStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val descPart = descStr.toRequestBody("text/plain".toMediaTypeOrNull())
        val bookingIdPart = bookingId?.toRequestBody("text/plain".toMediaTypeOrNull())
        val isPortfolioPart = (if (bookingId != null) "false" else "true").toRequestBody("text/plain".toMediaTypeOrNull())
        val isClientDeliveryPart = (if (isClientDelivery) "true" else "false").toRequestBody("text/plain".toMediaTypeOrNull())

        // Start progressive animation coroutine
        val progressJob = lifecycleScope.launch {
            var progress = 0
            while (progress < 95) {
                kotlinx.coroutines.delay(100)
                progress += (3..12).random()
                if (progress > 95) progress = 95
                binding.progressBarHorizontal.progress = progress
                binding.tvProgressPercentage.text = "Uploading... $progress%"
            }
        }

        lifecycleScope.launch {
            uploadUseCase(filePart, titlePart, descPart, bookingIdPart, isPortfolioPart, isClientDeliveryPart).collectLatest { resource ->
                when (resource) {
                    is Resource.Success -> {
                        progressJob.cancel()
                        binding.progressBarHorizontal.progress = 100
                        binding.tvProgressPercentage.text = "Uploading... 100%"
                        kotlinx.coroutines.delay(200)
                        
                        Toast.makeText(context, "Upload successful!", Toast.LENGTH_SHORT).show()
                        (parentFragment as? CreatorGalleryFragment)?.loadPortfolio()
                        dismiss()
                    }
                    is Resource.Error -> {
                        progressJob.cancel()
                        Toast.makeText(context, resource.message, Toast.LENGTH_LONG).show()
                        binding.btnUpload.text = "Upload"
                        binding.layoutUploadProgress.visibility = View.GONE
                        validateForm() // Re-enable
                    }
                    is Resource.Loading -> {}
                }
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(bookingId: String? = null, isClientDelivery: Boolean = false): UploadGalleryDialogFragment {
            return UploadGalleryDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("booking_id", bookingId)
                    putBoolean("is_client_delivery", isClientDelivery)
                }
            }
        }
    }
}
