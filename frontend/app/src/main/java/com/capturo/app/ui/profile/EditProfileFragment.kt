package com.capturo.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.capturo.app.R
import com.capturo.app.data.model.request.UpdateCreatorProfileRequest
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.data.preferences.UserRole
import com.capturo.app.data.repository.CreatorRepository
import com.capturo.app.data.repository.UserRepository
import com.capturo.app.databinding.FragmentEditProfileBinding
import com.capturo.app.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var creatorRepository: CreatorRepository

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        loadInitialData()
        setupLivePreview()

        binding.btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupLivePreview() {
        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        if (!isCreator) return

        binding.layoutLivePreviewContainer.visibility = View.VISIBLE

        val updatePreview = {
            val name = binding.etName.text.toString().ifEmpty { "Your Name" }
            val rate = binding.etHourlyRate.text.toString().ifEmpty { "0" }
            
            var specialty = "Photographer"
            if (binding.chipWedding.isChecked) specialty = "Wedding Photographer"
            else if (binding.chipPortrait.isChecked) specialty = "Portrait Photographer"
            else if (binding.chipCorporate.isChecked) specialty = "Corporate Photographer"
            else if (binding.chipSports.isChecked) specialty = "Sports Photographer"
            else if (binding.chipEvents.isChecked) specialty = "Events Photographer"

            binding.livePreviewCard.textName.text = name
            binding.livePreviewCard.textPrice.text = "₹${rate}/hr"
            binding.livePreviewCard.textSpecialty.text = specialty
            binding.livePreviewCard.textRating.text = "5.0"
            binding.livePreviewCard.textReviewsCount.text = "(New)"
            binding.livePreviewCard.textDistance.text = "0.0 km"
            binding.livePreviewCard.textAvailability.text = "Available Now"
        }

        binding.etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etHourlyRate.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { updatePreview() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ChipGroup setOnCheckedStateChangeListener is available in MaterialComponents
        binding.cgSpecializations.setOnCheckedChangeListener { _, _ -> updatePreview() }
        
        // Initial setup will happen when data loads, but call once just in case
        updatePreview()
    }

    private fun loadInitialData() {
        val isCreator = sessionManager.getUserRole() == UserRole.CREATOR
        binding.layoutCreatorDetails.visibility = if (isCreator) View.VISIBLE else View.GONE
        binding.tilBio.visibility = if (isCreator) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            userRepository.getProfile().collectLatest { resource ->
                if (resource is Resource.Success) {
                    val user = resource.data
                    binding.etName.setText(user.fullName)
                    binding.etPhone.setText(user.phone ?: "")
                }
            }
        }

        if (isCreator) {
            lifecycleScope.launch {
                creatorRepository.getOwnCreatorProfile().collectLatest { resource ->
                    if (resource is Resource.Success) {
                        val creator = resource.data
                        binding.etBio.setText(creator.bio ?: "")
                        binding.etHourlyRate.setText(creator.hourlyRate.toString())
                        binding.etMinHours.setText(creator.minimumHours.toString())
                        binding.etExperience.setText(creator.yearsExperience?.toString() ?: "")
                        binding.etRadius.setText(creator.serviceRadiusKm.toString())

                        // Select chips
                        creator.specializations.forEach { spec ->
                            when (spec.lowercase()) {
                                "wedding" -> binding.chipWedding.isChecked = true
                                "portrait" -> binding.chipPortrait.isChecked = true
                                "corporate" -> binding.chipCorporate.isChecked = true
                                "sports" -> binding.chipSports.isChecked = true
                                "events" -> binding.chipEvents.isChecked = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun saveChanges() {
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            return
        }

        binding.progressSaving.visibility = View.VISIBLE
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            userRepository.updateProfile(name, if (phone.isEmpty()) null else phone, null).collectLatest { userRes ->
                if (userRes is Resource.Success) {
                    if (sessionManager.getUserRole() == UserRole.CREATOR) {
                        saveCreatorProfile()
                    } else {
                        binding.progressSaving.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                } else if (userRes is Resource.Error) {
                    binding.progressSaving.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), userRes.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun saveCreatorProfile() {
        val bio = binding.etBio.text.toString().trim()
        val hourlyRate = binding.etHourlyRate.text.toString().toDoubleOrNull() ?: 0.0
        val minHours = binding.etMinHours.text.toString().toIntOrNull() ?: 2
        val experience = binding.etExperience.text.toString().toIntOrNull()
        val radius = binding.etRadius.text.toString().toIntOrNull() ?: 10

        val specs = mutableListOf<String>()
        if (binding.chipWedding.isChecked) specs.add("Wedding")
        if (binding.chipPortrait.isChecked) specs.add("Portrait")
        if (binding.chipCorporate.isChecked) specs.add("Corporate")
        if (binding.chipSports.isChecked) specs.add("Sports")
        if (binding.chipEvents.isChecked) specs.add("Events")

        val request = UpdateCreatorProfileRequest(
            specializations = specs,
            hourlyRate = hourlyRate,
            minimumHours = minHours,
            yearsExperience = experience,
            serviceRadiusKm = radius,
            bio = if (bio.isEmpty()) null else bio
        )

        creatorRepository.updateOwnCreatorProfile(request).collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    binding.progressSaving.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), "Creator profile updated", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                is Resource.Error -> {
                    binding.progressSaving.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_LONG).show()
                }
                is Resource.Loading -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
