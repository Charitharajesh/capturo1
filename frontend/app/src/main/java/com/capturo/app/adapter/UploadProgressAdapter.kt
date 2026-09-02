package com.capturo.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.databinding.ItemUploadProgressBinding
import com.capturo.app.ui.creatorDashboard.UploadProgress

class UploadProgressAdapter :
    ListAdapter<UploadProgress, UploadProgressAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUploadProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemUploadProgressBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UploadProgress) {
            binding.tvFileName.text = item.fileName
            binding.tvPercentage.text = "${item.progress}%"
            binding.progressBar.progress = item.progress

            when {
                item.isCompleted -> {
                    binding.icStatusDone.visibility = View.VISIBLE
                    binding.icStatusFailed.visibility = View.GONE
                    binding.tvPercentage.visibility = View.GONE
                }
                item.isFailed -> {
                    binding.icStatusDone.visibility = View.GONE
                    binding.icStatusFailed.visibility = View.VISIBLE
                    binding.tvPercentage.visibility = View.GONE
                }
                else -> {
                    binding.icStatusDone.visibility = View.GONE
                    binding.icStatusFailed.visibility = View.GONE
                    binding.tvPercentage.visibility = View.VISIBLE
                }
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<UploadProgress>() {
            override fun areItemsTheSame(oldItem: UploadProgress, newItem: UploadProgress): Boolean {
                return oldItem.uri == newItem.uri
            }

            override fun areContentsTheSame(oldItem: UploadProgress, newItem: UploadProgress): Boolean {
                return oldItem == newItem
            }
        }
    }
}
