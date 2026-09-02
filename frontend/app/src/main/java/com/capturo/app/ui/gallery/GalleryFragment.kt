package com.capturo.app.ui.gallery

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import coil.load
import com.capturo.app.R
import com.capturo.app.adapter.GalleryAdapter
import com.capturo.app.data.model.response.GalleryResponse
import com.capturo.app.databinding.FragmentGalleryBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.hide
import com.capturo.app.utils.show
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import com.capturo.app.utils.toFullUrl
import java.util.Locale

@AndroidEntryPoint
class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var galleryAdapter: GalleryAdapter
    private var bookingId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve Booking ID from arguments
        bookingId = arguments?.getString("booking_id") ?: ""
        if (bookingId.isEmpty()) {
            Snackbar.make(binding.root, "Error: Invalid Booking Selection", Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        setupToolbar()
        setupRecyclerView()
        observeViewModel()

        // Fetch delivered items
        viewModel.loadDeliveryGallery(bookingId)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        galleryAdapter = GalleryAdapter { item ->
            showFullscreenImage(item)
        }
        binding.recyclerGallery.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = galleryAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewModel.deliveryState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.show()
                    binding.layoutEmpty.hide()
                }
                is Resource.Success -> {
                    binding.progressBar.hide()
                    val response = resource.data
                    if (response != null && response.items.isNotEmpty()) {
                        binding.layoutEmpty.hide()
                        binding.recyclerGallery.show()
                        
                        // Set file count and aggregate size
                        binding.textMediaCount.text = String.format(Locale.ENGLISH, "%d files", response.total)
                        binding.textMediaSize.text = String.format(Locale.ENGLISH, "%.2f GB", response.totalSizeGb)
                        
                        galleryAdapter.submitList(response.items)
                    } else {
                        showEmptyState()
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.hide()
                    showEmptyState()
                    Snackbar.make(binding.root, resource.message ?: "Failed to load delivered media", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.layoutEmpty.show()
        binding.recyclerGallery.hide()
        binding.textMediaCount.text = "0 files"
        binding.textMediaSize.text = "0.00 GB"
        galleryAdapter.submitList(emptyList())
    }

    private fun showFullscreenImage(item: GalleryResponse) {
        val context = requireContext()
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.dialog_confirm_cancel) // Standard layout container (we will dynamically adjust dialog elements)
        
        // Let's create a beautiful custom full-screen dialog programmatically or with a custom layout
        val fullscreenDialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(true)
            
            // Programmatically build a premium view hierarchy
            val container = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val imageView = ImageView(context).apply {
                layoutParams = container
                scaleType = ImageView.ScaleType.FIT_CENTER
                setBackgroundColor(0xFF0D0020.toInt()) // Premium deepest near-black violet bg
                load(item.fileUrl.toFullUrl()) {
                    crossfade(true)
                    placeholder(R.color.colorSurfaceVariant)
                    error(R.color.colorSurfaceVariant)
                }
                setOnClickListener {
                    dismiss()
                }
            }
            setContentView(imageView)
        }
        fullscreenDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
