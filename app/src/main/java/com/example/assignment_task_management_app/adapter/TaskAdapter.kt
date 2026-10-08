package com.example.assignment_task_management_app.adapter

import android.content.Context
import android.content.res.ColorStateList
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
    private val onItemClick: (Task) -> Unit,
    private val onMarkCompleteClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit
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
            binding.tvItemSubject.text = task.subject

            // Description
            if (task.description.isNotBlank()) {
                binding.tvItemDescription.visibility = View.VISIBLE
                binding.tvItemDescription.text = task.description
            } else {
                binding.tvItemDescription.visibility = View.GONE
            }

            // Priority Badge Styling
            binding.tvItemPriority.text = task.priority.displayName.uppercase()
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

            // Status Badge Styling
            binding.tvItemStatus.text = task.status.displayName.uppercase()
            when (task.status) {
                TaskStatus.PENDING -> {
                    binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_text))
                    binding.btnItemMarkComplete.visibility = View.VISIBLE
                    binding.btnItemMarkComplete.text = context.getString(R.string.mark_completed)
                }
                TaskStatus.IN_PROGRESS -> {
                    binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                    binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_in_progress_text))
                    binding.btnItemMarkComplete.visibility = View.VISIBLE
                    binding.btnItemMarkComplete.text = context.getString(R.string.mark_completed)
                }
                TaskStatus.COMPLETED -> {
                    binding.tvItemStatus.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.tvItemStatus.setTextColor(ContextCompat.getColor(context, R.color.status_completed_text))
                    binding.btnItemMarkComplete.visibility = View.GONE
                }
            }

            // Due Date & Remaining Time
            binding.tvItemDueDate.text = "Due: " + DateUtils.formatDateTime(task.dueDate)
            binding.tvItemRemaining.text = DateUtils.getRemainingTimeString(task.dueDate)

            if (DateUtils.isOverdue(task.dueDate) && task.status != TaskStatus.COMPLETED) {
                binding.tvItemRemaining.setTextColor(ContextCompat.getColor(context, R.color.error_red))
            } else {
                binding.tvItemRemaining.setTextColor(ContextCompat.getColor(context, R.color.accent))
            }

            // Click Listeners
            binding.root.setOnClickListener { onItemClick(task) }
            binding.btnItemMarkComplete.setOnClickListener { onMarkCompleteClick(task) }
            binding.btnItemEdit.setOnClickListener { onEditClick(task) }
            binding.btnItemDelete.setOnClickListener { onDeleteClick(task) }
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
