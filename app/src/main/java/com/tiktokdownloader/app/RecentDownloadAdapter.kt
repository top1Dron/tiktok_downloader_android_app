package com.tiktokdownloader.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiktokdownloader.app.data.DownloadHistory
import com.tiktokdownloader.app.databinding.ItemRecentDownloadBinding

class RecentDownloadAdapter(
    private val onReloadClick: (DownloadHistory) -> Unit
) : ListAdapter<DownloadHistory, RecentDownloadAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ViewHolder(
        private val binding: ItemRecentDownloadBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(history: DownloadHistory) {
            // Display original video title (or use filePath as fallback for old entries)
            val displayName = if (history.fileName.isNullOrEmpty()) {
                history.filePath ?: "Unknown"
            } else {
                history.fileName
            }
            binding.fileNameText.text = displayName
            binding.urlText.text = history.url
            binding.statusText.text = history.status.replaceFirstChar { it.uppercase() }
            
            // Set platform badge
            binding.platformBadge.text = history.platform.uppercase()
            binding.platformBadge.setBackgroundColor(
                when (history.platform) {
                    "instagram" -> android.graphics.Color.parseColor("#E4405F")
                    else -> android.graphics.Color.parseColor("#000000")
                }
            )
            
            binding.reloadButton.setOnClickListener {
                onReloadClick(history)
            }
        }
    }
    
    class DiffCallback : DiffUtil.ItemCallback<DownloadHistory>() {
        override fun areItemsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory): Boolean {
            return oldItem.taskId == newItem.taskId
        }
        
        override fun areContentsTheSame(oldItem: DownloadHistory, newItem: DownloadHistory): Boolean {
            return oldItem == newItem
        }
    }
}

