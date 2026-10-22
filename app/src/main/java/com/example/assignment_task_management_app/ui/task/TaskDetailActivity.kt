package com.example.assignment_task_management_app.ui.task

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
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
import com.example.assignment_task_management_app.ui.main.MainActivity
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.DateUtils
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
        setupBottomNav()
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

    private fun setupBottomNav() {
        binding.detailBottomNavigation.setOnItemSelectedListener { item ->
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
            true
        }
    }

    private fun setupListeners() {
        binding.btnDetailEdit.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            val intent = Intent(this, AddEditTaskActivity::class.java).apply {
                putExtra("EXTRA_TASK_ID", task.id)
            }
            startActivity(intent)
        }

        binding.btnDetailUpdateStatus.setOnClickListener {
            showUpdateStatusDialog()
        }

        binding.btnDetailMarkCompleted.setOnClickListener {
            val task = currentTask ?: return@setOnClickListener
            val newStatus = if (task.status == TaskStatus.COMPLETED) TaskStatus.PENDING else TaskStatus.COMPLETED
            updateStatus(newStatus)
        }

        binding.btnDetailDelete.setOnClickListener {
            showDeleteDialog()
        }
    }

    private fun showUpdateStatusDialog() {
        val statuses = arrayOf("Pending", "In Progress", "Completed")
        AlertDialog.Builder(this)
            .setTitle("Update Task Status")
            .setItems(statuses) { _, which ->
                val newStatus = when (which) {
                    1 -> TaskStatus.IN_PROGRESS
                    2 -> TaskStatus.COMPLETED
                    else -> TaskStatus.PENDING
                }
                updateStatus(newStatus)
            }
            .show()
    }

    private fun updateStatus(newStatus: TaskStatus) {
        val task = currentTask ?: return
        lifecycleScope.launch {
            val success = taskRepository.updateTaskStatus(task.id, newStatus)
            if (success) {
                if (newStatus == TaskStatus.COMPLETED) {
                    AlarmScheduler.cancelTaskReminder(this@TaskDetailActivity, task.id)
                    Toast.makeText(this@TaskDetailActivity, "Task marked as completed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@TaskDetailActivity, "Task status updated to ${newStatus.displayName}", Toast.LENGTH_SHORT).show()
                }
                loadTaskDetails()
            }
        }
    }

    private fun showDeleteDialog() {
        val task = currentTask ?: return
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val msgView = dialogView.findViewById<TextView>(R.id.tv_delete_confirm_msg)
        msgView.text = getString(R.string.delete_task_confirm, task.title)

        dialogView.findViewById<Button>(R.id.btn_delete_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_delete_confirm).setOnClickListener {
            dialog.dismiss()
            lifecycleScope.launch {
                val deleted = taskRepository.deleteTask(task.id)
                if (deleted) {
                    AlarmScheduler.cancelTaskReminder(this@TaskDetailActivity, task.id)
                    Toast.makeText(this@TaskDetailActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        dialog.show()
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

            // Status badge styling
            val isOverdue = DateUtils.isOverdue(task.dueDate) && task.status != TaskStatus.COMPLETED
            if (isOverdue) {
                binding.tvDetailStatus.text = "OVERDUE"
                binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_overdue)
                binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_overdue_text))
            } else {
                binding.tvDetailStatus.text = task.status.displayName.uppercase()
                when (task.status) {
                    TaskStatus.PENDING -> {
                        binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_pending)
                        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_pending_text))
                    }
                    TaskStatus.IN_PROGRESS -> {
                        binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_in_progress)
                        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_in_progress_text))
                    }
                    TaskStatus.COMPLETED -> {
                        binding.tvDetailStatus.setBackgroundResource(R.drawable.bg_status_completed)
                        binding.tvDetailStatus.setTextColor(ContextCompat.getColor(this@TaskDetailActivity, R.color.status_completed_text))
                    }
                }
            }

            // Button label styling
            if (task.status == TaskStatus.COMPLETED) {
                binding.btnDetailMarkCompleted.text = "Re-open Task (Set Pending)"
            } else {
                binding.btnDetailMarkCompleted.text = getString(R.string.mark_as_completed)
            }
        }
    }
}
