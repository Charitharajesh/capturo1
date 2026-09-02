package com.capturo.app.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.capturo.app.BuildConfig
import com.capturo.app.R
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.databinding.FragmentPaymentBinding
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PaymentFragment : Fragment(), PaymentResultWithDataListener {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    private var bookingId: String = ""
    private var orderId: String = ""
    private var keyId: String = ""
    private var amount: Double = 0.0
    private var creatorName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Preload Razorpay Checkout for performance
        Checkout.preload(requireActivity().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve arguments
        bookingId = arguments?.getString("booking_id") ?: ""
        orderId = arguments?.getString("order_id") ?: ""
        keyId = arguments?.getString("key_id") ?: ""
        amount = arguments?.getFloat("amount", 0f)?.toDouble() ?: 0.0
        creatorName = arguments?.getString("creator_name") ?: "Creator"

        if (bookingId.isEmpty() || orderId.isEmpty()) {
            Snackbar.make(binding.root, "Error: Invalid checkout details", Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
            return
        }

        // Start Checkout flow
        startRazorpayCheckout()

        // Observe ViewModel verification responses
        observeViewModel()
    }

    private fun startRazorpayCheckout() {
        val checkout = Checkout()
        
        // Use BuildConfig key ID if populated, otherwise fallback to dynamic key ID from API
        val finalKeyId = if (BuildConfig.DEBUG && keyId.isNotEmpty()) {
            keyId
        } else {
            try {
                // Read from generated BuildConfig constant if present
                BuildConfig::class.java.getField("RAZORPAY_KEY_ID").get(null) as String
            } catch (e: Exception) {
                // Muted fallback
                keyId.ifEmpty { "rzp_test_SvtW8YTEDKQlBp" }
            }
        }
        
        checkout.setKeyID(finalKeyId)

        try {
            val options = JSONObject().apply {
                put("name", "Capturo")
                put("description", "Booking reservation for $creatorName")
                put("theme.color", "#7B2FBE")
                put("currency", "INR")
                put("order_id", orderId)
                put("amount", (amount * 100).toInt()) // amount in paise (1 INR = 100 paise)
                
                // Prefill user details
                val prefill = JSONObject().apply {
                    put("email", sessionManager.getAccessToken() ?: "user@example.com")
                    put("contact", "9876543210")
                }
                put("prefill", prefill)
            }

            checkout.open(requireActivity(), options)
        } catch (e: Exception) {
            Timber.e(e, "Error launching Razorpay checkout")
            Snackbar.make(binding.root, "Error launching payment portal: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        Timber.d("Payment succeeded: $razorpayPaymentId")
        binding.textPaymentStatus.text = "Verifying Transaction..."
        
        val signature = paymentData?.signature ?: ""
        viewModel.verifyAndConfirmPayment(bookingId, razorpayPaymentId ?: "", signature)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Timber.e("Payment failed with code $code: $response")
        Snackbar.make(binding.root, "Payment failed: $response", Snackbar.LENGTH_LONG).show()
        
        // Return to booking detail fragment
        val bundle = Bundle().apply {
            putString("booking_id", bookingId)
        }
        findNavController().navigate(R.id.bookingDetailFragment, bundle)
    }

    private fun observeViewModel() {
        viewModel.paymentConfirmationState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.textPaymentStatus.text = "Securing transaction..."
                }
                is Resource.Success -> {
                    Snackbar.make(binding.root, "Payment completed and booking confirmed!", Snackbar.LENGTH_LONG).show()
                    
                    // Route to booking detail screen
                    val bundle = Bundle().apply {
                        putString("booking_id", bookingId)
                    }
                    findNavController().navigate(R.id.bookingDetailFragment, bundle)
                }
                is Resource.Error -> {
                    Snackbar.make(binding.root, resource.message, Snackbar.LENGTH_LONG).show()
                    
                    // Route back on failure to detail review
                    val bundle = Bundle().apply {
                        putString("booking_id", bookingId)
                    }
                    findNavController().navigate(R.id.bookingDetailFragment, bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
