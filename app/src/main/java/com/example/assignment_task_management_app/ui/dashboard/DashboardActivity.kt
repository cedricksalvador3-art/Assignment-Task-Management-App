package com.example.assignment_task_management_app.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.assignment_task_management_app.adapter.TaskAdapter
import com.example.assignment_task_management_app.databinding.ActivityDashboardBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.ui.auth.LoginActivity
import com.example.assignment_task_management_app.ui.task.AddEditTaskActivity
import com.example.assignment_task_management_app.ui.task.TaskDetailActivity
import com.example.assignment_task_management_app.ui.task.TaskListActivity
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.NotificationHelper
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var taskAdapter: TaskAdapter
    private var userId: Long = -1L

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userId = sessionManager.getLoggedInUserId()

        if (userId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)

        setupUI()
        setupListeners()
        checkNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupUI() {
        binding.tvUserFullName.text = sessionManager.getLoggedInFullName()

        taskAdapter = TaskAdapter(
            onItemClick = { task ->
                val intent = Intent(this, TaskDetailActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            },
            onMarkCompleteClick = { task ->
                markTaskCompleted(task)
            },
            onEditClick = { task ->
                val intent = Intent(this, AddEditTaskActivity::class.java).apply {
                    putExtra("EXTRA_TASK_ID", task.id)
                }
                startActivity(intent)
            },
            onDeleteClick = { task ->
                confirmDeleteTask(task)
            }
        )

        binding.rvDueSoonTasks.layoutManager = LinearLayoutManager(this)
        binding.rvDueSoonTasks.adapter = taskAdapter
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out of your account?")
                .setPositiveButton("Logout") { _, _ ->
                    sessionManager.clearSession()
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnAddNewTask.setOnClickListener {
            startActivity(Intent(this, AddEditTaskActivity::class.java))
        }

        binding.btnViewAllTasks.setOnClickListener {
            startActivity(Intent(this, TaskListActivity::class.java))
        }

        binding.btnTestReminderNotification.setOnClickListener {
            testNotificationTrigger()
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val stats = taskRepository.getDashboardStats(userId)
            binding.tvStatTotal.text = stats.totalTasks.toString()
            binding.tvStatPending.text = stats.pendingTasks.toString()
            binding.tvStatInProgress.text = stats.inProgressTasks.toString()
            binding.tvStatCompleted.text = stats.completedTasks.toString()

            binding.tvProgressRatio.text = "${stats.completedTasks} of ${stats.totalTasks} Tasks Completed"
            binding.tvProgressPercentage.text = "${stats.completionPercentage}%"
            binding.progressIndicator.progress = stats.completionPercentage

            val dueSoonTasks = taskRepository.getDueSoonTasks(userId)
            if (dueSoonTasks.isNotEmpty()) {
                binding.rvDueSoonTasks.visibility = View.VISIBLE
                binding.tvNoDueSoonTasks.visibility = View.GONE
                taskAdapter.submitList(dueSoonTasks)

                // Trigger automatic notification for the most urgent task if due soon
                val mostUrgent = dueSoonTasks.first()
                if (DateUtils.isDueSoon(mostUrgent.dueDate, 24) && mostUrgent.isReminderSet) {
                    NotificationHelper.showTaskReminderNotification(
                        context = this@DashboardActivity,
                        taskId = mostUrgent.id,
                        title = mostUrgent.title,
                        subject = mostUrgent.subject,
                        remainingText = DateUtils.getRemainingTimeString(mostUrgent.dueDate)
                    )
                }
            } else {
                binding.rvDueSoonTasks.visibility = View.GONE
                binding.tvNoDueSoonTasks.visibility = View.VISIBLE
            }
        }
    }

    private fun markTaskCompleted(task: Task) {
        lifecycleScope.launch {
            val success = taskRepository.updateTaskStatus(task.id, TaskStatus.COMPLETED)
            if (success) {
                AlarmScheduler.cancelTaskReminder(this@DashboardActivity, task.id)
                Toast.makeText(this@DashboardActivity, "Task '${task.title}' marked as completed!", Toast.LENGTH_SHORT).show()
                loadDashboardData()
            }
        }
    }

    private fun confirmDeleteTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    val deleted = taskRepository.deleteTask(task.id)
                    if (deleted) {
                        AlarmScheduler.cancelTaskReminder(this@DashboardActivity, task.id)
                        Toast.makeText(this@DashboardActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                        loadDashboardData()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun testNotificationTrigger() {
        NotificationHelper.showTaskReminderNotification(
            context = this,
            taskId = 999L,
            title = "Sample Assignment Reminder",
            subject = "Mobile App Dev",
            remainingText = "Due in 2 hours"
        )
        Toast.makeText(this, "Test reminder notification sent!", Toast.LENGTH_SHORT).show()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
