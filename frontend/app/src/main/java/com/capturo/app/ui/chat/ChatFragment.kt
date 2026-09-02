package com.capturo.app.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.R
import com.capturo.app.adapter.ChatAdapter
import com.capturo.app.data.preferences.SessionManager
import com.capturo.app.databinding.FragmentChatBinding
import com.capturo.app.utils.Resource
import com.capturo.app.utils.toFullUrl
import com.capturo.app.utils.showSnackbar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat
import coil.load
import coil.transform.CircleCropTransformation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    private var bookingId: String = ""
    private var receiverId: String = ""
    private var currentUserId: String = ""

    private var pollingJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve arguments
        bookingId = arguments?.getString("booking_id") ?: ""
        receiverId = arguments?.getString("receiver_id") ?: ""
        val receiverName = arguments?.getString("receiver_name") ?: "Photographer"
        val receiverPic = arguments?.getString("receiver_pic")
        currentUserId = sessionManager.getUserId() ?: ""

        if (bookingId.isEmpty() || currentUserId.isEmpty()) {
            showSnackbar("You must book this creator first to send messages.", Snackbar.LENGTH_LONG)
            findNavController().navigateUp()
            return
        }

        // Bind receiver name & avatar
        binding.textReceiverName.text = receiverName
        binding.imageReceiverAvatar.load(receiverPic.toFullUrl()) {
            crossfade(true)
            transformations(CircleCropTransformation())
            placeholder(R.drawable.ic_profile)
            error(R.drawable.ic_profile)
        }

        // Setup Toolbar back button
        binding.buttonBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Setup RecyclerView
        chatAdapter = ChatAdapter(currentUserId)
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = false
        }
        binding.recyclerMessages.layoutManager = layoutManager
        binding.recyclerMessages.adapter = chatAdapter

        // Setup Send button
        binding.buttonSend.setOnClickListener {
            val content = binding.editMessageInput.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.sendMessage(bookingId, receiverId, content)
                binding.editMessageInput.text?.clear()
            }
        }

        // Collect messages Flow and states in lifecycleScope
        collectMessagesAndStates()
        observeAiSuggestions()
    }

    private fun observeAiSuggestions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.replySuggestions.collectLatest { resource ->
                    if (resource is Resource.Success) {
                        val suggestions = resource.data.suggestions
                        if (suggestions.isNotEmpty()) {
                            binding.chipGroupSmartReplies.removeAllViews()
                            suggestions.forEach { reply ->
                                val chip = Chip(requireContext()).apply {
                                    text = reply
                                    isClickable = true
                                    isCheckable = false
                                    setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextPrimary))
                                    setChipBackgroundColorResource(R.color.colorPrimaryContainer)
                                    chipStrokeColor = ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
                                    chipStrokeWidth = resources.displayMetrics.density * 1.0f
                                    
                                    setOnClickListener {
                                        binding.editMessageInput.setText(reply)
                                        binding.editMessageInput.setSelection(reply.length)
                                    }
                                }
                                binding.chipGroupSmartReplies.addView(chip)
                            }
                            binding.scrollReplies.visibility = View.VISIBLE
                        } else {
                            binding.scrollReplies.visibility = View.GONE
                        }
                    } else {
                        binding.scrollReplies.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun collectMessagesAndStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 1. Collect messages flow from database
                launch {
                    viewModel.getMessageFlow(bookingId).collectLatest { messages ->
                        val prevSize = chatAdapter.itemCount
                        chatAdapter.submitList(messages) {
                            if (messages.size > prevSize) {
                                binding.recyclerMessages.smoothScrollToPosition(messages.size - 1)
                            }
                        }

                        // Automatically fetch smart replies if the last message is from the other user!
                        val lastMsg = messages.lastOrNull()
                        if (lastMsg != null && lastMsg.senderId != currentUserId) {
                            val role = if (sessionManager.getUserRole() == com.capturo.app.data.preferences.UserRole.CREATOR) "creator" else "attendee"
                            viewModel.fetchAiSuggestions(
                                lastMessage = lastMsg.content,
                                senderRole = if (role == "creator") "attendee" else "creator",
                                contextText = "Active photography booking discussion"
                            )
                        } else {
                            binding.scrollReplies.visibility = View.GONE
                        }
                    }
                }

                // 2. Collect sending state
                launch {
                    viewModel.sendState.collectLatest { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                binding.buttonSend.isEnabled = false
                            }
                            is Resource.Success -> {
                                binding.buttonSend.isEnabled = true
                                viewModel.resetSendState()
                                viewModel.syncChat(bookingId) // Force sync immediately
                            }
                            is Resource.Error -> {
                                binding.buttonSend.isEnabled = true
                                viewModel.resetSendState()
                                showSnackbar(resource.message, Snackbar.LENGTH_SHORT)
                            }
                            null -> {
                                binding.buttonSend.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startChatPolling() {
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                try {
                    viewModel.syncChat(bookingId)
                } catch (e: Exception) {
                    Timber.e(e, "Error syncing chat details")
                }
                delay(5000) // Poll remote history every 5 seconds
            }
        }
    }

    private fun stopChatPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onResume() {
        super.onResume()
        startChatPolling()
    }

    override fun onPause() {
        super.onPause()
        stopChatPolling()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
