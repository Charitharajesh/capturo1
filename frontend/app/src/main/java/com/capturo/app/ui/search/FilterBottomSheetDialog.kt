package com.capturo.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.capturo.app.databinding.DialogFilterCreatorsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FilterBottomSheetDialog(
    private val initialParams: FilterParams,
    private val onFiltersApplied: (FilterParams) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogFilterCreatorsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFilterCreatorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup initial UI states
        setupInitialStates()

        // Price Slider change listener
        binding.sliderPrice.addOnChangeListener { _, value, _ ->
            binding.textPriceRangeValue.text = "₹${value.toInt()}"
        }

        // Clear button
        binding.buttonClear.setOnClickListener {
            onFiltersApplied(FilterParams())
            dismiss()
        }

        // Apply button
        binding.buttonApply.setOnClickListener {
            val selectedSpecId = binding.chipGroupSpec.checkedChipId
            val specialization = when (selectedSpecId) {
                binding.chipSpecWedding.id -> "Wedding"
                binding.chipSpecPortrait.id -> "Portrait"
                binding.chipSpecCorporate.id -> "Corporate"
                binding.chipSpecEvents.id -> "Events"
                else -> null
            }

            val maxRate = binding.sliderPrice.value.toDouble()

            val selectedRatingId = binding.chipGroupRating.checkedChipId
            val minRating = when (selectedRatingId) {
                binding.chipRating4.id -> 4.0
                binding.chipRating45.id -> 4.5
                binding.chipRating48.id -> 4.8
                else -> null
            }

            onFiltersApplied(
                FilterParams(
                    specialization = specialization,
                    minRating = minRating,
                    maxRate = maxRate
                )
            )
            dismiss()
        }
    }

    private fun setupInitialStates() {
        // Specialization
        when (initialParams.specialization?.lowercase()) {
            "wedding" -> binding.chipSpecWedding.isChecked = true
            "portrait" -> binding.chipSpecPortrait.isChecked = true
            "corporate" -> binding.chipSpecCorporate.isChecked = true
            "events" -> binding.chipSpecEvents.isChecked = true
            else -> binding.chipSpecAll.isChecked = true
        }

        // Price
        val price = initialParams.maxRate ?: 5000.0
        binding.sliderPrice.value = price.toFloat().coerceIn(500f, 10000f)
        binding.textPriceRangeValue.text = "₹${price.toInt()}"

        // Rating
        when (initialParams.minRating) {
            4.0 -> binding.chipRating4.isChecked = true
            4.5 -> binding.chipRating45.isChecked = true
            4.8 -> binding.chipRating48.isChecked = true
            else -> binding.chipRatingAny.isChecked = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
