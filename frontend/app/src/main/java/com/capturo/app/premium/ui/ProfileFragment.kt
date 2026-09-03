package com.capturo.app.premium.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.capturo.app.R
import com.capturo.app.databinding.FragmentPremiumProfileBinding
import com.capturo.app.premium.PremiumAuthActivity
import com.capturo.app.premium.PremiumConversationsActivity
import com.capturo.app.premium.PremiumDashboardActivity
import com.capturo.app.premium.PremiumInfoActivity
import com.capturo.app.premium.PremiumMainActivity
import com.capturo.app.premium.PremiumPaymentsActivity
import com.capturo.app.premium.PremiumRegisterActivity
import com.capturo.app.premium.PremiumSavedActivity
import com.capturo.app.premium.PremiumStore

class ProfileFragment : Fragment() {

    private var _binding: FragmentPremiumProfileBinding? = null
    private val binding get() = _binding!!

    private val menuItems = listOf(
        "My Bookings", "Favorites", "Messages", "Payments",
        "Notifications", "Help & Support", "Privacy", "Terms"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PremiumStore.currentAccount(requireContext())?.let { acc ->
            binding.textProfileName.text = acc.name
            binding.textProfileEmail.text = acc.email
        }

        menuItems.forEach { binding.menuContainer.addView(buildRow(it)) }

        binding.btnSwitchMode.setOnClickListener {
            // First time → register as a photographer; afterwards → dashboard.
            val target = if (PremiumStore.myPhotographer(requireContext()) == null)
                PremiumRegisterActivity::class.java else PremiumDashboardActivity::class.java
            startActivity(Intent(requireContext(), target))
        }
        binding.btnLogout.setOnClickListener {
            PremiumStore.logout(requireContext())
            startActivity(Intent(requireContext(), PremiumAuthActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun onMenuClick(label: String) {
        val ctx = requireContext()
        when (label) {
            "My Bookings" -> startActivity(
                Intent(ctx, PremiumMainActivity::class.java)
                    .putExtra(PremiumMainActivity.EXTRA_OPEN_TAB, "bookings")
            )
            "Favorites" -> startActivity(Intent(ctx, PremiumSavedActivity::class.java))
            "Messages" -> startActivity(Intent(ctx, PremiumConversationsActivity::class.java))
            "Payments" -> startActivity(Intent(ctx, PremiumPaymentsActivity::class.java))
            else -> startActivity(
                Intent(ctx, PremiumInfoActivity::class.java)
                    .putExtra(PremiumInfoActivity.EXTRA_TITLE, label)
            )
        }
    }

    private fun buildRow(label: String): View {
        val ctx = requireContext()
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val row = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            isClickable = true
            isFocusable = true
            setOnClickListener { onMenuClick(label) }
        }
        val text = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            this.text = label
            setTextColor(ContextCompat.getColor(ctx, R.color.colorTextPrimary))
            textSize = 14f
        }
        val arrow = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
            setImageResource(R.drawable.ic_arrow_forward)
            setColorFilter(ContextCompat.getColor(ctx, R.color.colorTextHint))
        }
        row.addView(text)
        row.addView(arrow)
        return row
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
