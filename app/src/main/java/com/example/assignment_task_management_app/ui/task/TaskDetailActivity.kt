package com.example.assignment_task_management_app.ui.task

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityTaskDetailBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.NotificationHelper
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var taskRepository: TaskRepository
    private var taskId: Long = -1L
    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)

        taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)

        setupToolbar()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadTaskDetails()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarTaskDetail)
        binding.toolbarTaskDetail.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnDetailToggleStatus.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
            lifecycleScope.launch {
                val success = taskRepository.updateTaskStatus(task.id, newStatus)
                if (success) {
                    if (newStatus == TaskStatus.COMPLETED) {
                        AlarmScheduler.cancelTaskReminder(this@TaskDetailActivity, task.id)
                        Toast.makeText(this@TaskDetailActivity, "Task marked as completed!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@TaskDetailActivity, "Task marked as pending", Toast.LENGTH_SHORT).show()
                    }
                    loadTaskDetails()
                }
            }
        }

        binding.btnDetailEdit.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            val intent = Intent(this, AddEditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            startActivity(intent)
        }

        binding.btnDetailDelete.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete '${task.title}'? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        val deleted = taskRepository.deleteTask(task.id)
                        if (deleted) {
                            AlarmScheduler.cancelTaskReminder(this@TaskDetailActivity, task.id)
                            Toast.makeText(this@TaskDetailActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnDetailTestNotification.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            NotificationHelper.showTaskReminderNotification(
                context = this,
                taskId = task.id,
                title = task.title,
                subject = task.subject,
                remainingText = DateUtils.getRemainingTimeString(task.dueDate)
            )
            Toast.makeText(this, "Reminder notification triggered!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTaskDetails() {
        lifecycleScope.launch {
            val task = taskRepository.getTaskById(taskId)
            if (task == null) {
                Toast.makeText(this@TaskDetailActivity, "Task not found", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }
            currentTask = task

            binding.tvDetailTitle.text = task.title
            binding.tvDetailSubject.text = task.subject

            if (task.description.isNotBlank()) {
                binding.tvDetailDescription.text = task.description
            } else {
                binding.tvDetailDescription.text = "No additional description provided."
            }

            binding.tvDetailDueDate.text = DateUtils.formatDateTime(task.dueDate)
            binding.tvDetailRemaining.text = DateUtils.getRemainingTimeString(task.dueDate)

            // Priority badge styling
            binding.tvDetailPriority.text = task.priority.displayName.uppercase()
            when (task.priority) {
                TaskPriority.HIGH -> {
                    binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_high)
                    binding.tvDetailPriority.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.priority_high_text))
                }
                TaskPriority.MEDIUM -> {
                    binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_medium)
                    binding.tvDetailPriority.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.priority_medium_text))
                }
                TaskPriority.LOW -> {
                    binding.tvDetailPriority.setBackgroundResource(R.drawable.bg_priority_low)
                    binding.tvDetailPriority.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.priority_low_text))
                }
            }

            // Status badge & button styling
            binding.tvDetailStatus.text = task.status.displayName.uppercase()
            when (task.status) {
                TaskStatus.PENDING -> {
                    binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_pending)
                    binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_pending_text))
                    binding.btnDetailToggleStatus.text = "Mark as Complete"
                    binding.btnDetailToggleStatus.backgroundTintList = ContextCompat.getColorStateList(this@TaskDetailActivity, R.color.status_completed_text)
                }
                TaskStatus.IN_PROGRESS -> {
                    binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                    binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_in_progress_text))
                    binding.btnDetailToggleStatus.text = "Mark as Complete"
                    binding.btnDetailToggleStatus.backgroundTintList = ContextCompat.getColorStateList(this@TaskDetailActivity, R.color.status_completed_text)
                }
                TaskStatus.COMPLETED -> {
                    binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_completed)
                    binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_completed_text))
                    binding.btnDetailToggleStatus.text = "Re-open Task (Set Pending)"
                    binding.btnDetailToggleStatus.backgroundTintList = ContextCompat.getColorStateList(this@TaskDetailActivity, R.color.status_pending_text)
                }
            }
        }
    }
}
