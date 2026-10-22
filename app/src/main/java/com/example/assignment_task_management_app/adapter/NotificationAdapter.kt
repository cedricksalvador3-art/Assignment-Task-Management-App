package com.example.assignment_task_management_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ItemNotificationBinding
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.utils.DateUtils

data class NotificationItem(
    val id: Long,
    val taskId: Long,
    val title: String,
    val message: String,
    val timeAgo: String,
    val isOverdue: Boolean
)

class NotificationAdapter(
    private val onItemClick: (NotificationItem) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationItem) {
            val context = binding.root.context
            binding.tvNotifMessage.text = item.message
            binding.tvNotifTime.text = item.timeAgo

            if (item.isOverdue) {
                binding.ivNotifIcon.setImageResource(R.drawable.ic_bell)
                binding.ivNotifIcon.setColorFilter(ContextCompat.getColor(context, R.color.error_red))
            } else {
                binding.ivNotifIcon.setImageResource(R.drawable.ic_bell)
                binding.ivNotifIcon.setColorFilter(ContextCompat.getColor(context, R.color.navy_primary))
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem): Boolean {
            return oldItem == newItem
        }
    }
}
