package com.capturo.app.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.capturo.app.databinding.FragmentOnboardingPageBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingPageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val title = it.getString(ARG_TITLE, "")
            val desc = it.getString(ARG_DESC, "")
            val imageRes = it.getInt(ARG_IMAGE_RES, 0)

            binding.slideTitle.text = title
            binding.slideDesc.text = desc
            if (imageRes != 0) {
                binding.slideGraphic.setImageResource(imageRes)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"
        private const val ARG_IMAGE_RES = "image_res"

        fun newInstance(title: String, desc: String, imageRes: Int): OnboardingFragment {
            return OnboardingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_DESC, desc)
                    putInt(ARG_IMAGE_RES, imageRes)
                }
            }
        }
    }
}
