package com.capturo.app.ui.notifications

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.R
import com.capturo.app.adapter.NotificationAdapter
import com.capturo.app.adapter.NotificationListItem
import com.capturo.app.data.model.response.NotificationResponse
import com.capturo.app.databinding.FragmentNotificationsBinding
import com.capturo.app.ui.common.BaseFragment
import com.capturo.app.utils.Resource
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationsViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSwipeToRefresh()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.inflateMenu(R.menu.menu_notifications)
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_mark_all_read) {
                viewModel.markAllRead()
                true
            } else {
                false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter { notification ->
            handleNotificationTap(notification)
        }

        binding.recyclerNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NotificationsFragment.adapter
        }

        setupSwipeToDelete()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setColorSchemeColors(Color.parseColor("#E040FB"))
        binding.swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#240A4A"))
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadNotifications()
        }
    }

    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder is NotificationAdapter.HeaderViewHolder) return 0
                return super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentList[position]
                if (item is NotificationListItem.Item) {
                    val notificationId = item.notification.id
                    viewModel.deleteNotification(notificationId)
                    showSnackbar("Notification deleted", "Undo") {
                        // Undo deletion if needed (not supported by API currently, but provides premium feedback)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                if (viewHolder is NotificationAdapter.ItemViewHolder && dX > 0) {
                    val itemView = viewHolder.itemView
                    val background = ColorDrawable(Color.parseColor("#FF5252"))
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    background.draw(c)

                    val icon = ContextCompat.getDrawable(recyclerView.context, R.drawable.ic_delete)
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + it.intrinsicWidth
                        
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                        it.draw(c)
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerNotifications)
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                    binding.layoutEmpty.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    val list = resource.data
                    adapter.submitList(list)

                    if (list.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.recyclerNotifications.visibility = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.recyclerNotifications.visibility = View.VISIBLE
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    showSnackbar(resource.message ?: "Failed to load notifications")
                }
            }
        }

        viewModel.markAllReadState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> showLoadingOverlay()
                is Resource.Success -> {
                    hideLoadingOverlay()
                    showSnackbar("All notifications marked as read")
                }
                is Resource.Error -> {
                    hideLoadingOverlay()
                    showSnackbar(resource.message ?: "Failed to mark all as read")
                }
            }
        }
    }

    private fun handleNotificationTap(notification: NotificationResponse) {
        viewModel.markAsRead(notification.id)

        val referenceId = notification.referenceId ?: return
        val bundle = Bundle().apply {
            putString("booking_id", referenceId)
        }

        try {
            when (notification.notificationType) {
                "booking_confirmed", "booking_cancelled", "payment_captured", "review_requested" -> {
                    findNavController().navigate(R.id.bookingDetailFragment, bundle)
                }
                "new_message" -> {
                    findNavController().navigate(R.id.chatFragment, bundle)
                }
                "upload_ready" -> {
                    findNavController().navigate(R.id.deliveryFragment, bundle)
                }
                else -> {
                    // Default fallback: do nothing or stay on screen
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showLoadingOverlay() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showSnackbar(message: String, actionText: String? = null, action: View.OnClickListener? = null) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).apply {
            setBackgroundTint(Color.parseColor("#2D1060"))
            setTextColor(Color.WHITE)
            if (actionText != null && action != null) {
                setAction(actionText, action)
                setActionTextColor(Color.parseColor("#E040FB"))
            }
            show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
