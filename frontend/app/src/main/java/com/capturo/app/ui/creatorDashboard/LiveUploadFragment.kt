package com.capturo.app.ui.creatorDashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.lifecycleScope
import com.capturo.app.R
import com.capturo.app.adapter.UploadProgressAdapter
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.databinding.FragmentLiveUploadBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class LiveUploadFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var _binding: FragmentLiveUploadBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveUploadViewModel by viewModels()
    private lateinit var progressAdapter: UploadProgressAdapter
    private var bookingId: String = ""

    // ActivityResultLauncher for multi-media picking
    private val pickMedia = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (!uris.isNullOrEmpty()) {
            binding.tvProgressTitle.visibility = View.VISIBLE
            binding.recyclerProgress.visibility = View.VISIBLE
            binding.cardUploadStats.visibility = View.VISIBLE
            viewModel.uploadFiles(requireContext(), bookingId, uris)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ROLE GATE: Creator check
        if (sessionManager.getUserRole() != UserRole.CREATOR) {
            findNavController().navigate(R.id.homeFragment)
            return
        }

        // Retrieve Booking ID arguments
        bookingId = arguments?.getString("booking_id") ?: ""
        if (bookingId.isEmpty()) {
            bookingId = "booking_mock_id" // fallback default safety anchor
        }

        setupToolbar()
        setupRecyclerView()
        startBlinkingAnimation()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        progressAdapter = UploadProgressAdapter()
        binding.recyclerProgress.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = progressAdapter
        }
    }

    private fun setupButtons() {
        binding.btnPickMedia.setOnClickListener {
            // Launches picker to select images/videos
            pickMedia.launch("image/*")
        }
    }

    private fun observeViewModel() {
        viewModel.uploadList.observe(viewLifecycleOwner) { list ->
            progressAdapter.submitList(list)
            
            val total = list.size
            viewModel.totalUploadedCount.observe(viewLifecycleOwner) { uploaded ->
                binding.tvUploadedCounter.text = "$uploaded / $total"
                if (uploaded == total && total > 0) {
                    binding.tvUploadStatusLabel.text = "Completed"
                    binding.tvUploadStatusLabel.setTextColor(resources.getColor(R.color.colorSuccess, null))
                } else {
                    binding.tvUploadStatusLabel.text = "Uploading..."
                    binding.tvUploadStatusLabel.setTextColor(resources.getColor(R.color.colorAccent, null))
                }
            }
        }

        viewModel.allCompleted.observe(viewLifecycleOwner) { isCompleted ->
            if (isCompleted) {
                showSnackbar("All files successfully uploaded to delivery gallery!")
                viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                    kotlinx.coroutines.delay(1000)
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun startBlinkingAnimation() {
        val anim = AlphaAnimation(1.0f, 0.3f).apply {
            duration = 800
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
        }
        binding.viewLiveDot.startAnimation(anim)
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(resources.getColor(R.color.colorSurfaceElevated, null))
            setTextColor(resources.getColor(R.color.colorTextPrimary, null))
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
