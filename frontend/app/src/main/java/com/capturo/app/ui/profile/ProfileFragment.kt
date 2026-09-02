package com.capturo.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.capturo.app.R
import com.capturo.app.data.model.response.CreatorStatsResponse
import com.capturo.app.data.model.response.UserResponse
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.databinding.FragmentProfileBinding
import com.capturo.app.ui.auth.LoginActivity
import com.capturo.app.utils.Resource
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.capturo.app.utils.toFullUrl
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSettingsTriggers()
        observeViewModel()
    }

    private fun setupSettingsTriggers() {
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        binding.btnPayouts.setOnClickListener {
            findNavController().navigate(R.id.payoutsFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Setup availability toggle behavior
        binding.switchAvailability.setOnCheckedChangeListener { _, isChecked ->
            val statusMessage = if (isChecked) "You are now Online" else "You are now Offline"
            Toast.makeText(requireContext(), statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        // Observe user profile LiveData
        viewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.textName.text = "Loading..."
                }
                is Resource.Success -> {
                    bindUserProfile(resource.data)
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }

        // Observe Creator Stats LiveData
        viewModel.creatorStats.observe(viewLifecycleOwner) { resource ->
            if (resource is Resource.Success) {
                bindCreatorStats(resource.data)
            }
        }

        // Observe Logout Event LiveData
        viewModel.logoutEvent.observe(viewLifecycleOwner) { loggedOut ->
            if (loggedOut) {
                val intent = Intent(requireActivity(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun bindUserProfile(user: UserResponse) {
        binding.textName.text = user.fullName
        binding.imageAvatar.load(user.profilePicUrl.toFullUrl()) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }

        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        
        // Show role-specific details
        binding.textRoleBadge.text = if (isCreator) "Creator Profile" else "Event Attendee"
        
        if (isCreator) {
            binding.cardStats.visibility = View.VISIBLE
            binding.btnAvailability.visibility = View.VISIBLE
            binding.dividerAvailability.visibility = View.VISIBLE
            binding.layoutOnlineStatus.visibility = View.VISIBLE
            binding.textPayoutsLabel.text = "Payouts"
            binding.textPayoutBalance.visibility = View.VISIBLE
            
            // Set initial switch state based on status
            binding.switchAvailability.isChecked = true
        } else {
            binding.cardStats.visibility = View.GONE
            binding.btnAvailability.visibility = View.GONE
            binding.dividerAvailability.visibility = View.GONE
            binding.layoutOnlineStatus.visibility = View.GONE
            binding.textPayoutsLabel.text = "Payment History"
            binding.textPayoutBalance.visibility = View.GONE
        }
    }

    private fun bindCreatorStats(stats: CreatorStatsResponse) {
        binding.textBookingsCount.text = stats.bookings.toString()
        binding.textRatingValue.text = String.format("%.1f", stats.rating)
        binding.textFollowersCount.text = "2.5k" // Custom fallback static spec follow count
        binding.textPayoutBalance.text = "₹${stats.earningsThisMonth.toInt()}"
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of Capturo?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Log Out") { _, _ ->
                viewModel.logout()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
