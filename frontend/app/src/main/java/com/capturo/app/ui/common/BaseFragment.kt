package com.capturo.app.ui.common

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.capturo.app.R

abstract class BaseFragment<VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    private var _binding: VB? = null
    val binding get() = _binding!!

    private var loadingDialog: LoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideLoading()
        _binding = null
    }

    // Loading Dialog Overlays
    fun showLoading(message: String = "Please wait…") {
        if (loadingDialog == null) {
            loadingDialog = LoadingDialog(requireContext())
        }
        loadingDialog?.show(message)
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    // Error alert banner
    fun showError(message: String, action: String? = null, actionCallback: (() -> Unit)? = null) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(Color.parseColor("#2D1060"))
            setTextColor(Color.WHITE)
            if (action != null && actionCallback != null) {
                setAction(action) { actionCallback() }
                setActionTextColor(Color.parseColor("#E040FB"))
            }
            val bottomCta = binding.root.findViewById<View>(R.id.layoutBottomCta)
            if (bottomCta != null) {
                anchorView = bottomCta
            }
            show()
        }
    }

    // Success alert banner
    fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).apply {
            setBackgroundTint(Color.parseColor("#2D1060"))
            setTextColor(Color.WHITE)
            val bottomCta = binding.root.findViewById<View>(R.id.layoutBottomCta)
            if (bottomCta != null) {
                anchorView = bottomCta
            }
            show()
        }
    }

    // Generic Network Error Dialog
    fun showNetworkError(retryAction: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Connection Error")
            .setMessage("No internet connection detected. Please verify your connection and try again.")
            .setCancelable(false)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Retry") { _, _ -> retryAction() }
            .show()
    }
}
