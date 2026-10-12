package com.example.assignment_task_management_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ItemTaskBinding
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.utils.DateUtils

class TaskAdapter(
    private val onItemClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context

            binding.tvItemTitle.text = task.title

            if (task.description.isNotBlank()) {
                binding.tvItemDescription.visibility = View.VISIBLE
                binding.tvItemDescription.text = task.description
            } else {
                binding.tvItemDescription.visibility = View.GONE
            }

            // Priority Badge
            binding.tvItemPriority.text = task.priority.displayName
            when (task.priority) {
                TaskPriority.HIGH -> {
                    binding.tvItemPriority.setBackgroundResource(R.drawable.bg_priority_high)
                    binding.tvItemPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_high_text))
                }
                TaskPriority.MEDIUM -> {
                    binding.tvItemPriority.setBackgroundResource(R.drawable.bg_priority_medium)
                    binding.tvItemPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_medium_text))
                }
                TaskPriority.LOW -> {
                    binding.tvItemPriority.setBackgroundResource(R.drawable.bg_priority_low)
                    binding.tvItemPriority.setTextColor(ContextCompat.getColor(context, R.color.priority_low_text))
                }
            }

            // Status Badge
            val isOverdue = DateUtils.isOverdue(task.dueDate) && task.status != TaskStatus.COMPLETED
            if (isOverdue) {
                binding.tvItemStatus.text = "Overdue"
                binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_overdue)
                binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_overdue_text))
            } else {
                binding.tvItemStatus.text = task.status.displayName
                when (task.status) {
                    TaskStatus.PENDING -> {
                        binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_pending)
                        binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                    }
                    TaskStatus.IN_PROGRESS -> {
                        binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                        binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_in_progress_text))
                    }
                    TaskStatus.COMPLETED -> {
                        binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_completed)
                        binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text))
                    }
                }
            }

            // Due Date
            binding.tvItemDueDate.text = "Due " + DateUtils.formatDate(task.dueDate)

            // Click listener
            binding.root.setOnClickListener { onItemClick(task) }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
