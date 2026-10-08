package com.example.assignment_task_management_app.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityAddEditTaskBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    private var taskId: Long = -1L
    private var userId: Long = -1L
    private var selectedDueDateMs: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // Default tomorrow

    private val calendar: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        userId = sessionManager.getLoggedInUserId()

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)

        taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)

        setupToolbar()
        setupSubjectAutoComplete()
        setupDateTimePickers()
        setupListeners()

        if (taskId != -1L) {
            loadExistingTaskDetails()
        } else {
            updateDateTimeDisplay()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarAddEditTask)
        binding.toolbarAddEditTask.setNavigationOnClickListener { finish() }
        binding.toolbarAddEditTask.title = if (taskId == -1L) "Add New Task" else "Edit Task"
    }

    private fun setupSubjectAutoComplete() {
        lifecycleScope.launch {
            val subjects = taskRepository.getSubjectsForUser(userId)
            val adapter = ArrayAdapter(this@AddEditTaskActivity, android.R.layout.simple_dropdown_item_1line, subjects)
            binding.actvSubject.setAdapter(adapter)
        }
    }

    private fun setupDateTimePickers() {
        binding.btnPickDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    selectedDueDateMs = calendar.timeInMillis
                    updateDateTimeDisplay()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        binding.btnPickTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    selectedDueDateMs = calendar.timeInMillis
                    updateDateTimeDisplay()
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePicker.show()
        }
    }

    private fun updateDateTimeDisplay() {
        binding.tvSelectedDateTime.text = "Due: " + DateUtils.formatDateTime(selectedDueDateMs)
        binding.btnPickDate.text = DateUtils.formatDate(selectedDueDateMs)
        binding.btnPickTime.text = DateUtils.formatTime(selectedDueDateMs)
    }

    private fun setupListeners() {
        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }
    }

    private fun loadExistingTaskDetails() {
        lifecycleScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            binding.etTaskTitle.setText(task.title)
            binding.etTaskDesc.setText(task.description)
            binding.actvSubject.setText(task.subject, false)

            selectedDueDateMs = task.dueDate
            calendar.timeInMillis = selectedDueDateMs
            updateDateTimeDisplay()

            when (task.priority) {
                TaskPriority.HIGH -> binding.rbPriorityHigh.isChecked = true
                TaskPriority.MEDIUM -> binding.rbPriorityMedium.isChecked = true
                TaskPriority.LOW -> binding.rbPriorityLow.isChecked = true
            }

            when (task.status) {
                TaskStatus.PENDING -> binding.rbStatusPending.isChecked = true
                TaskStatus.IN_PROGRESS -> binding.rbStatusInProgress.isChecked = true
                TaskStatus.COMPLETED -> binding.rbStatusCompleted.isChecked = true
            }

            binding.switchReminder.isChecked = task.isReminderSet
        }
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDesc.text.toString().trim()
        val subject = binding.actvSubject.text.toString().trim()

        binding.tilTaskTitle.error = null
        binding.tilTaskSubject.error = null

        if (title.isEmpty()) {
            binding.tilTaskTitle.error = "Title is required"
            return
        }

        if (subject.isEmpty()) {
            binding.tilTaskSubject.error = "Subject is required"
            return
        }

        val priority = when {
            binding.rbPriorityHigh.isChecked -> TaskPriority.HIGH
            binding.rbPriorityLow.isChecked -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        val status = when {
            binding.rbStatusCompleted.isChecked -> TaskStatus.COMPLETED
            binding.rbStatusInProgress.isChecked -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.PENDING
        }

        val isReminderSet = binding.switchReminder.isChecked

        val taskToSave = Task(
            id = if (taskId == -1L) 0 else taskId,
            userId = userId,
            title = title,
            description = description,
            subject = subject,
            dueDate = selectedDueDateMs,
            priority = priority,
            status = status,
            isReminderSet = isReminderSet
        )

        lifecycleScope.launch {
            binding.btnSaveTask.isEnabled = false
            if (taskId == -1L) {
                val result = taskRepository.insertTask(taskToSave)
                result.onSuccess { newId ->
                    val savedTask = taskToSave.copy(id = newId)
                    if (isReminderSet && status != TaskStatus.COMPLETED) {
                        AlarmScheduler.scheduleTaskReminder(this@AddEditTaskActivity, savedTask)
                    }
                    Toast.makeText(this@AddEditTaskActivity, "Task saved successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { ex ->
                    binding.btnSaveTask.isEnabled = true
                    Toast.makeText(this@AddEditTaskActivity, ex.message ?: "Failed to save task", Toast.LENGTH_SHORT).show()
                }
            } else {
                val result = taskRepository.updateTask(taskToSave)
                result.onSuccess {
                    if (isReminderSet && status != TaskStatus.COMPLETED) {
                        AlarmScheduler.scheduleTaskReminder(this@AddEditTaskActivity, taskToSave)
                    } else {
                        AlarmScheduler.cancelTaskReminder(this@AddEditTaskActivity, taskId)
                    }
                    Toast.makeText(this@AddEditTaskActivity, "Task updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }.onFailure { ex ->
                    binding.btnSaveTask.isEnabled = true
                    Toast.makeText(this@AddEditTaskActivity, ex.message ?: "Failed to update task", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
