package com.capturo.app.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.capturo.app.databinding.DialogLoadingBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoadingDialog(private val context: Context) {

    private var dialog: AlertDialog? = null

    fun show(message: String = "Please wait…") {
        if (dialog == null) {
            val binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
            
            dialog = MaterialAlertDialogBuilder(context)
                .setView(binding.root)
                .setCancelable(false)
                .create()
                
            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
