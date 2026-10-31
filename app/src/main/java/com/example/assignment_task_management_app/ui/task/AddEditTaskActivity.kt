package com.example.assignment_task_management_app.ui.task

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.assignment_task_management_app.R
import com.example.assignment_task_management_app.databinding.ActivityAddEditTaskBinding
import com.example.assignment_task_management_app.db.DatabaseHelper
import com.example.assignment_task_management_app.model.Task
import com.example.assignment_task_management_app.model.TaskPriority
import com.example.assignment_task_management_app.model.TaskStatus
import com.example.assignment_task_management_app.repository.TaskRepository
import com.example.assignment_task_management_app.utils.AlarmScheduler
import com.example.assignment_task_management_app.utils.AssignmentPdfGenerator
import com.example.assignment_task_management_app.utils.DateUtils
import com.example.assignment_task_management_app.utils.MathExpressionSolver
import com.example.assignment_task_management_app.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditTaskBinding
    private lateinit var taskRepository: TaskRepository
    private lateinit var sessionManager: SessionManager

    private var taskId: Long = -1L
    private val calendar: Calendar = DateUtils.getPhilippineCalendar()
    private var selectedDueDateMs: Long = 0L
    private var existingTaskStatus: TaskStatus = TaskStatus.PENDING

    private var scannedPdfPath: String? = null
    private var scannedMathSolution: String? = null

    private val scanAssignmentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val currentTitle = binding.etTaskTitle.text.toString().ifBlank { "Scanned Assignment" }
                    val currentSubject = binding.etTaskSubject.text.toString().ifBlank { "General" }

                    val isMath = currentSubject.contains("math", ignoreCase = true) ||
                            currentTitle.contains("math", ignoreCase = true)

                    if (isMath) {
                        val mathResult = MathExpressionSolver.solveExpressionsFromText(currentTitle)
                        scannedMathSolution = if (mathResult.formattedSolutionSummary.isNotBlank()) mathResult.formattedSolutionSummary else null
                        if (scannedMathSolution != null) {
                            binding.tvScannedMathPreview.text = scannedMathSolution
                            binding.tvScannedMathPreview.visibility = View.VISIBLE
                        } else {
                            binding.tvScannedMathPreview.visibility = View.GONE
                        }
                    } else {
                        scannedMathSolution = null
                        binding.tvScannedMathPreview.visibility = View.GONE
                    }

                    val pdfPath = AssignmentPdfGenerator.generatePdfFromScan(
                        context = this,
                        scannedBitmap = bitmap,
                        title = currentTitle,
                        subject = currentSubject,
                        mathSolutions = scannedMathSolution ?: "Digital Soft Copy Attached."
                    )

                    scannedPdfPath = pdfPath
                    binding.cardScanResult.visibility = View.VISIBLE

                    if (binding.etTaskTitle.text.isNullOrBlank()) {
                        binding.etTaskTitle.setText("Scanned Assignment")
                    }

                    Toast.makeText(this, "Paper scanned & soft copy file attached!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Could not load paper image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dbHelper = DatabaseHelper.getInstance(this)
        taskRepository = TaskRepository(dbHelper)
        sessionManager = SessionManager(this)

        taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)

        binding.toolbarAddEditTask.setNavigationOnClickListener { finish() }

        if (taskId != -1L) {
            binding.toolbarAddEditTask.title = "Edit Task"
            loadExistingTaskDetails()
        } else {
            binding.toolbarAddEditTask.title = "Add New Task"
            // Default due date: Tomorrow at 11:59 PM Philippine Time
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            selectedDueDateMs = calendar.timeInMillis
            updateDateTimeDisplay()
        }

        setupDatePickers()
        setupListeners()
    }

    private fun setupDatePickers() {
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
            val minDateVal = minOf(calendar.timeInMillis, System.currentTimeMillis() - 1000L)
            datePicker.datePicker.minDate = minDateVal
            datePicker.show()
        }

        binding.btnPickTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
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

        if (selectedDueDateMs < System.currentTimeMillis()) {
            binding.tvDateWarning.visibility = View.VISIBLE
            binding.tvSelectedDateTime.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        } else {
            binding.tvDateWarning.visibility = View.GONE
            binding.tvSelectedDateTime.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun openAttachmentFile(filePath: String?) {
        if (filePath.isNullOrBlank()) {
            Toast.makeText(this, "No soft copy file attached.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(this, "Soft copy file not found.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                file
            )

            val mimeType = when {
                filePath.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                filePath.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                filePath.endsWith(".png", ignoreCase = true) -> "image/png"
                filePath.endsWith(".jpg", ignoreCase = true) || filePath.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
                filePath.endsWith(".txt", ignoreCase = true) -> "text/plain"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "Open Soft Copy Assignment"))
        } catch (_: Exception) {
            Toast.makeText(this, "No compatible app found to open this file.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.btnScanAssignment.setOnClickListener {
            scanAssignmentLauncher.launch("image/*")
        }

        binding.cardScanResult.setOnClickListener {
            openAttachmentFile(scannedPdfPath)
        }

        binding.btnOpenScannedFile.setOnClickListener {
            openAttachmentFile(scannedPdfPath)
        }

        binding.btnSaveTask.setOnClickListener {
            saveTask()
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadExistingTaskDetails() {
        lifecycleScope.launch {
            val task = taskRepository.getTaskById(taskId) ?: return@launch
            binding.etTaskTitle.setText(task.title)
            binding.etTaskDesc.setText(task.description)
            binding.etTaskSubject.setText(task.subject)

            selectedDueDateMs = task.dueDate
            calendar.timeInMillis = selectedDueDateMs
            updateDateTimeDisplay()
            existingTaskStatus = task.status

            scannedPdfPath = task.pdfPath
            scannedMathSolution = task.mathSolution

            if (task.pdfPath != null || task.mathSolution != null) {
                binding.cardScanResult.visibility = View.VISIBLE
                if (!task.mathSolution.isNullOrBlank()) {
                    binding.tvScannedMathPreview.visibility = View.VISIBLE
                    binding.tvScannedMathPreview.text = task.mathSolution
                } else {
                    binding.tvScannedMathPreview.visibility = View.GONE
                }
            } else {
                binding.cardScanResult.visibility = View.GONE
            }

            when (task.priority) {
                TaskPriority.HIGH -> binding.rbPriorityHigh.isChecked = true
                TaskPriority.MEDIUM -> binding.rbPriorityMedium.isChecked = true
                TaskPriority.LOW -> binding.rbPriorityLow.isChecked = true
            }

            binding.switchReminder.isChecked = task.isReminderSet
        }
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val description = binding.etTaskDesc.text.toString().trim()
        val subject = binding.etTaskSubject.text.toString().trim()

        if (title.isBlank()) {
            binding.etTaskTitle.error = "Task title is required"
            binding.etTaskTitle.requestFocus()
            return
        }

        if (subject.isBlank()) {
            binding.etTaskSubject.error = "Subject is required"
            binding.etTaskSubject.requestFocus()
            return
        }

        if (selectedDueDateMs < System.currentTimeMillis()) {
            AlertDialog.Builder(this)
                .setTitle("Invalid Due Date")
                .setMessage("The selected due date is in the past. Please choose a valid future date.")
                .setPositiveButton("Choose Date") { dialog, _ ->
                    dialog.dismiss()
                    binding.btnPickDate.performClick()
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }

        val userId = sessionManager.getLoggedInUserId()
        if (userId == -1L) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        val priority = when (binding.rgPriority.checkedRadioButtonId) {
            R.id.rb_priority_high -> TaskPriority.HIGH
            R.id.rb_priority_medium -> TaskPriority.MEDIUM
            R.id.rb_priority_low -> TaskPriority.LOW
            else -> TaskPriority.MEDIUM
        }

        val isReminderSet = binding.switchReminder.isChecked

        val taskToSave = Task(
            id = if (taskId != -1L) taskId else 0L,
            userId = userId,
            title = title,
            description = description,
            subject = subject,
            dueDate = selectedDueDateMs,
            priority = priority,
            status = existingTaskStatus,
            isReminderSet = isReminderSet,
            pdfPath = scannedPdfPath,
            mathSolution = scannedMathSolution
        )

        lifecycleScope.launch {
            binding.btnSaveTask.isEnabled = false
            val result = if (taskId == -1L) {
                taskRepository.insertTask(taskToSave)
            } else {
                val updateRes = taskRepository.updateTask(taskToSave)
                if (updateRes.isSuccess) Result.success(taskId) else Result.failure(updateRes.exceptionOrNull() ?: Exception("Update failed"))
            }

            result.onSuccess { savedTaskId ->
                if (isReminderSet) {
                    AlarmScheduler.scheduleTaskReminder(this@AddEditTaskActivity, taskToSave.copy(id = savedTaskId))
                } else {
                    AlarmScheduler.cancelTaskReminder(this@AddEditTaskActivity, savedTaskId)
                }

                val msg = if (taskId == -1L) "Task created successfully!" else "Task updated successfully!"
                Toast.makeText(this@AddEditTaskActivity, msg, Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { exception ->
                binding.btnSaveTask.isEnabled = true
                Toast.makeText(this@AddEditTaskActivity, exception.message ?: "Failed to save task", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
