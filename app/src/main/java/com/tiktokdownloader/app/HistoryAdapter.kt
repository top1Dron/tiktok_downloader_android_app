package com.tiktokdownloader.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tiktokdownloader.app.data.DownloadHistory
import com.tiktokdownloader.app.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onReloadClick: (DownloadHistory) -> Unit
) : ListAdapter<DownloadHistory, HistoryAdapter.ViewHolder>(DiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
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
        private val binding: ItemHistoryBinding
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
            
            // Format date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = if (history.completedAt != null) {
                dateFormat.format(Date(history.completedAt))
            } else {
                dateFormat.format(Date(history.createdAt))
            }
            binding.dateText.text = date
            
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

